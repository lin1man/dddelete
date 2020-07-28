package com.my.ddfiledelete;

import android.content.SharedPreferences;
import android.nfc.Tag;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

public class MainActivity extends AppCompatActivity {
    private static SharedPreferences settings;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Button btnReset = findViewById(R.id.btnReset);
        final Button btnLogSwitch = findViewById(R.id.btnLogSwitch);

        settings = getSharedPreferences(utils.strSettingsName, MODE_PRIVATE);
        final boolean bLogOn = settings.getBoolean(utils.strLogSwitchConfigName, false);
        if (bLogOn) {
            btnLogSwitch.setText(R.string.logon);
        } else {
            btnLogSwitch.setText(R.string.logoff);
        }

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settings.edit().putBoolean(utils.strResetConfigName, true).apply();
            }
        });
        btnLogSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean bLogOn = false;
                if (btnLogSwitch.getText().toString().equals(getString(R.string.logoff))) {
                    bLogOn = true;
                }
                if (bLogOn) {
                    btnLogSwitch.setText(R.string.logon);
                } else {
                    btnLogSwitch.setText(R.string.logoff);
                }
                settings.edit().putBoolean(utils.strLogSwitchConfigName, bLogOn).apply();
            }
        });

        Spinner spinner_mode = findViewById(R.id.spin_mode);
        spinner_mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String mode = parent.getItemAtPosition(position).toString();
                switch (mode) {
                    case "我创建的":
                        settings.edit().putInt(utils.strModeConfigName, utils.n_mode_my_group).apply();
                        break;
                    case "我加入的":
                        settings.edit().putInt(utils.strModeConfigName, utils.n_mode_my_join_group).apply();
                        break;
                    case "消息":
                        settings.edit().putInt(utils.strModeConfigName, utils.n_mode_msg).apply();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
