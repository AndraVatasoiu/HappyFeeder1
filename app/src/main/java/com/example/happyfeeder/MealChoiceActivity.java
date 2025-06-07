package com.example.happyfeeder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MealChoiceActivity extends AppCompatActivity {

    private LinearLayout cardAuto, cardManual;
    private Button buttonBack;
    private String usernameFromIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_choice); // asigură-te că XML-ul tău se numește așa

        // Preluăm elementele din XML
        cardAuto = findViewById(R.id.cardAuto);
        cardManual = findViewById(R.id.cardManual);
        buttonBack = findViewById(R.id.button_back);

        // Preluăm username-ul din Intent
        usernameFromIntent = getIntent().getStringExtra("username");
        if (usernameFromIntent == null || usernameFromIntent.isEmpty()) {
            Toast.makeText(this, "Eroare: username lipsă!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Click pe cardul "Programare automată"
        cardAuto.setOnClickListener(v -> {
            Intent intent = new Intent(MealChoiceActivity.this, AutoMealActivity.class);
            intent.putExtra("username", usernameFromIntent);
            startActivity(intent);
        });

        // Click pe cardul "Programare manuală"
        cardManual.setOnClickListener(v -> {
            Intent intent = new Intent(MealChoiceActivity.this, AddMealActivity.class);
            intent.putExtra("username", usernameFromIntent);
            startActivity(intent);
        });

        // Click pe butonul Înapoi
        buttonBack.setOnClickListener(v -> finish());
    }
}
