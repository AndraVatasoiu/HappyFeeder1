package com.example.happyfeeder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;



public class SignupActivity extends AppCompatActivity {

    EditText signupUsername, signupEmail, signupPassword, signupConfirmPass;
    TextView loginRedirectText;
    Button signupButton;

    FirebaseAuth auth;
    FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        signupUsername = findViewById(R.id.signup_username);
        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        signupConfirmPass = findViewById(R.id.signup_confirmpass);
        signupButton = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        auth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        signupButton.setOnClickListener(view -> {
            String username = signupUsername.getText().toString().trim();
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString();
            String confirmPassword = signupConfirmPass.getText().toString();

            if (!email.contains("@") || !email.contains(".")) {
                Toast.makeText(this, "Email invalid!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Parolele nu se potrivesc!", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    if (firebaseUser != null) {
                        String userID = firebaseUser.getUid();
                        DocumentReference documentReference = fStore.collection("users").document(userID);
                        Map<String, Object> user = new HashMap<>();
                        user.put("username", username);
                        user.put("email", email);
                        user.put("passwordHash", hashPassword(password));
                        documentReference.set(user)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(SignupActivity.this, "Utilizator salvat în Firestore!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SignupActivity.this, "Eroare la salvare în Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("SignupActivity", "Firestore error: ", e);
                                });
                        SharedPreferences sharedPreferences = getSharedPreferences("user_details", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("username", username);
                        editor.apply();
                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                        intent.putExtra("username", username);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(SignupActivity.this, "Eroare: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        loginRedirectText.setOnClickListener(view -> {
            startActivity(new Intent(SignupActivity.this, HomeActivity.class));
        });
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}

