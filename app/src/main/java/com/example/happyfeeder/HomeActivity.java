package com.example.happyfeeder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private TextView welcomeText, petNameText, petWeightText, mealTimeText, foodLevelText;
    private ImageView petImage;
    private ProgressBar foodProgressBar;
    private Button logoutButton;
    private LinearLayout mealTimesContainer;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private CollectionReference usersRef, petsRef;
    private DatabaseReference realtimeDb;
    private CountDownTimer mealCountdownTimer;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private String username;
    private ImageButton menuButton;
    private Button addPetButton;
    private Handler handler = new Handler();
    private Runnable updateImageTask;
    private StorageReference storageRef;
    private ImageView imageLiveCamera;
    private View cardCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        imageLiveCamera = findViewById(R.id.imageLiveCamera);
        storageRef = FirebaseStorage.getInstance().getReference().child("images/");

        updateImageTask = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 6000);
            }
        };

        initializeViews();
        setupFirebase();
        setupClickListeners();

        updateFoodLevelFromRealtimeDb(); // Afișare nivel hrană la start
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
        cardCamera = findViewById(R.id.cardCamera);
        foodLevelText = findViewById(R.id.foodLevelText); // Nou
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
        realtimeDb = FirebaseDatabase.getInstance().getReference();

        loadUserData();
        loadPetData();
        usersRef.whereEqualTo("username", username)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("Firestore", "Listen failed.", e);
                        return;
                    }
                    if (snapshots != null && !snapshots.isEmpty()) {
                        loadMealTimes();
                    }
                });
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
            intent.putExtra("username", username);
            startActivity(intent);
        });

        findViewById(R.id.cardGreutate).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, PetWeightActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        cardCamera.setOnClickListener(v -> {
            DatabaseReference linkRef = realtimeDb.child("a0001").child("link");
            linkRef.get().addOnSuccessListener(dataSnapshot -> {
                String link = dataSnapshot.getValue(String.class);
                if (link != null && !link.isEmpty()) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(browserIntent);
                } else {
                    Toast.makeText(HomeActivity.this, "Link-ul nu a fost găsit.", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(HomeActivity.this, "Eroare la încărcarea link-ului.", Toast.LENGTH_SHORT).show();
            });
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

                                Object weightObj = document.get("weight");
                                if (weightObj != null) {
                                    String weightStr;
                                    if (weightObj instanceof Number) {
                                        weightStr = String.format("%.2f", ((Number) weightObj).doubleValue());
                                    } else {
                                        weightStr = weightObj.toString();
                                    }
                                    petWeightText.setText("Greutatea animalului: " + weightStr + " kg");
                                } else {
                                    petWeightText.setText("Greutatea animalului: N/A");
                                }
                            }
                        } else {
                            petNameText.setText("Nu ai adăugat încă un animal.");
                            petWeightText.setText("Greutatea animalului: N/A");
                            foodProgressBar.setProgress(0);
                        }
                    } else {
                        Toast.makeText(HomeActivity.this, "Eroare la încărcarea datelor despre animal.", Toast.LENGTH_SHORT).show();
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
                Map<LocalTime, Map<String, Object>> mealsMap = new HashMap<>();

                if (mealsObj instanceof Map) {
                    Map<String, Object> meals = (Map<String, Object>) mealsObj;

                    for (Map.Entry<String, Object> entry : meals.entrySet()) {
                        if (entry.getValue() instanceof Map) {
                            Map<String, Object> mealMap = (Map<String, Object>) entry.getValue();
                            String timeStr = (String) mealMap.get("ora_mesei");

                            if (timeStr != null) {
                                try {
                                    LocalTime time = LocalTime.parse(timeStr, timeFormatter);
                                    mealTimes.add(time);
                                    mealsMap.put(time, mealMap);
                                    addMealTimeCard(timeStr);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    if (!mealTimes.isEmpty()) {
                        Collections.sort(mealTimes);

                        LocalTime now = LocalTime.now();
                        LocalTime nextMealTime = null;

                        for (LocalTime time : mealTimes) {
                            if (time.isAfter(now)) {
                                nextMealTime = time;
                                break;
                            }
                        }

                        if (nextMealTime == null) {
                            nextMealTime = mealTimes.get(0);
                        }

                        int weightInGrams = 0;
                        Map<String, Object> nextMealMap = mealsMap.get(nextMealTime);
                        if (nextMealMap != null && nextMealMap.containsKey("cantitate")) {
                            try {
                                weightInGrams = Integer.parseInt(nextMealMap.get("cantitate").toString());
                            } catch (Exception e) {
                                weightInGrams = 0;
                            }
                        }

                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                            String currentHour = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

                            Map<String, Object> realtimeData = new HashMap<>();
                            realtimeData.put("time", nextMealTime.toString());
                            realtimeData.put("weight", weightInGrams);
                            realtimeData.put("oraCurenta", currentHour);

                            dbRef.child("users")
                                    .child(user.getUid())
                                    .child("nextMeal")
                                    .setValue(realtimeData);
                        }

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
        LocalDate today = LocalDate.now();
        LocalDateTime nextMealDateTime;
        LocalTime nextMeal = null;
        for (LocalTime time : sortedTimes) {
            if (time.isAfter(now)) {
                nextMeal = time;
                break;
            }
        }
        if (nextMeal == null) {
            nextMeal = sortedTimes.get(0);
            nextMealDateTime = LocalDateTime.of(today.plusDays(1), nextMeal);
        } else {
            nextMealDateTime = LocalDateTime.of(today, nextMeal);
        }
        long millis = Duration.between(LocalDateTime.now(), nextMealDateTime).toMillis();
        if (millis < 0) millis = 1000;
        realtimeDb.child("users")
                .child(currentUser.getUid())
                .child("nextMealTime")
                .setValue(nextMealDateTime.toString());
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

    private void updateFoodLevelFromRealtimeDb() {
        if (currentUser == null) {
            foodLevelText.setText("Nu ești logat!");
            foodProgressBar.setProgress(0);
            return;
        }

        DatabaseReference foodLevelRef = realtimeDb
                .child("users")
                .child(currentUser.getUid())
                .child("nextMeal")
                .child("foodLevel");  // calea corectă actualizată

        foodLevelRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long foodLevel = snapshot.getValue(Long.class);
                    if (foodLevel != null) {
                        int safeLevel = Math.max(0, Math.min(100, foodLevel.intValue())); // asigură-te că este între 0-100
                        foodLevelText.setText("Nivelul de hrana din dispozitiv este: " + safeLevel + "%");
                        foodProgressBar.setProgress(safeLevel);
                    } else {
                        foodLevelText.setText("Nivelul de hrana din dispozitiv este: N/A");
                        foodProgressBar.setProgress(0);
                    }
                } else {
                    foodLevelText.setText("Nivelul de hrana din dispozitiv este: N/A");
                    foodProgressBar.setProgress(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                foodLevelText.setText("Eroare la citirea nivelului de hrana.");
                foodProgressBar.setProgress(0);
            }
        });
    }




    @Override
    protected void onResume() {
        super.onResume();
        loadPetData();
        loadMealTimes();
        updateFoodLevelFromRealtimeDb();
    }

    @Override
    protected void onDestroy() {
        if (mealCountdownTimer != null) {
            mealCountdownTimer.cancel();
        }
        handler.removeCallbacks(updateImageTask);
        super.onDestroy();
    }
}
