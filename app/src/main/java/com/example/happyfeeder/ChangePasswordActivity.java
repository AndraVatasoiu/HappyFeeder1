package com.example.happyfeeder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText oldPasswordEditText, newPasswordEditText, confirmPasswordEditText;
    private Button saveButton;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DocumentReference userDocRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        oldPasswordEditText = findViewById(R.id.password_old);
        newPasswordEditText = findViewById(R.id.password_new);
        confirmPasswordEditText = findViewById(R.id.password_confirm);
        saveButton = findViewById(R.id.save_button);

        // Inițializare Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            userDocRef = db.collection("users").document(userId);
        }

        // Acțiune pe buton
        saveButton.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String oldPassword = oldPasswordEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Parolele nu se potrivesc!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Toate câmpurile sunt obligatorii!", Toast.LENGTH_SHORT).show();
            return;
        }
        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String savedPasswordHash = documentSnapshot.getString("passwordHash");
                String enteredOldHash = hashPassword(oldPassword);
                if (enteredOldHash.equals(savedPasswordHash)) {
                    String newHashedPassword = hashPassword(newPassword);
                    userDocRef.update("passwordHash", newHashedPassword)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Parola a fost schimbată!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Eroare la salvarea parolei.", Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(this, "Parola actuală este greșită!", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Eroare la accesarea datelor utilizatorului.", Toast.LENGTH_SHORT).show();
        });
    }

    // Hash SHA-256 (folosit doar pentru exemplu)
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
