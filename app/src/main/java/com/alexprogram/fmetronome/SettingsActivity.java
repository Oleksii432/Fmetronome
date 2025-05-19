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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {
    private String[] itemList = new String[] {"Appearance", "Keep screen on"};
    private String[] subList = new String[] {"Change theme settings", "Do not turn off the screen"};
    private int[] icons = {
            R.drawable.baseline_bedtime_24,
            R.drawable.baseline_remove_red_eye_24
    };
    private ListView settingListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        boolean isEnabled = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("keep_screen_on", false);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "MyApp::KeepScreenOn");

        if (isEnabled) {
            if (!wakeLock.isHeld()) {
                wakeLock.acquire(10 * 60 * 1000L);
            }
        } else {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }

        settingListView = findViewById(R.id.settingListView);

        CustomAdapter adapter = new CustomAdapter(this, itemList, subList, icons);

        settingListView.setAdapter(adapter);

        MaterialToolbar toolbar = findViewById(R.id.settingMaterialToolbar);
        setSupportActionBar(toolbar);

        settingListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (itemList[position].equals("Appearance")) {
                    showThemeDialog();
                } else if (itemList[position].equals("Keep screen on")) {
                    showKeepScreenOnDialog();
                }
            }
        });
    }

    private void showThemeDialog() {
        String[] themes = {"Light", "Dark", "Auto"};

        int currentTheme = AppCompatDelegate.getDefaultNightMode();
        int checkedItem = 0;

        if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            checkedItem = 1;
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            checkedItem = 2;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_Dialog);
        builder.setTitle("Select Theme")
                .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                    SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    switch (which) {
                        case 0:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            editor.putInt("theme", AppCompatDelegate.MODE_NIGHT_NO);
                            break;
                        case 1:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            editor.putInt("theme", AppCompatDelegate.MODE_NIGHT_YES);
                            break;
                        case 2:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            editor.putInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            break;
                    }
                    editor.apply();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showKeepScreenOnDialog() {
        boolean isEnabled = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("keep_screen_on", false);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_keep_screen_on, null);

        SwitchMaterial aSwitch = dialogView.findViewById(R.id.switch_keep_screen_on);
        aSwitch.setChecked(isEnabled);

        new AlertDialog.Builder(this)
                .setTitle("Keep screen on")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    boolean newState = aSwitch.isChecked();

                    getSharedPreferences("settings", MODE_PRIVATE)
                            .edit()
                            .putBoolean("keep_screen_on", newState)
                            .apply();

                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "MyApp::KeepScreenOn");

                    if (newState) {
                        if (!wakeLock.isHeld()) {
                            wakeLock.acquire(10 * 60 * 1000L);
                        }
                        Toast.makeText(this, "Screen will stay on", Toast.LENGTH_SHORT).show();
                    } else {
                        if (wakeLock.isHeld()) {
                            wakeLock.release();
                        }
                        Toast.makeText(this, "Screen may turn off", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isEnabled = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("keep_screen_on", false);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "MyApp::KeepScreenOn");

        if (isEnabled && !wakeLock.isHeld()) {
            wakeLock.acquire(10 * 60 * 1000L);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "MyApp::KeepScreenOn");

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
