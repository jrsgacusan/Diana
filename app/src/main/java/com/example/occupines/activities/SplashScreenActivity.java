package com.example.occupines.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.occupines.R;

public class SplashScreenActivity extends AppCompatActivity {

    //App starts here
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Sets content view to activity_splash_screen.xml
        setContentView(R.layout.activity_splash_screen);
    }

    //After setting the view
    @Override
    public void onStart() {
        super.onStart();
        //Shows the splash screen for 2500 milliseconds (2.5 seconds)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            //Then starts the LoginActivity and clears the activity stack
            startActivity(new Intent(SplashScreenActivity.this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            //Closes the SplashScreenActivity
            finish();
            //Changes animation transition to fade in and fade out
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }, 2500);
    }
}