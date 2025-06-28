package com.example.happyfeeder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Button btnChangeEmail = findViewById(R.id.btnChangeEmail);
        Button btnChangePassword = findViewById(R.id.btnChangePassword);

        btnChangeEmail.setOnClickListener(v -> {
            startActivity(new Intent(this, ChangeEmailActivity.class));
        });

        btnChangePassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ChangePasswordActivity.class));
        });
    }
}