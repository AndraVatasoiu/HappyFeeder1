package com.example.happyfeeder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private TextView welcomeText, petNameText, petWeightText, mealTimeText;
    private ImageView petImage;
    private ProgressBar foodProgressBar;
    private Button logoutButton;
    private LinearLayout mealTimesContainer;

    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private CollectionReference usersRef, petsRef;

    private CountDownTimer mealCountdownTimer;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private String username;
    private ImageButton menuButton;
    private Button addPetButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeViews();
        setupFirebase();
        setupClickListeners();
    }

    private void initializeViews() {
        welcomeText = findViewById(R.id.welcomeText);
        petNameText = findViewById(R.id.petNameText);
        petWeightText = findViewById(R.id.petWeightText);
        mealTimeText = findViewById(R.id.mealTime);
        foodProgressBar = findViewById(R.id.foodProgressBar);
        logoutButton = findViewById(R.id.logoutButton);
        mealTimesContainer = findViewById(R.id.mealTimesContainer);
        menuButton = findViewById(R.id.menuButton);
        addPetButton = findViewById(R.id.addPetButton);

    }

    private void setupFirebase() {
        SharedPreferences prefs = getSharedPreferences("userSession", MODE_PRIVATE);
        username = prefs.getString("loggedUsername", null);

        if (username == null) {
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
            return;
        }

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Nu ești logat!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");
        petsRef = db.collection("pets");

        loadUserData();
        loadPetData();
        loadMealTimes();
    }

    private void setupClickListeners() {
        findViewById(R.id.cardMese).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MealChoiceActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            getSharedPreferences("userSession", MODE_PRIVATE)
                    .edit()
                    .remove("loggedUsername")
                    .apply();
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
        });
        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MenuActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        addPetButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddPetActivity.class);
            intent.putExtra("username", username); // dacă vrei să trimiți username mai departe
            startActivity(intent);
        });
        findViewById(R.id.cardGreutate).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, PetWeightActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });
        findViewById(R.id.cardMesaj).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, RecordAudioActivity.class);
            startActivity(intent);
        });


    }

    private void loadUserData() {
        usersRef.whereEqualTo("username", username)
                .get().addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        welcomeText.setText("Bună, " + doc.getString("username"));
                    }
                });
    }

    private void loadPetData() {
        petsRef.whereEqualTo("owner_username", username).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                petNameText.setText(document.getString("name"));
                                Long foodLevel = document.getLong("foodLevel");
                                foodProgressBar.setProgress(foodLevel != null ? foodLevel.intValue() : 0);
                                petWeightText.setText("Greutatea animalului: " + document.getString("weight") + " kg");
                            }
                        } else {
                            petNameText.setText("Nu ai adăugat încă un animal.");
                        }
                    }
                });
    }

    private void loadMealTimes() {
        if (mealCountdownTimer != null) {
            mealCountdownTimer.cancel();
        }

        usersRef.whereEqualTo("username", username).get().addOnSuccessListener(querySnapshot -> {
            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                Object mealsObj = userDoc.get("meals");

                mealTimesContainer.removeAllViews();
                List<LocalTime> mealTimes = new ArrayList<>();

                if (mealsObj instanceof Map) {
                    Map<String, Object> mealsMap = (Map<String, Object>) mealsObj;

                    for (Map.Entry<String, Object> entry : mealsMap.entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof Map) {
                            Map<String, Object> mealMap = (Map<String, Object>) value;
                            String timeStr = (String) mealMap.get("ora_mesei");

                            if (timeStr != null) {
                                try {
                                    LocalTime time = LocalTime.parse(timeStr, timeFormatter);
                                    mealTimes.add(time);
                                    addMealTimeCard(timeStr);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    if (!mealTimes.isEmpty()) {
                        Collections.sort(mealTimes);
                        startMealCountdown(mealTimes);
                    } else {
                        mealTimeText.setText("Nicio masă programată.");
                    }
                } else {
                    mealTimeText.setText("Nu există mese salvate.");
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(HomeActivity.this, "Eroare la încărcarea meselor.", Toast.LENGTH_SHORT).show();
        });
    }

    private void startMealCountdown(List<LocalTime> sortedTimes) {
        LocalTime now = LocalTime.now();
        LocalTime nextMeal = null;

        // Găsește prima masă care este după ora curentă
        for (LocalTime time : sortedTimes) {
            if (time.isAfter(now)) {
                nextMeal = time;
                break;
            }
        }

        long millis;
        if (nextMeal == null) {
            // Dacă nu mai sunt mese astăzi, luăm prima masă de mâine
            nextMeal = sortedTimes.get(0);
            long millisUntilMidnight = Duration.between(now, LocalTime.MAX).toMillis() + 1000; // +1 sec pentru a trece la ziua următoare
            long millisFromMidnightToNextMeal = Duration.between(LocalTime.MIDNIGHT, nextMeal).toMillis();
            millis = millisUntilMidnight + millisFromMidnightToNextMeal;
        } else {
            millis = Duration.between(now, nextMeal).toMillis();
        }

        launchCountdown(millis, nextMeal);
    }

    private void launchCountdown(long millis, LocalTime nextMealTime) {
        if (mealCountdownTimer != null) {
            mealCountdownTimer.cancel();
        }

        mealCountdownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long ms) {
                long h = ms / 3600000;
                long m = (ms % 3600000) / 60000;
                long s = (ms % 60000) / 1000;
                String countdownText = String.format("%02dh %02dm %02ds", h, m, s);
                mealTimeText.setText(countdownText);
            }

            @Override
            public void onFinish() {
                // Când expiră timer-ul, reîncărcăm mesele pentru a afișa următoarea masă
                loadMealTimes();
            }
        }.start();
    }

    private void addMealTimeCard(String time) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View timeCard = inflater.inflate(R.layout.item_meal_time, mealTimesContainer, false);

        TextView timeText = timeCard.findViewById(R.id.meal_time_text);
        timeText.setText(time);

        mealTimesContainer.addView(timeCard);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPetData();
        loadMealTimes(); // Reîncarcă mesele când activitatea revine în prim-plan
    }

    @Override
    protected void onDestroy() {
        if (mealCountdownTimer != null) {
            mealCountdownTimer.cancel();
        }
        super.onDestroy();
    }
}