package com.example.occupines.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.occupines.LoadingDialog;
import com.example.occupines.R;
import com.example.occupines.Utility;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    //Setup global variables
    private static final String TAG = "RegisterActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private LoadingDialog loadingDialog;
    private String name;

    //Sets content view to activity_login.xml
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        //Remove blinking bug on shared element transition
        Utility.removeBlinkOnTransition(RegisterActivity.this);

        //Instantiate global variables
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadingDialog = new LoadingDialog(RegisterActivity.this);

        //Setup button events
        buttonEvents();
    }

    //Method for setting up button events
    private void buttonEvents() {
        //Get button reference
        Button signUp = findViewById(R.id.signUp2);

        //Set on click event for signUp button
        signUp.setOnClickListener(v -> {
            //Disable button on click to prevent multiple requests
            Utility.toggleButton(signUp);

            //Get text from EditText and convert to String
            name = ((EditText) findViewById(R.id.editTextFullName)).getText().toString();
            String email = ((EditText) findViewById(R.id.email)).getText().toString();
            String password = ((EditText) findViewById(R.id.password)).getText().toString();
            String rePassword = ((EditText) findViewById(R.id.rePassword)).getText().toString();

            //Check if fields are empty
            if (!name.isEmpty() && !email.isEmpty() && !password.isEmpty() && !rePassword.isEmpty()) {
                //Check if passwords match
                if (password.equals(rePassword)) {
                    //Call the signUp method
                    signUp(email, password);
                } else {
                    Utility.showToast(RegisterActivity.this, "Password does not match.");
                }
            } else {
                Utility.showToast(RegisterActivity.this, "Some fields are empty.");
            }

            //Enable button
            Utility.toggleButton(signUp);
        });
    }

    //User is logged in if registration is success
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            //Calls the addUser method
            addUser(user, name);
            //Starts the LoginActivity and clears the activity stack
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            //Close the RegisterActivity
            finish();
            //Changes animation transition to fade in and fade out
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    //Add new registered user
    private void addUser(FirebaseUser firebaseUser, String fullName) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName).build();

        //Updates account username to full name that was entered
        firebaseUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile updated.");
                    }
                });

        //Adds new user to users collection in firestore
        HashMap<String, Object> user = new HashMap<>();
        user.put("username", fullName);
        user.put("imageUrl", "default");
        db.collection("users").document(firebaseUser.getUid()).set(user);
    }

    //Register the user through firebase authentication API
    private void signUp(String email, String password) {
        //Start loading animation
        loadingDialog.start();
        //Use String from email and password fields to sign in
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign up success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success");
                        Utility.showToast(RegisterActivity.this, "You are now signed in.");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        // If sign up fails, display a message to the user.
                        try {
                            throw Objects.requireNonNull(task.getException());
                        } catch (FirebaseAuthWeakPasswordException e) {
                            Utility.showToast(RegisterActivity.this, "Authentication failed: Weak Password.");
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            Utility.showToast(RegisterActivity.this, "Authentication failed: Invalid Email.");
                        } catch (FirebaseAuthUserCollisionException e) {
                            Utility.showToast(RegisterActivity.this, "Authentication failed: User already exists.");
                        } catch (Exception e) {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Utility.showToast(RegisterActivity.this, "Authentication failed.");
                        }
                        updateUI(null);
                    }
                    //Stop loading animation
                    loadingDialog.dismiss();
                });
    }
}