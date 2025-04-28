package com.example.happyfeeder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddPetActivity extends AppCompatActivity {

    private ImageView profileImage;
    private EditText editPetName, editPetWeight;
    private Spinner spinnerBreed;
    private Button buttonSavePet;

    private Uri selectedImageUri = null;

    private FirebaseAuth auth;
    private FirebaseFirestore fStore;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);

        profileImage = findViewById(R.id.profileImage);
        editPetName = findViewById(R.id.edit_pet_name);
        editPetWeight = findViewById(R.id.edit_pet_weight);
        spinnerBreed = findViewById(R.id.spinner_breed);
        buttonSavePet = findViewById(R.id.button_save_pet);

        auth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Trebuie să fii logat!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Populate spinner cu exemple de rase
        String[] breeds = {"Persană", "Siameză", "British Shorthair", "Maine Coon", "Altele"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, breeds);
        spinnerBreed.setAdapter(adapter);

        // Selectare poză din galerie
        profileImage.setOnClickListener(v -> openImagePicker());

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        profileImage.setImageURI(selectedImageUri);
                    }
                }
        );

        buttonSavePet.setOnClickListener(v -> savePet());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void savePet() {
        String petName = editPetName.getText().toString().trim();
        String petBreed = spinnerBreed.getSelectedItem().toString();
        String petWeight = editPetWeight.getText().toString().trim();

        if (petName.isEmpty() || petWeight.isEmpty()) {
            Toast.makeText(this, "Completează toate câmpurile!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            uploadImageAndSavePet(petName, petBreed, petWeight);
        } else {
            savePetData(petName, petBreed, petWeight, ""); // fără poză
        }
    }

    private void uploadImageAndSavePet(String name, String breed, String weight) {
        StorageReference storageRef = storage.getReference();
        String filename = "pets/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference petImageRef = storageRef.child(filename);

        petImageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> petImageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            savePetData(name, breed, weight, imageUrl);
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la încărcarea imaginii!", Toast.LENGTH_SHORT).show();
                });
    }

    private void savePetData(String name, String breed, String weight, String imageUrl) {
        String username = getIntent().getStringExtra("username");

        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "Eroare: username lipsă!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> petData = new HashMap<>();
        petData.put("owner_username", username);
        petData.put("name", name);
        petData.put("breed", breed);
        petData.put("weight", weight);  // Aici salvăm greutatea
        petData.put("photoUrl", imageUrl);

        // Salvăm în colecția globală "pets"
        fStore.collection("pets").add(petData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Animal adăugat în baza de date globală!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare salvare în pets/: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Căutăm utilizatorul după username (nu după ID-ul documentului!)
        fStore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String documentId = queryDocumentSnapshots.getDocuments().get(0).getId(); // luam ID-ul documentului găsit

                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("pets", name); // Sau poți salva mai multe dacă vrei

                        // Facem update la documentul corect
                        fStore.collection("users").document(documentId)
                                .update(updateData)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Animal adăugat la utilizator!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Eroare la update user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Utilizatorul nu a fost găsit!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la căutarea utilizatorului: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
