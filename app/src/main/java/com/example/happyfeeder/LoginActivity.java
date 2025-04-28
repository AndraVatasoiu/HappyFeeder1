package com.example.happyfeeder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginActivity extends AppCompatActivity {

    EditText loginUsername, loginPassword;
    Button loginButton;
    TextView signupRedirectText;

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginUsername = findViewById(R.id.login_username);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectText = findViewById(R.id.signupRedirectText);

        db = FirebaseFirestore.getInstance();

        loginButton.setOnClickListener(view -> {
            if (!validateUsername() || !validatePassword()) return;
            checkUser();
        });

        signupRedirectText.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    public Boolean validateUsername(){
        String val = loginUsername.getText().toString();
        if(val.isEmpty()) {
            loginUsername.setError("Username trebuie să fie completat!");
            return false;
        } else {
            loginUsername.setError(null);
            return true;
        }
    }

    public Boolean validatePassword(){
        String val = loginPassword.getText().toString();
        if(val.isEmpty()) {
            loginPassword.setError("Parola trebuie să fie completată!");
            return false;
        } else {
            loginPassword.setError(null);
            return true;
        }
    }

    public void checkUser(){
        String userUsername = loginUsername.getText().toString().trim();
        String userPassword = loginPassword.getText().toString().trim();

        // Căutăm utilizatorul în colecția 'users' folosind username-ul
        db.collection("users")
                .whereEqualTo("username", userUsername)  // Căutăm utilizatorul după câmpul 'username'
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // Dacă există documente care corespund căutării
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);  // Obținem primul document găsit

                            // Accesăm câmpul 'passwordHash' din Firestore
                            String passwordHashFromDB = document.getString("passwordHash");
                            String hashedInputPassword = hashPassword(userPassword);  // Hash-uim parola introdusă de utilizator

                            // Comparam cele două hash-uri
                            if (passwordHashFromDB != null && passwordHashFromDB.equals(hashedInputPassword)) {
                                // Salvăm sesiunea utilizatorului
                                SharedPreferences prefs = getSharedPreferences("userSession", MODE_PRIVATE);
                                prefs.edit().putString("loggedUsername", userUsername).apply();

                                // Trecem la pagina Home
                                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                loginPassword.setError("Parola este incorectă!");
                                loginPassword.requestFocus();
                            }
                        } else {
                            loginUsername.setError("Username inexistent!");
                            loginUsername.requestFocus();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Eroare la conectare cu baza de date.", Toast.LENGTH_SHORT).show();
                    }
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
