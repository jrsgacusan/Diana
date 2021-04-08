package com.example.occupines;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.transition.Fade;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.occupines.activities.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import id.zelory.compressor.Compressor;

public class Utility {

    //Compresses the image to a smaller size for faster loading
    public static Uri compressImage(Context context, Uri imagePath) {
        //Getting imageUri and store in file. and compress to using compression library
        File filesDir = context.getFilesDir();
        File imageFile = new File(filesDir, "profile.jpg");

        File compressedImage = null;
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imagePath);
            OutputStream os = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, os);
            os.flush();
            os.close();

            compressedImage = new Compressor(context)
                    .setMaxWidth(250)
                    .setMaxHeight(250)
                    .setQuality(75)
                    .setCompressFormat(Bitmap.CompressFormat.WEBP)
                    .compressToFile(imageFile, "compressedImage");
        } catch (Exception e) {
            Log.e(context.getClass().getSimpleName(), "Error writing bitmap", e);
        }

        return Uri.fromFile(compressedImage);
    }

    //Fixes shared element transition bug
    public static void removeBlinkOnTransition(Activity activity) {
        //Exclude things from transition animation
        Fade fade = new Fade();
        View decor = activity.getWindow().getDecorView();
        fade.excludeTarget(decor.findViewById(R.id.action_bar_container), true);
        fade.excludeTarget(android.R.id.statusBarBackground, true);
        fade.excludeTarget(android.R.id.navigationBarBackground, true);

        activity.getWindow().setEnterTransition(fade);
        activity.getWindow().setExitTransition(fade);
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message,
                Toast.LENGTH_SHORT).show();
    }

    //Disables or enables a button
    public static void toggleButton(Button btn) {
        boolean toggle = btn.isEnabled();
        btn.setEnabled(!toggle);
    }

    //Sign out dialog
    public static void signOut(Activity activity, FirebaseAuth mAuth) {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    //Yes button clicked
                    //Sign out current user
                    mAuth.signOut();
                    //Starts the LoginActivity and clears the activity stack
                    activity.startActivity(new Intent(activity, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    //Closes the MainActivity
                    activity.finish();
                    //Changes animation transition to fade in and fade out
                    activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        };

        //Show dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("Sign out?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

}