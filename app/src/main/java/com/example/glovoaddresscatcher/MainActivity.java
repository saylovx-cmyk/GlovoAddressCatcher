package com.example.glovoaddresscatcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 80, 40, 40);

        TextView title = new TextView(this);
        title.setText("Glovo Address Catcher");
        title.setTextSize(24);

        TextView info = new TextView(this);
        info.setText(
                "Приложение сохраняет адрес доставки, " +
                "который появляется на экране Glovo."
        );
        info.setTextSize(16);

        Button accessibility = new Button(this);
        accessibility.setText("Включить доступ");

        accessibility.setOnClickListener(v -> {
            Intent intent =
                    new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        layout.addView(title);
        layout.addView(info);
        layout.addView(accessibility);

        setContentView(layout);
    }
    }
