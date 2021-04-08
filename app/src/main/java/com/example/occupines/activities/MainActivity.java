package com.example.occupines.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;

import com.example.occupines.R;
import com.example.occupines.Utility;
import com.example.occupines.databinding.ActivityMainBinding;
import com.example.occupines.fragments.FifthFragment;
import com.example.occupines.fragments.FirstFragment;
import com.example.occupines.fragments.FourthFragment;
import com.example.occupines.fragments.SecondFragment;
import com.example.occupines.fragments.ThirdFragment;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    //Setup global variables
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 101;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private ActivityMainBinding binding;

    public static File localFile;
    private BottomNavigationView bottomNav;

    //MainActivity starts here
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        //Sets data binding for bottomNavigationView
        setContentView(binding.getRoot());

        //Connect to Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        //Initialize fragments
        FirstFragment firstFragment = new FirstFragment();
        SecondFragment secondFragment = new SecondFragment();
        ThirdFragment thirdFragment = new ThirdFragment();
        FourthFragment fourthFragment = new FourthFragment();
        FifthFragment fifthFragment = new FifthFragment();

        //Get bottomNav reference
        bottomNav = binding.bottomNavigationView;
        //Add badge on notification for messages
        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.messages);
        //Number of notifications
        MutableLiveData<Integer> number = new MutableLiveData<>();
        //Initialize with a value
        getNotificationCount(number);
        //Listener for variable
        number.observe(MainActivity.this, integer ->
                setupBadge(badge, Objects.requireNonNull(number.getValue())));

        //Show each fragment on each menu item click
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            //Clear fragment stack
            getSupportFragmentManager().popBackStackImmediate();
            //Set current fragment on bottomNav item click
            if (itemId == R.id.home) {
                //1st page
                setCurrentFragment(firstFragment);
                return true;
            } else if (itemId == R.id.likes) {
                //2nd page
                setCurrentFragment(secondFragment);
                return true;
            } else if (itemId == R.id.messages) {
                //3rd page
                setCurrentFragment(thirdFragment);
                //Removes badge on click
                destroyBadge(badge);
                return true;
            } else if (itemId == R.id.calendar) {
                //4th page
                setCurrentFragment(fourthFragment);
                return true;
            } else if (itemId == R.id.location) {
                //5th page
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
                } else {
                    setCurrentFragment(fifthFragment);
                }
                return true;
            }
            return false;
        });

        //Download profile image then load first fragment
        downloadImage().addOnCompleteListener(v -> setCurrentFragment(firstFragment));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setCurrentFragment(new FifthFragment());
            } else {
                bottomNav.setSelectedItemId(R.id.home);
            }
        }
    }

    //Gets number of unopened chat
    public void getNotificationCount(MutableLiveData<Integer> number) {
        db.collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }
                    //Redraw on data change
                    int count = 0;
                    //Loop through the documents
                    for (QueryDocumentSnapshot document : Objects.requireNonNull(value)) {
                        //Get the chat data of a document
                        String receiver = document.getString("receiver");
                        if (document.getBoolean("isSeen") != null) {
                            boolean isSeen = Objects.requireNonNull(document.getBoolean("isSeen"));
                            //If the receiver of the message is the current user and the message is not read yet
                            //Then increment notification count
                            assert receiver != null;
                            if (receiver.equals(currentUser.getUid()) && !isSeen) {
                                count++;
                            }
                        }
                    }
                    Log.d(TAG, "Notification count: " + count);
                    number.postValue(count);
                });
    }

    //Sets up number of badge
    private void setupBadge(BadgeDrawable badge, int number) {
        destroyBadge(badge);
        if (number != 0) {
            // An icon only badge will be displayed unless a number is set:
            badge.setBackgroundColor(getResources().getColor(R.color.badge_color));
            badge.setBadgeTextColor(getResources().getColor(R.color.white));
            badge.setBadgeGravity(BadgeDrawable.TOP_END);
            badge.setMaxCharacterCount(3);
            badge.setNumber(number);
            badge.setVisible(true);
        } else {
            destroyBadge(badge);
        }
    }

    //Sets number to zero and hides badge
    private void destroyBadge(BadgeDrawable badge) {
        badge.clearNumber();
        badge.setVisible(false);
    }

    //Download image from firebase storage if user uploaded a profile image
    private FileDownloadTask downloadImage() {
        //Instantiate FirebaseStorage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference pathReference = storageRef.child("images").child(Objects.requireNonNull(mAuth.getUid())).child("profile");
        try {
            //Create temporary file for image data
            localFile = File.createTempFile("profile", "jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Download the image data and put it in the temporary file
        return pathReference.getFile(localFile);
    }

    //Sets the current fragment
    private void setCurrentFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        //Clear fragment stack
        fm.popBackStack(fragment.getTag(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        //Replace current fragment
        fm.beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        //If back button is pressed and fragment stack is not clear
        //Then go back 1 fragment
        //Else ask if going to sign out
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
        } else {
            Utility.signOut(this, mAuth);
        }
    }

    @Override
    protected void onDestroy() {
        //Set values to null to prevent memory leak
        binding = null;
        super.onDestroy();
    }
}