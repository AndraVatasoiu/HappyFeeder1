package com.example.happyfeeder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;

public class PetWeightActivity extends AppCompatActivity {

    private TableLayout weightTable;
    private FirebaseFirestore db;
    private DatabaseReference realtimeDbRef;
    private String username;
    private String petId; // ID-ul animalului
    private String currentDate;
    private float currentWeight;

    private TextView currentWeightTextView; // pentru afisare greutate curenta

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_weight);

        weightTable = findViewById(R.id.weightTable);
        currentWeightTextView = findViewById(R.id.currentWeightTextView);
        Button backButton = findViewById(R.id.backButton);

        currentDate = LocalDate.now().toString(); // ex: 2025-05-24

        db = FirebaseFirestore.getInstance();
        realtimeDbRef = FirebaseDatabase.getInstance().getReference();

        SharedPreferences prefs = getSharedPreferences("userSession", MODE_PRIVATE);
        username = prefs.getString("loggedUsername", null);

        if (username == null) {
            Toast.makeText(this, "Utilizator neautentificat!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Pas 1: Ia petId si greutatea initiala din Firestore
        db.collection("pets")
                .whereEqualTo("owner_username", username)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot petDoc = snapshot.getDocuments().get(0);
                        petId = petDoc.getId();
                        String weightStr = petDoc.getString("weight");
                        if (weightStr != null) {
                            try {
                                currentWeight = Float.parseFloat(weightStr);
                            } catch (NumberFormatException e) {
                                currentWeight = 0f;
                            }
                        } else {
                            currentWeight = 0f;
                        }
                        // Pas 2: Verifica greutatea din Realtime DB si actualizeaza currentWeight daca e cazul
                        checkRealtimeWeightAndLoad();
                    } else {
                        Toast.makeText(this, "Nu s-a găsit animalul!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Eroare la accesarea datelor animalului.", Toast.LENGTH_SHORT).show());

        backButton.setOnClickListener(v -> {
            startActivity(new Intent(PetWeightActivity.this, HomeActivity.class));
            finish();
        });
    }

    private void checkRealtimeWeightAndLoad() {
        // Referinta exacta in Realtime Database
        DatabaseReference greutateRealtimeRef = realtimeDbRef.child("users")
                .child("Q0rrPOn4ZJfUpyqWI0xuKDliPP02")
                .child("nextMeal")
                .child("greutateAnimal");

        greutateRealtimeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double greutateRealtime = snapshot.getValue(Double.class);
                    if (greutateRealtime != null) {
                        Log.d("PetWeightActivity", "Greutate din Realtime DB: " + greutateRealtime);
                        Toast.makeText(PetWeightActivity.this, "Greutate RTDB: " + greutateRealtime, Toast.LENGTH_SHORT).show();

                        if (greutateRealtime >= 500) {
                            currentWeight = greutateRealtime.floatValue();
                            Log.d("PetWeightActivity", "Greutate actualizata cu RTDB: " + currentWeight);
                            Toast.makeText(PetWeightActivity.this, "Greutate actualizata la RTDB: " + currentWeight, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d("PetWeightActivity", "Greutatea RTDB mai mica de 500, se pastreaza Firestore: " + currentWeight);
                            Toast.makeText(PetWeightActivity.this, "Greutate ramane Firestore: " + currentWeight, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d("PetWeightActivity", "Greutatea RTDB este null");
                    }
                } else {
                    Log.d("PetWeightActivity", "Greutatea RTDB nu exista");
                }
                // Afiseaza greutatea curenta pe ecran
                currentWeightTextView.setText("Greutate curenta: " + currentWeight + " kg");

                // NU mai incarca istoricul in tabel (am comentat apelul)
                // loadWeightHistory();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PetWeightActivity.this, "Eroare la citirea din Realtime DB", Toast.LENGTH_SHORT).show();
                // NU mai incarca istoricul in tabel
                // loadWeightHistory();
            }
        });
    }

    // Eliminam sau comentam restul metodelor legate de incarcare tabel (optional, pentru claritate)

    /*
    private void loadWeightHistory() {
        // Nu mai folosim
    }

    private void saveTodayWeight() {
        // Nu mai folosim
    }

    private void addRowToTable(String date, String weight, String diff) {
        // Nu mai folosim
    }
    */

    // Clasă internă pentru modelul greutății (dacă vrei o poți păstra)
    public static class WeightEntry {
        public String date;
        public float weight;

        public WeightEntry() {} // constructor gol pt Firebase

        public WeightEntry(String date, float weight) {
            this.date = date;
            this.weight = weight;
        }

        public String getDate() {
            return date;
        }

        public String getWeight() {
            return String.valueOf(weight);
        }
    }
}
