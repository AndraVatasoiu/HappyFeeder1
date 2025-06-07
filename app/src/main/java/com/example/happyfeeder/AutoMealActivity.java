package com.example.happyfeeder;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AutoMealActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String username;
    private String petId;
    private float currentWeight;

    private LinearLayout mealCardContainer;
    private Button buttonSave, buttonBack;

    // Orele meselor
    private final String[] mealTimes = {"08:00", "12:00", "16:00"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auto_meal);

        mealCardContainer = findViewById(R.id.mealCardContainer);
        buttonSave = findViewById(R.id.button_save_auto);
        buttonBack = findViewById(R.id.button_back_auto);

        db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = getSharedPreferences("userSession", MODE_PRIVATE);
        username = prefs.getString("loggedUsername", null);

        if (username == null) {
            Toast.makeText(this, "Utilizator neautentificat!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Preia greutatea animalului
        db.collection("pets")
                .whereEqualTo("owner_username", username)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot petDoc = snapshot.getDocuments().get(0);
                        petId = petDoc.getId();

                        // Dacă greutatea e stocată ca String
                        String weightStr = petDoc.getString("weight");
                        if (weightStr != null) {
                            try {
                                currentWeight = Float.parseFloat(weightStr);
                            } catch (NumberFormatException e) {
                                currentWeight = 0f;
                            }
                        } else {
                            // Dacă e stocată ca număr
                            Double weightDouble = petDoc.getDouble("weight");
                            currentWeight = weightDouble != null ? weightDouble.floatValue() : 0f;
                        }

                        if (currentWeight > 0) {
                            showMeals(currentWeight);
                        } else {
                            Toast.makeText(this, "Greutate invalidă a animalului.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Nu s-a găsit animalul!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Eroare la accesarea datelor animalului.", Toast.LENGTH_SHORT).show());

        buttonBack.setOnClickListener(v -> finish());

        buttonSave.setOnClickListener(v -> {
            // Aici poți adăuga logica de salvare a programării meselor în baza de date
            Toast.makeText(this, "Mesele au fost salvate (funcționalitate de implementat)", Toast.LENGTH_SHORT).show();
        });
    }

    private void showMeals(float weightKg) {
        mealCardContainer.removeAllViews();

        // Calculăm cantitatea per masă (15g * kg)
        int gramsPerMeal = (int) (15 * weightKg);

        for (String time : mealTimes) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_rounded_gray);
            card.setPadding(32, 32, 32, 32);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 32);
            card.setLayoutParams(params);

            TextView timeText = new TextView(this);
            timeText.setText("Ora mesei: " + time);
            timeText.setTextSize(18f);
            timeText.setTextColor(getResources().getColor(android.R.color.black));
            timeText.setPadding(0, 0, 0, 8);

            TextView amountText = new TextView(this);
            amountText.setText("Cantitate: " + gramsPerMeal + " grame");
            amountText.setTextSize(16f);
            amountText.setTextColor(getResources().getColor(android.R.color.darker_gray));

            card.addView(timeText);
            card.addView(amountText);

            mealCardContainer.addView(card);
        }
    }
}
