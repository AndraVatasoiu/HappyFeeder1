package com.example.happyfeeder;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChangeEmailActivity extends AppCompatActivity {

    private EditText emailOld, emailNew;
    private Button saveButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_email);

        emailOld = findViewById(R.id.email_old);
        emailNew = findViewById(R.id.email_new);
        saveButton = findViewById(R.id.save_button);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        saveButton.setOnClickListener(v -> changeEmail());
    }

    private void changeEmail() {
        String oldEmailInput = emailOld.getText().toString().trim();
        String newEmailInput = emailNew.getText().toString().trim();

        if (!isValidEmail(oldEmailInput) || !isValidEmail(newEmailInput)) {
            Toast.makeText(this, "Emailurile trebuie să fie valide și să conțină '@' și '.'", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Utilizatorul nu este autentificat!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Caută documentul din Firestore pe baza emailului vechi introdus
        db.collection("users")
                .whereEqualTo("email", oldEmailInput)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Emailul actual introdus nu există în baza de date!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Emailul există, continuăm cu actualizarea
                    String documentId = queryDocumentSnapshots.getDocuments().get(0).getId();

                    // Încearcă schimbarea în Firebase Auth
                    user.updateEmail(newEmailInput)
                            .addOnSuccessListener(unused -> {
                                db.collection("users").document(documentId)
                                        .update("email", newEmailInput)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Emailul a fost actualizat cu succes!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(this, "Eroare la actualizarea în Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Eroare la actualizarea în Firebase Auth: " + e.getMessage(), Toast.LENGTH_LONG).show());

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Eroare la căutarea emailului: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches() && email.contains("@") && email.contains(".");
    }
}
