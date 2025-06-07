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
import com.google.firebase.firestore.DocumentSnapshot;
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
        String username = getIntent().getStringExtra("username");

        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "Eroare: username lipsă!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificăm dacă userul are deja un pet (adică dacă există câmpul "pets")
        fStore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        Object petsField = userDoc.get("pets");

                        boolean isEditMode = petsField != null && !petsField.toString().isEmpty();

                        // Validare: dacă suntem în adăugare, toate câmpurile trebuie completate
                        if (!isEditMode) {
                            if (petName.isEmpty() || petWeight.isEmpty()) {
                                Toast.makeText(this, "Completează toate câmpurile pentru a adăuga un animal!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        // Dacă suntem în editare și toate câmpurile sunt goale, nu continuăm
                        if (isEditMode && petName.isEmpty() && petWeight.isEmpty() && selectedImageUri == null) {
                            Toast.makeText(this, "Introduceți cel puțin o modificare pentru a edita!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Salvăm sau actualizăm
                        if (selectedImageUri != null) {
                            uploadImageAndSavePet(petName, petBreed, petWeight);
                        } else {
                            savePetData(petName, petBreed, petWeight, ""); // fără poză
                        }

                    } else {
                        Toast.makeText(this, "Utilizatorul nu a fost găsit!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la căutarea utilizatorului: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

        fStore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String documentId = userDoc.getId();

                        Object petsField = userDoc.get("pets");
                        String oldPetName = (petsField != null) ? petsField.toString() : "";

                        boolean nameChanged = !oldPetName.equals(name);

                        if (petsField != null && !oldPetName.isEmpty()) {
                            if (nameChanged) {
                                // Doar actualizare nume in tabela users
                                fStore.collection("users").document(documentId)
                                        .update("pets", name)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(this, "Nume animal actualizat!", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Eroare la actualizarea numelui: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }

                            fStore.collection("pets")
                                    .whereEqualTo("owner_username", username)
                                    .get()
                                    .addOnSuccessListener(petsQuery -> {
                                        if (!petsQuery.isEmpty()) {
                                            DocumentSnapshot petDoc = petsQuery.getDocuments().get(0); // presupunem un singur animal
                                            Map<String, Object> updates = new HashMap<>();

                                            String oldBreed = petDoc.getString("breed");
                                            String oldWeight = petDoc.getString("weight");
                                            String oldName = petDoc.getString("name");

                                            if (breed != null && !breed.isEmpty() && !breed.equals(oldBreed)) {
                                                updates.put("breed", breed);
                                            }
                                            if (weight != null && !weight.isEmpty() && !weight.equals(oldWeight)) {
                                                updates.put("weight", weight);
                                            }
                                            if (name != null && !name.isEmpty() && !name.equals(oldName)) {
                                                updates.put("name", name);
                                            }

                                            if (!updates.isEmpty()) {
                                                fStore.collection("pets").document(petDoc.getId())
                                                        .update(updates)
                                                        .addOnSuccessListener(aVoid -> {
                                                            Toast.makeText(this, "Detalii animal actualizate!", Toast.LENGTH_SHORT).show();
                                                            finish();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(this, "Eroare la actualizarea detaliilor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        });
                                            } else {
                                                Toast.makeText(this, "Nicio modificare detectată.", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            // Dacă nu există, adăugăm animalul nou
                                            Map<String, Object> petData = new HashMap<>();
                                            petData.put("owner_username", username);
                                            petData.put("name", name);
                                            petData.put("breed", breed);
                                            petData.put("weight", weight);

                                            fStore.collection("pets")
                                                    .add(petData)
                                                    .addOnSuccessListener(docRef -> {
                                                        Toast.makeText(this, "Animal adăugat!", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(this, "Eroare la salvare: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Eroare la verificare animale: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });


                        } else {
                            // Dacă pets e null sau gol, adăugăm
                            fStore.collection("users").document(documentId)
                                    .update("pets", name)
                                    .addOnSuccessListener(unused -> {
                                        Map<String, Object> petData = new HashMap<>();
                                        petData.put("owner_username", username);
                                        petData.put("name", name);
                                        petData.put("breed", breed);
                                        petData.put("weight", weight);

                                        fStore.collection("pets")
                                                .add(petData)
                                                .addOnSuccessListener(docRef -> {
                                                    Toast.makeText(this, "Animal adăugat!", Toast.LENGTH_SHORT).show();
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(this, "Eroare la salvare: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Eroare la actualizarea utilizatorului: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(this, "Utilizatorul nu a fost găsit!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la căutarea utilizatorului: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


}
