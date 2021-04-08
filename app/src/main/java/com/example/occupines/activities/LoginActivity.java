package com.example.occupines.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import com.example.occupines.LoadingDialog;
import com.example.occupines.R;
import com.example.occupines.Utility;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    //Setup global variables
    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private LoadingDialog loadingDialog;

    //onStart method always starts first on activity load
    //Sets content view to activity_login.xml
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //Remove blinking bug on shared element transition
        Utility.removeBlinkOnTransition(LoginActivity.this);

        //Instantiate global variables
        mAuth = FirebaseAuth.getInstance();
        loadingDialog = new LoadingDialog(LoginActivity.this);

        //Setup button events
        buttonEvents();
    }

    //onStart method always starts after onCreate based on android's activity lifecycle
    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    //Method for setting up button events
    private void buttonEvents() {
        //Get button reference
        Button signIn = findViewById(R.id.signIn);
        Button signUp = findViewById(R.id.signUp);

        //Set on click event for signIn button
        signIn.setOnClickListener(v -> {
            //Disable buttons on click to prevent multiple requests
            Utility.toggleButton(signIn);
            Utility.toggleButton(signUp);

            //Get text from EditText and convert to String
            String email = ((EditText) findViewById(R.id.email)).getText().toString();
            String password = ((EditText) findViewById(R.id.password)).getText().toString();

            //Check if fields are empty
            if (!email.isEmpty() && !password.isEmpty()) {
                //Call the signIn method
                signIn(email, password);
            } else {
                Utility.showToast(LoginActivity.this, "Some fields are empty.");
            }

            //Enable buttons
            Utility.toggleButton(signIn);
            Utility.toggleButton(signUp);
        });

        //Set on click event for signUp button
        signUp.setOnClickListener(v -> {
            //Disable buttons on click to prevent multiple requests
            Utility.toggleButton(signIn);
            Utility.toggleButton(signUp);

            //Get icon reference
            ImageView imageView = findViewById(R.id.icon);
            //Set next page to RegisterActivity from this activity
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            //Shared element transition
            ActivityOptionsCompat options = ActivityOptionsCompat.
                    makeSceneTransitionAnimation(LoginActivity.this, imageView, "icon");
            //Start RegisterActivity
            startActivity(intent, options.toBundle());

            //Enable buttons
            Utility.toggleButton(signIn);
            Utility.toggleButton(signUp);
        });
    }

    //MainActivity is loaded if user is logged in
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            //Starts the MainActivity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            //Close the LoginActivity
            finish();
        }
    }

    //Sign in the user through firebase authentication API
    private void signIn(String email, String password) {
        //Start loading animation
        loadingDialog.start();
        //Use String from email and password fields to sign in
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success");
                        Utility.showToast(LoginActivity.this, "Login success.");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        // If sign in fails, display a message to the user.
                        try {
                            throw Objects.requireNonNull(task.getException());
                        } catch (FirebaseAuthInvalidUserException e) {
                            Utility.showToast(LoginActivity.this, "Authentication failed: User not found.");
                        } catch (Exception e) {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Utility.showToast(LoginActivity.this, "Authentication failed.");
                        }
                        updateUI(null);
                    }
                    //Stop loading animation
                    loadingDialog.dismiss();
                });
    }
}