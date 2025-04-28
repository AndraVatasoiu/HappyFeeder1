package com.example.happyfeeder;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String username = getIntent().getStringExtra("username");
        // Delay de 2 secunde pentru a da timp utilizatorului să vadă ecranul principal
        new Handler().postDelayed(() -> {
            // Obține utilizatorul curent din Firebase
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            // Dacă utilizatorul este logat, îl ducem la AddPetActivity
            if (user != null) {
                Intent intent = new Intent(MainActivity.this, AddPetActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);
            } else {
                // Dacă nu este logat, îl ducem la HomeActivity
                startActivity(new Intent(MainActivity.this, HomeActivity.class));
            }

            // Termină MainActivity pentru a nu o mai lăsa în istoric
            finish();
        }, 2000); // Așteaptă 2 secunde
    }
}
