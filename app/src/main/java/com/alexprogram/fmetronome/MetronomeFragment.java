/*
 * Copyright (C) 2025 Oleksii Chepishko
 * Fmetronome is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Fmetronome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Fmetronome.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.alexprogram.fmetronome;


import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;


public class MetronomeFragment extends Fragment {

    private Spinner rhythmSpinner;
    private SeekBar tempoSeekBar;
    private TextView tempoValue;
    private Button increaseTempoBtn, decreaseTempoBtn;
    private Button tapTempoButton;
    private MaterialButton startStopBut;
    private boolean isPlaying = false;
    private Handler metronomeHandler = new Handler(Looper.getMainLooper());
    private long nextTickTime;
    private Handler tempoHandler = new Handler();
    private Runnable metronomeRunnable;
    private SoundPool soundPool;
    private int ticSoundId;
    private int tacSoundId;
    private boolean soundsLoaded = false;
    private int tempo = 100;
    private Animation pulseAnimation;
    private int currentBeat = 0;
    private int beatsPerMeasure = 4;
    private TableRow beatRow;
    private List<ImageView> beatCircles = new ArrayList<>();
    private ArrayList<Long> tapTimes = new ArrayList<>();
    private static final int TAP_RESET_TIME_MS = 2000;

    public MetronomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_metronome, container, false);

        SharedPreferences preferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        int theme = preferences.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(theme);

        SharedPreferences prefs = requireActivity().getSharedPreferences("MetronomePrefs", Context.MODE_PRIVATE);
        tempo = prefs.getInt("savedTempo", 100);

        rhythmSpinner = view.findViewById(R.id.rhythmSpinner);
        tempoSeekBar = view.findViewById(R.id.tempoSeekBar);
        tempoValue = view.findViewById(R.id.tempoValue);
        increaseTempoBtn = view.findViewById(R.id.increaseTempoBtn);
        decreaseTempoBtn = view.findViewById(R.id.decreaseTempoBtn);
        tapTempoButton = view.findViewById(R.id.tapTempoButton);
        startStopBut = view.findViewById(R.id.startStopBut);
        beatRow = view.findViewById(R.id.beatRow);
        Button coachBtn = view.findViewById(R.id.coachBtn);

        coachBtn.setOnClickListener(v -> showBpmDialog());

        soundPool = new SoundPool.Builder().setMaxStreams(2).build();
        ticSoundId = soundPool.load(requireContext(), R.raw.tic, 1);
        tacSoundId = soundPool.load(requireContext(), R.raw.tac, 1);

        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) soundsLoaded = true;
        });

        beatCircles.add(view.findViewById(R.id.beatCircle1));
        beatCircles.add(view.findViewById(R.id.beatCircle2));
        beatCircles.add(view.findViewById(R.id.beatCircle3));
        beatCircles.add(view.findViewById(R.id.beatCircle4));

        pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse);

        setupRhythmSpinner();
        setupSeekBar();
        setupButtons();
        setupTapTempoButton();
        setupStartStopButton();

        return view;
    }

    private void setupStartStopButton() {
        startStopBut.setOnClickListener(v -> {
            isPlaying = !isPlaying;

            if (isPlaying) {
                startMetronome();
                startStopBut.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.outline_pause_24));
            } else {
                stopMetronome();
                startStopBut.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.outline_play_arrow_24));
            }
        });
    }

    private void startMetronomeInternal() {
        currentBeat = 0;
        nextTickTime = SystemClock.elapsedRealtime();
        metronomeHandler.removeCallbacksAndMessages(null);

        metronomeRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPlaying) return;

                if (soundsLoaded) {
                    if (beatsPerMeasure == 1) {
                        soundPool.play(tacSoundId, 1f, 1f, 0, 0, 1f);
                    } else {
                        if (currentBeat == 0) {
                            soundPool.play(ticSoundId, 1f, 1f, 0, 0, 1f);
                        } else {
                            soundPool.play(tacSoundId, 1f, 1f, 0, 0, 1f);
                        }
                    }
                }

                for (ImageView beatCircle : beatCircles) {
                    beatCircle.clearAnimation();
                    beatCircle.setColorFilter(null);
                }

                if (currentBeat < beatCircles.size()) {
                    beatCircles.get(currentBeat).startAnimation(pulseAnimation);
                    beatCircles.get(currentBeat).setColorFilter(
                            currentBeat == 0 ? Color.GREEN : Color.BLUE
                    );
                }

                currentBeat = (currentBeat + 1) % beatsPerMeasure;

                long interval = 60000L / tempo;
                nextTickTime += interval;
                long delay = nextTickTime - SystemClock.elapsedRealtime();
                metronomeHandler.postDelayed(this, Math.max(0, delay));
            }
        };

        metronomeHandler.post(metronomeRunnable);
    }


    private void startMetronome() {
        isPlaying = true;
        Toast.makeText(requireContext(), "Metronome is running", Toast.LENGTH_SHORT).show();
        startMetronomeInternal();
    }

    private void stopMetronome() {
        Toast.makeText(requireContext(), "Metronome stopped", Toast.LENGTH_SHORT).show();
        isPlaying = false;

        metronomeHandler.removeCallbacks(metronomeRunnable);

        for (ImageView beat : beatCircles) {
            beat.clearAnimation();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }


    private void setupRhythmSpinner() {
        String[] rhythms = {"1/4", "2/4", "3/4", "4/4", "6/8"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                rhythms
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rhythmSpinner.setAdapter(adapter);

        SharedPreferences prefs = requireActivity().getSharedPreferences("MetronomePrefs", Context.MODE_PRIVATE);
        String savedRhythm = prefs.getString("selectedRhythm", "4/4");
        int defaultPosition = adapter.getPosition(savedRhythm);
        rhythmSpinner.setSelection(defaultPosition);

        rhythmSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRhythm = parent.getItemAtPosition(position).toString();
                Toast.makeText(requireContext(), "Selected rhythm: " + selectedRhythm, Toast.LENGTH_SHORT).show();

                SharedPreferences prefs = requireActivity().getSharedPreferences("MetronomePrefs", Context.MODE_PRIVATE);
                prefs.edit().putString("selectedRhythm", selectedRhythm).apply();

                boolean wasPlaying = isPlaying;
                if (isPlaying) stopMetronome();

                String[] parts = selectedRhythm.split("/");
                if (parts.length == 2) {
                    beatsPerMeasure = Integer.parseInt(parts[0]);
                    buildBeatCircles(beatsPerMeasure);
                }

                if (wasPlaying) startMetronome();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupSeekBar() {
        tempoSeekBar.setMax(260);
        tempoSeekBar.setProgress(tempo - 20);
        tempoValue.setText("Tempo: " + tempo + " BPM");

        tempoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tempo = progress + 20;
                tempoValue.setText("Tempo: " + tempo + " BPM");

                SharedPreferences prefs = requireActivity().getSharedPreferences("MetronomePrefs", Context.MODE_PRIVATE);
                prefs.edit().putInt("savedTempo", tempo).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void buildBeatCircles(int count) {
        beatRow.removeAllViews();
        beatCircles.clear();

        int sizeInDp = 58;
        int sizeInPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                sizeInDp,
                getResources().getDisplayMetrics()
        );

        for (int i = 0; i < count; i++) {
            ImageView beat = new ImageView(requireContext());
            beat.setLayoutParams(new TableRow.LayoutParams(0, sizeInPx, 1));
            beat.setImageResource(R.drawable.baseline_circle_24);
            beat.setPadding(16, 16, 16, 16);
            beatRow.addView(beat);
            beatCircles.add(beat);
        }
    }

    private void setupTapTempoButton() {
        tapTempoButton.setOnClickListener(view -> {
            Animation tapAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.but_tap);
            tapTempoButton.startAnimation(tapAnimation);

            long currentTime = System.currentTimeMillis();

            if (!tapTimes.isEmpty() && currentTime - tapTimes.get(tapTimes.size() - 1) > TAP_RESET_TIME_MS) {
                tapTimes.clear();
            }

            tapTimes.add(currentTime);

            if (tapTimes.size() >= 2) {
                long totalInterval = 0;
                for (int i = 1; i < tapTimes.size(); i++) {
                    totalInterval += (tapTimes.get(i) - tapTimes.get(i - 1));
                }

                long avgInterval = totalInterval / (tapTimes.size() - 1);
                int calculatedTempo = Math.round(60000f / avgInterval);

                if (calculatedTempo >= 20 && calculatedTempo <= 280) {
                    tempo = calculatedTempo;
                    tempoSeekBar.setProgress(tempo - 20);
                    tempoValue.setText("Tempo: " + tempo + " BPM");
                    SharedPreferences prefs = requireActivity().getSharedPreferences("MetronomePrefs", Context.MODE_PRIVATE);
                    prefs.edit().putInt("savedTempo", tempo).apply();
                }
            }
        });
    }


    private void showBpmDialog() {
        final String[] bpmOptions = {"+1 BPM", "+2 BPM", "+5 BPM"};
        final int[] bpmValues = {1, 2, 5};
        final int[] selectedIndex = {0};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.AppTheme_Dialog);
        builder.setTitle("Choose the tempo increment");

        builder.setSingleChoiceItems(bpmOptions, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedIndex[0] = which;
            }
        });

        builder.setPositiveButton("Start", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int selectedBpm = bpmValues[selectedIndex[0]];
                startSpeedCoach(selectedBpm);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void startSpeedCoach(int bpmIncrease) {
        if (isPlaying) {
            stopMetronome();
        }

        isPlaying = true;
        startStopBut.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.outline_pause_24));
        Toast.makeText(requireContext(), "Speed Coach started", Toast.LENGTH_SHORT).show();

        startMetronomeInternal();

        final int maxTempo = 280;
        final long stepInterval = 3000;

        tempoHandler.removeCallbacksAndMessages(null);

        tempoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isPlaying) return;

                if (tempo + bpmIncrease <= maxTempo) {
                    tempo += bpmIncrease;
                    tempoSeekBar.setProgress(tempo - 20);
                    tempoValue.setText("Tempo: " + tempo + " BPM");

                    requireActivity().getSharedPreferences("MetronomePrefs", Context.MODE_PRIVATE)
                            .edit().putInt("savedTempo", tempo).apply();

                    tempoHandler.postDelayed(this, stepInterval);
                } else {
                    Toast.makeText(requireContext(), "Max tempo reached: " + tempo, Toast.LENGTH_SHORT).show();
                }
            }
        }, stepInterval);
    }
    private void setupButtons() {
        Runnable increaseTempoRunnable = new Runnable() {
            @Override
            public void run() {
                if (tempo < 280) {
                    tempo++;
                    SharedPreferences prefs = requireActivity().getSharedPreferences("MetronomePrefs", Context.MODE_PRIVATE);
                    prefs.edit().putInt("savedTempo", tempo).apply();
                    tempoSeekBar.setProgress(tempo - 20);
                    tempoValue.setText("Tempo: " + tempo + " BPM");
                    tempoHandler.postDelayed(this, 100);
                }
            }
        };

        increaseTempoBtn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    tempoHandler.post(increaseTempoRunnable);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    tempoHandler.removeCallbacks(increaseTempoRunnable);
                    return true;
            }
            return false;
        });

        Runnable decreaseTempoRunnable = new Runnable() {
            @Override
            public void run() {
                if (tempo > 20) {
                    tempo--;
                    SharedPreferences prefs = requireActivity().getSharedPreferences("MetronomePrefs", Context.MODE_PRIVATE);
                    prefs.edit().putInt("savedTempo", tempo).apply();
                    tempoSeekBar.setProgress(tempo - 20);
                    tempoValue.setText("Tempo: " + tempo + " BPM");
                    tempoHandler.postDelayed(this, 100);
                }
            }
        };

        decreaseTempoBtn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    tempoHandler.post(decreaseTempoRunnable);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    tempoHandler.removeCallbacks(decreaseTempoRunnable);
                    return true;
            }
            return false;
        });
    }
}


