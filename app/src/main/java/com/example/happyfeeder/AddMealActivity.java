package com.example.happyfeeder;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddMealActivity extends AppCompatActivity {

    private LinearLayout mealCardContainer;
    private LinearLayout existingMealsContainer;
    private ImageView buttonAdd;
    private FirebaseFirestore fStore;
    private FirebaseUser currentUser;
    private String usernameFromIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_meal);

        mealCardContainer = findViewById(R.id.mealCardContainer);
        existingMealsContainer = findViewById(R.id.existingMealsContainer);
        fStore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Nu ești logat!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        usernameFromIntent = getIntent().getStringExtra("username");
        if (usernameFromIntent == null || usernameFromIntent.isEmpty()) {
            Toast.makeText(this, "Eroare: username lipsă!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        addMealCard(false, null); // Adaugă un card nou pentru mese noi

        Button buttonBack = findViewById(R.id.button_back);
        buttonBack.setOnClickListener(v -> finish());

        Button buttonSave = findViewById(R.id.button_save);
        buttonSave.setOnClickListener(v -> saveMeals());

        loadExistingMeals(); // Încarcă mesele existente
    }

    private void addMealCard(boolean isSaved, Map<String, Object> existingMeal) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View mealCard = inflater.inflate(R.layout.meal_input, null);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        mealCard.setLayoutParams(params);

        EditText editMealName = mealCard.findViewById(R.id.edit_meal_name);
        EditText editMealQuantity = mealCard.findViewById(R.id.edit_meal_quantity);
        EditText editMealTime = mealCard.findViewById(R.id.edit_meal_time);
        ImageView buttonRemoveMeal = mealCard.findViewById(R.id.button_remove_meal);

        if (isSaved && existingMeal != null) {
            editMealName.setText((String) existingMeal.get("nume_masa"));
            editMealQuantity.setText((String) existingMeal.get("cantitate"));
            editMealTime.setText((String) existingMeal.get("ora_mesei"));

            mealCard.setBackgroundResource(R.drawable.bg_rounded_gray);

            // Blocăm editarea pentru mesele existente
            editMealName.setEnabled(false);
            editMealQuantity.setEnabled(false);
            editMealTime.setEnabled(false);

            editMealName.setFocusable(false);
            editMealQuantity.setFocusable(false);
            editMealTime.setFocusable(false);

            editMealTime.setClickable(false);

        } else {
            mealCard.setBackgroundResource(R.drawable.bg_rounded_white);
        }

        // Time picker doar pentru mesele noi
        if (!isSaved) {
            editMealTime.setFocusable(false);
            editMealTime.setClickable(true);
            editMealTime.setOnClickListener(v -> {
                final Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        AddMealActivity.this,
                        (view, hourOfDay, minute1) -> {
                            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                            editMealTime.setText(formattedTime);
                        },
                        hour,
                        minute,
                        true
                );
                timePickerDialog.show();
            });
        }

        buttonRemoveMeal.setOnClickListener(v -> {
            if (isSaved) {
                // Pentru mesele salvate în Firestore
                String mealId = (String) buttonRemoveMeal.getTag();
                if (mealId != null) {
                    deleteMealFromFirestore(mealId, mealCard);
                }
            } else {
                // Pentru mesele noi (nesalvate încă)
                mealCardContainer.removeView(mealCard);
            }
        });

        if (isSaved) {
            existingMealsContainer.addView(mealCard);
        } else {
            mealCardContainer.addView(mealCard);
        }
    }

    private void deleteMealFromFirestore(String mealId, View mealCard) {
        CollectionReference usersRef = fStore.collection("users");
        usersRef.whereEqualTo("username", usernameFromIntent)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            DocumentReference userDocRef = documentSnapshot.getReference();
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("meals." + mealId, com.google.firebase.firestore.FieldValue.delete()); // AICI E SECRETUL

                            userDocRef.update(updates)
                                    .addOnSuccessListener(unused -> {
                                        existingMealsContainer.removeView(mealCard);
                                        Toast.makeText(AddMealActivity.this, "Masa a fost ștearsă complet!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(AddMealActivity.this, "Eroare la ștergere: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(AddMealActivity.this, "Eroare la căutarea utilizatorului: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void saveMeals() {
        int count = mealCardContainer.getChildCount();
        if (count == 0) {
            Toast.makeText(this, "Adaugă cel puțin o masă!", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference usersRef = fStore.collection("users");
        usersRef.whereEqualTo("username", usernameFromIntent)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            DocumentReference userDocRef = documentSnapshot.getReference();
                            userDocRef.get()
                                    .addOnSuccessListener(document -> {
                                        Map<String, Object> existingMeals = (Map<String, Object>) document.get("meals");
                                        if (existingMeals == null) {
                                            existingMeals = new HashMap<>();
                                        }

                                        Map<String, Object> mealsMap = new HashMap<>();

                                        for (int i = 0; i < count; i++) {
                                            View card = mealCardContainer.getChildAt(i);
                                            EditText name = card.findViewById(R.id.edit_meal_name);
                                            EditText quantity = card.findViewById(R.id.edit_meal_quantity);
                                            EditText time = card.findViewById(R.id.edit_meal_time);

                                            String mealName = name.getText().toString().trim();
                                            String mealQuantity = quantity.getText().toString().trim();
                                            String mealTime = time.getText().toString().trim();

                                            if (mealName.isEmpty() || mealQuantity.isEmpty() || mealTime.isEmpty()) {
                                                Toast.makeText(this, "Completează toate câmpurile pentru masa #" + (i + 1), Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            String newMealId = "meal" + (existingMeals.size() + mealsMap.size() + 1);

                                            Map<String, Object> mealDetails = new HashMap<>();
                                            mealDetails.put("nume_masa", mealName);
                                            mealDetails.put("cantitate", mealQuantity);
                                            mealDetails.put("ora_mesei", mealTime);

                                            mealsMap.put(newMealId, mealDetails);
                                        }

                                        existingMeals.putAll(mealsMap);

                                        Map<String, Object> updateData = new HashMap<>();
                                        updateData.put("meals", existingMeals);

                                        userDocRef.update(updateData)
                                                .addOnSuccessListener(unused -> {
                                                    Toast.makeText(AddMealActivity.this, "Mesele au fost salvate!", Toast.LENGTH_LONG).show();
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(AddMealActivity.this, "Eroare la salvare: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    });
                        }
                    } else {
                        Toast.makeText(AddMealActivity.this, "Utilizatorul nu a fost găsit!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(AddMealActivity.this, "Eroare la căutare: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadExistingMeals() {
        CollectionReference usersRef = fStore.collection("users");
        usersRef.whereEqualTo("username", usernameFromIntent)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            DocumentReference userDocRef = documentSnapshot.getReference();
                            userDocRef.get()
                                    .addOnSuccessListener(document -> {
                                        Map<String, Object> existingMeals = (Map<String, Object>) document.get("meals");
                                        if (existingMeals != null && !existingMeals.isEmpty()) {
                                            for (Map.Entry<String, Object> entry : existingMeals.entrySet()) {
                                                Map<String, Object> mealDetails = (Map<String, Object>) entry.getValue();
                                                String mealId = entry.getKey();

                                                addMealCard(true, mealDetails);

                                                int lastIndex = existingMealsContainer.getChildCount() - 1;
                                                View mealCard = existingMealsContainer.getChildAt(lastIndex);
                                                ImageView buttonRemoveMeal = mealCard.findViewById(R.id.button_remove_meal);

                                                buttonRemoveMeal.setTag(mealId); // important pentru ștergere
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
}
