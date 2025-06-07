package com.example.happyfeeder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PetWeightActivity extends AppCompatActivity {

    private TableLayout weightTable;
    private FirebaseFirestore db;
    private String username;
    private String petId; // ID-ul animalului
    private String currentDate;
    private float currentWeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_weight);

        weightTable = findViewById(R.id.weightTable);
        Button backButton = findViewById(R.id.backButton);

        currentDate = LocalDate.now().toString(); // ex: 2025-05-24

        db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = getSharedPreferences("userSession", MODE_PRIVATE);
        username = prefs.getString("loggedUsername", null);

        if (username == null) {
            Toast.makeText(this, "Utilizator neautentificat!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Pasul 1: Caută animalul asociat
        db.collection("pets")
                .whereEqualTo("owner_username", username)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot petDoc = snapshot.getDocuments().get(0);
                        petId = petDoc.getId();
                        currentWeight = Float.parseFloat(petDoc.getString("weight"));

                        // Pasul 2: încarcă istoricul greutăților
                        loadWeightHistory();
                    } else {
                        Toast.makeText(this, "Nu s-a găsit animalul!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Eroare la accesarea datelor animalului.", Toast.LENGTH_SHORT).show());

        // Buton Înapoi
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(PetWeightActivity.this, HomeActivity.class));
            finish();
        });
    }

    private void loadWeightHistory() {
        CollectionReference weightsRef = db.collection("pets")
                .document(petId)
                .collection("weights");

        weightsRef.get().addOnSuccessListener(querySnapshot -> {
            List<DocumentSnapshot> docs = querySnapshot.getDocuments();
            List<WeightEntry> entries = new ArrayList<>();

            for (DocumentSnapshot doc : docs) {
                String date = doc.getString("date");
                String weightStr = doc.getString("weight");
                if (date != null && weightStr != null) {
                    try {
                        float w = Float.parseFloat(weightStr);
                        entries.add(new WeightEntry(date, w));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Sortează după dată (descrescător)
            Collections.sort(entries, (a, b) -> b.date.compareTo(a.date));

            // Adaugă rândurile existente
            for (int i = 0; i < entries.size(); i++) {
                WeightEntry current = entries.get(i);
                String diff = "—";

                if (i + 1 < entries.size()) {
                    float delta = current.weight - entries.get(i + 1).weight;
                    diff = String.format("%.2f kg", delta);
                }

                addRowToTable(current.date, current.weight + " kg", diff);
            }

            // Verifică dacă data de azi e deja adăugată
            boolean todayExists = entries.stream().anyMatch(e -> e.date.equals(currentDate));
            if (!todayExists) {
                saveTodayWeight();
                addRowToTable(currentDate, currentWeight + " kg", "—");
            }

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Eroare la încărcarea istoricului.", Toast.LENGTH_SHORT).show());
    }

    private void saveTodayWeight() {
        DocumentReference todayRef = db.collection("pets")
                .document(petId)
                .collection("weights")
                .document(currentDate);

        todayRef.set(new WeightEntry(currentDate, currentWeight));
    }

    private void addRowToTable(String date, String weight, String diff) {
        TableRow row = new TableRow(this);

        TextView dateView = new TextView(this);
        TextView weightView = new TextView(this);
        TextView diffView = new TextView(this);

        int padding = 8;
        dateView.setPadding(padding, padding, padding, padding);
        weightView.setPadding(padding, padding, padding, padding);
        diffView.setPadding(padding, padding, padding, padding);

        dateView.setText(date);
        weightView.setText(weight);
        diffView.setText(diff);

        row.addView(dateView);
        row.addView(weightView);
        row.addView(diffView);

        weightTable.addView(row);
    }

    // Clasă internă pentru modelul greutății
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
