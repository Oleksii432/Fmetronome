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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class AboutActivity extends AppCompatActivity {

    private String[] itemList = new String[] {"Github", "Version", "License"};
    private String[] subList = new String[] {"GitHub repository link", "1.0.0", "GPLv3" };
    private ListView aboutListView;
    private int[] icons = {
            R.drawable.baseline_code_24,
            R.drawable.baseline_info_outline_24,
            R.drawable.baseline_copyright_24
    };
    private int versionClickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        aboutListView = findViewById(R.id.aboutListView);
        CustomAdapter adapter = new CustomAdapter(this, itemList, subList, icons);
        aboutListView.setAdapter(adapter);

        MaterialToolbar toolbar = findViewById(R.id.aboutMaterialToolbar);
        setSupportActionBar(toolbar);

        aboutListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = itemList[position];

                switch (selectedItem) {
                    case "Github":
                        openUrlInBrowser("https://github.com/Oleksii432/Fmetronome");
                        break;

                    case "Version":
                        versionClickCount++;

                        if (versionClickCount > 4) {
                            Toast.makeText(AboutActivity.this, "Good luck :)", Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case "License":
                        openUrlInBrowser("https://github.com/Oleksii432/Fmetronome/blob/main/LICENSE");
                        break;

                    default:
                        break;
                }
            }
        });
    }

    private void openUrlInBrowser(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
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

