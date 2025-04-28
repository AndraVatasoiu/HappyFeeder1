package com.example.happyfeeder;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
        existingMealsContainer = findViewById(R.id.existingMealsContainer);  // Container pentru mesele deja existente
        fStore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Nu ești logat!", Toast.LENGTH_SHORT).show();
            finish();
        }

        usernameFromIntent = getIntent().getStringExtra("username");

        if (usernameFromIntent == null || usernameFromIntent.isEmpty()) {
            Toast.makeText(this, "Eroare: username lipsă!", Toast.LENGTH_SHORT).show();
            finish();
        }

        addMealCard(false, null);  // Adaugă un card nou

        Button buttonBack = findViewById(R.id.button_back);
        buttonBack.setOnClickListener(v -> finish());

        Button buttonSave = findViewById(R.id.button_save);
        buttonSave.setOnClickListener(v -> saveMeals());

        // Încarcă mesele deja existente
        loadExistingMeals();
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

            // Aplică fundalul gri pentru mesele deja adăugate
            mealCard.setBackgroundResource(R.drawable.bg_rounded_gray);
        } else {
            // Mesele noi au alt fundal, de exemplu roșu
            mealCard.setBackgroundResource(R.drawable.bg_rounded_white);  // Poți schimba în funcție de preferințe
        }

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

        buttonRemoveMeal.setOnClickListener(v -> mealCardContainer.removeView(mealCard));

        // Adaugă cardul în containerul corespunzător
        if (isSaved) {
            existingMealsContainer.addView(mealCard);  // Mesele existente
        } else {
            mealCardContainer.addView(mealCard);  // Mesele noi
        }
    }

    private void saveMeals() {
        int count = mealCardContainer.getChildCount();

        if (count == 0) {
            Toast.makeText(this, "Adaugă cel puțin o masă!", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference usersRef = fStore.collection("users");

        // Căutăm utilizatorul după username (nu după document ID)
        usersRef.whereEqualTo("username", usernameFromIntent)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            DocumentReference userDocRef = documentSnapshot.getReference();

                            // Obținem mesele deja existente
                            userDocRef.get()
                                    .addOnSuccessListener(document -> {
                                        Map<String, Object> existingMeals = (Map<String, Object>) document.get("meals");
                                        if (existingMeals == null) {
                                            existingMeals = new HashMap<>();
                                        }

                                        // Creăm un Map pentru noile mese
                                        Map<String, Object> mealsMap = new HashMap<>();

                                        // Adăugăm mesele noi
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

                                            // Generăm un identificator unic pentru fiecare masă
                                            String newMealId = "meal" + (existingMeals.size() + 1);

                                            Map<String, Object> mealDetails = new HashMap<>();
                                            mealDetails.put("nume_masa", mealName);
                                            mealDetails.put("cantitate", mealQuantity);
                                            mealDetails.put("ora_mesei", mealTime);

                                            mealsMap.put(newMealId, mealDetails); // Adăugăm masa în map-ul noilor mese
                                        }

                                        // Adăugăm noile mese la mesele existente
                                        existingMeals.putAll(mealsMap);

                                        // Salvăm toate mesele (existente și noile mese) în Firestore
                                        Map<String, Object> updateData = new HashMap<>();
                                        updateData.put("meals", existingMeals);

                                        // Actualizăm documentul utilizatorului cu noile mese
                                        userDocRef.update(updateData)
                                                .addOnSuccessListener(unused -> {
                                                    Toast.makeText(AddMealActivity.this, "Mesele au fost salvate în Firestore!", Toast.LENGTH_LONG).show();
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(AddMealActivity.this, "Eroare la salvarea meselor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(AddMealActivity.this, "Eroare la obținerea meselor existente: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(AddMealActivity.this, "Utilizatorul nu a fost găsit!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddMealActivity.this, "Eroare la căutarea utilizatorului: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Încarcă mesele deja existente
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
                                            // Afisează mesele deja existente
                                            for (Map.Entry<String, Object> entry : existingMeals.entrySet()) {
                                                Map<String, Object> mealDetails = (Map<String, Object>) entry.getValue();
                                                addMealCard(true, mealDetails);  // Adaugă mesele existente cu fundal gri
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
}
