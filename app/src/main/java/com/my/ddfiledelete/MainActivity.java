package com.my.ddfiledelete;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private static SharedPreferences settings;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnReset = findViewById(R.id.btnReset);
        final Button btnLogSwitch = findViewById(R.id.btnLogSwitch);

        settings = getSharedPreferences(utils.strSettingsName, MODE_PRIVATE);
        boolean bLogOn = settings.getBoolean(utils.strLogSwitchConfigName, false);
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
    }
}
