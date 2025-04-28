package com.example.happyfeeder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
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

public class HomeActivity extends AppCompatActivity {

    TextView welcomeText, petNameText, mealTimeText;
    ImageView petImage;
    ProgressBar foodProgressBar;
    Button logoutButton;
    private LinearLayout mealTimesContainer;

    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private CollectionReference usersRef, petsRef, mealsRef;

    private CountDownTimer mealCountdownTimer;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        welcomeText = findViewById(R.id.welcomeText);
        petNameText = findViewById(R.id.petNameText);
        mealTimeText = findViewById(R.id.mealTime);
        petImage = findViewById(R.id.petImage);
        foodProgressBar = findViewById(R.id.foodProgressBar);
        logoutButton = findViewById(R.id.logoutButton);
        mealTimesContainer = findViewById(R.id.mealTimesContainer);

        SharedPreferences prefs = getSharedPreferences("userSession", MODE_PRIVATE);
        String username = prefs.getString("loggedUsername", null);

        if (username != null) {
            welcomeText.setText("Bună, " + username);
        } else {
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
        }

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            db = FirebaseFirestore.getInstance();
            usersRef = db.collection("users");
            petsRef = db.collection("pets");
            mealsRef = db.collection("meals").document(currentUser.getUid()).collection("mealTimes");

            loadUserData(username);
            loadPetData(username);
            loadMealTimes();
        } else {
            Toast.makeText(this, "Nu ești logat!", Toast.LENGTH_SHORT).show();
            finish();
        }

        LinearLayout cardMese = findViewById(R.id.cardMese);
        cardMese.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddMealActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            prefs.edit().remove("loggedUsername").apply();
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void loadUserData(String username) {
        DocumentReference userDoc = usersRef.document(username);

        userDoc.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String usernameFromFirebase = document.getString("username");
                    welcomeText.setText("Bună, " + usernameFromFirebase);
                }
            } else {
                Toast.makeText(HomeActivity.this, "Eroare la obținerea datelor utilizatorului.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPetData(String username) {
        petsRef.whereEqualTo("owner_username", username).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                String petName = document.getString("name");
                                Long foodLevel = document.getLong("foodLevel");

                                if (petName != null) {
                                    petNameText.setText(petName);
                                }
                                if (foodLevel != null) {
                                    foodProgressBar.setProgress(foodLevel.intValue());
                                }

                                String petImageUrl = document.getString("imageUrl");
                                if (petImageUrl != null) {
                                    // Glide.with(HomeActivity.this).load(petImageUrl).into(petImage);
                                }
                            }
                        }
                    } else {
                        petNameText.setText("Nu ai adăugat încă un animal.");
                    }
                });
    }

    private void loadMealTimes() {
        mealsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                mealTimesContainer.removeAllViews();
                List<LocalTime> mealTimes = new ArrayList<>();

                QuerySnapshot snapshot = task.getResult();
                if (snapshot != null) {
                    for (QueryDocumentSnapshot mealSnapshot : snapshot) {
                        Meal meal = mealSnapshot.toObject(Meal.class);
                        if (meal != null) {
                            try {
                                LocalTime time = LocalTime.parse(meal.getTime(), timeFormatter);
                                mealTimes.add(time);
                                addMealTimeCard(meal.getTime());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (mealCountdownTimer != null) {
                        mealCountdownTimer.cancel();
                        mealCountdownTimer = null;
                    }

                    if (!mealTimes.isEmpty()) {
                        Collections.sort(mealTimes);
                        startMealCountdown(mealTimes);
                    } else {
                        mealTimeText.setText("Nicio masă programată.");
                    }
                }
            } else {
                Toast.makeText(HomeActivity.this, "Eroare la încărcarea meselor.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startMealCountdown(List<LocalTime> sortedTimes) {
        LocalTime now = LocalTime.now();
        LocalTime nextMeal = null;

        for (LocalTime time : sortedTimes) {
            if (time.isAfter(now)) {
                nextMeal = time;
                break;
            }
        }

        long millis;
        if (nextMeal == null) {
            nextMeal = sortedTimes.get(0);
            long millisUntilMidnight = Duration.between(now, LocalTime.MIDNIGHT).toMillis();
            long millisFromMidnightToNextMeal = Duration.between(LocalTime.MIDNIGHT, nextMeal).toMillis();
            millis = millisUntilMidnight + millisFromMidnightToNextMeal;
        } else {
            millis = Duration.between(now, nextMeal).toMillis();
        }

        launchCountdown(millis);
    }

    private void launchCountdown(long millis) {
        mealCountdownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long ms) {
                long h = ms / 3600000;
                long m = (ms % 3600000) / 60000;
                long s = (ms % 60000) / 1000;
                mealTimeText.setText(String.format("%02dh %02dm %02ds", h, m, s));
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
}
