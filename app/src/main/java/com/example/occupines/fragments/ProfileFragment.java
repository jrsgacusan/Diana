package com.example.occupines.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.occupines.LoadingDialog;
import com.example.occupines.R;
import com.example.occupines.Utility;
import com.example.occupines.activities.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class ProfileFragment extends Fragment {

    //Setup global variables
    private static final String TAG = "ProfileFragment";
    private static final String COLLECTION = "properties";
    private static final int PICK_IMAGE = 123;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private LoadingDialog loadingDialog;

    private Uri imagePath;
    private ImageView userImage;
    private TextView name;

    public ProfileFragment() {
        // Required empty public constructor
    }

    //Instantiate objects
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        loadingDialog = new LoadingDialog(getActivity());
        //Set shared element transition
        setSharedElementEnterTransition(TransitionInflater.from(getContext()).inflateTransition(android.R.transition.move));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userImage = view.findViewById(R.id.userImage);
        setImage(userImage);

        ImageButton changePicture = view.findViewById(R.id.changePicture);
        name = view.findViewById(R.id.fullName);
        ImageButton editName = view.findViewById(R.id.editName);
        TextView email = view.findViewById(R.id.textEmail);

        Button viewProperty = view.findViewById(R.id.viewProperty);
        Button listProperty = view.findViewById(R.id.listProperty);
        Button signOut = view.findViewById(R.id.signOut);

        name.setText(Objects.requireNonNull(currentUser.getDisplayName()));
        email.setText(Objects.requireNonNull(currentUser.getEmail()));

        changePicture.setOnClickListener(v -> {
            Intent profileIntent = new Intent();
            profileIntent.setType("image/*");
            profileIntent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(profileIntent, "Select Image."), PICK_IMAGE);
        });

        editName.setOnClickListener(v -> changeName());

        viewProperty.setOnClickListener(v -> db.collection(COLLECTION).document(currentUser.getUid())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        assert document != null;
                        if (document.exists()) {
                            Log.d(TAG, "Document exists!");
                            setCurrentFragment(new PropertyFragment());
                        } else {
                            Log.d(TAG, "Document does not exist!");
                            Utility.showToast(getContext(), "You have no property listed");
                        }
                    } else {
                        Log.d(TAG, "Failed with: ", task.getException());
                    }
                }));

        listProperty.setOnClickListener(v -> db.collection(COLLECTION).document(currentUser.getUid())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        assert document != null;
                        if (document.exists()) {
                            Log.d(TAG, "Document exists!");
                            Utility.showToast(getContext(), "You can only submit 1 property");
                        } else {
                            Log.d(TAG, "Document does not exist!");
                            setCurrentFragment(new FormFragment());
                        }
                    } else {
                        Log.d(TAG, "Failed with: ", task.getException());
                    }
                }));

        //Show signOut dialog on signOut click
        signOut.setOnClickListener(v -> Utility.signOut(getActivity(), mAuth));
    }

    //Changes current username
    private void changeName() {
        Activity activity = (Activity) getContext();
        final EditText input = new EditText(activity);
        input.setText(name.getText().toString());
        input.setSelectAllOnFocus(true);

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    //Yes button clicked
                    if (!input.getText().toString().trim().isEmpty())
                        updateName(currentUser, input.getText().toString());
                    else
                        Utility.showToast(getContext(), "Field is empty");
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        };

        assert activity != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(input);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        builder.setMessage("Change name").setPositiveButton("Save", dialogClickListener)
                .setNegativeButton("Cancel", dialogClickListener).show();
    }

    private void updateName(FirebaseUser user, String fullName) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName).build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateProperty(fullName);
                        Log.d(TAG, "User profile updated.");
                        reloadCurrentFragment();
                    }
                });
    }

    private void updateProperty(String username) {
        loadingDialog.start();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> name = new HashMap<>();
        name.put("owner", username);

        db.collection("properties").document(currentUser.getUid()).update(name);

        db.collection("users").document(currentUser.getUid())
                .update("username", username)
                .addOnCompleteListener(task -> loadingDialog.dismiss())
                .addOnSuccessListener(aVoid -> Utility.showToast(getContext(), "User name updated"))
                .addOnFailureListener(e -> {
                    Utility.showToast(getContext(), "Error: Submission failed");
                    Log.w(TAG, "Error writing document", e);
                });
    }

    private void reloadCurrentFragment() {
        assert getFragmentManager() != null;
        getFragmentManager()
                .beginTransaction()
                .detach(this)
                .attach(this)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE && resultCode == AppCompatActivity.RESULT_OK && data.getData() != null) {
            imagePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(Objects.requireNonNull(getActivity()).getContentResolver(), imagePath);
                userImage.setImageBitmap(bitmap);
                uploadImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setCurrentFragment(Fragment fragment) {
        assert getFragmentManager() != null;
        getFragmentManager()
                .beginTransaction()
                .addToBackStack(TAG)
                .replace(R.id.flFragment, fragment)
                .commit();
    }

    private void setImage(ImageView userImage) {
        Picasso.get().load(MainActivity.localFile)
                .noPlaceholder()
                .error(R.drawable.ic_user)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .centerCrop()
                .resize(500, 500)
                .into(userImage);
    }

    private void updateImageUrl(FirebaseUser user, String url) {
        db.collection("users").document(user.getUid()).update("imageUrl", url);
    }

    private void uploadImage() {
        //images/User id/profile.jpg
        StorageReference pathReference = storageRef.child("images").child(currentUser.getUid()).child("profile");
        //Compress image then upload
        UploadTask uploadTask = pathReference.putFile(Utility.compressImage(Objects.requireNonNull(getContext()), imagePath));
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            pathReference.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        // Got the download URL for 'images/User id/profile.jpg'
                        updateImageUrl(currentUser, uri.toString());
                        Log.d(TAG, uri.toString());
                    }).addOnFailureListener(exception -> {
                // Handle any errors
            });
            Utility.showToast(getContext(), "Profile picture uploaded");
            //Restart activity to reload new uploaded image
            Intent intent = Objects.requireNonNull(getActivity()).getIntent();
            getActivity().finish();
            startActivity(intent);
        }).addOnFailureListener(e -> Utility.showToast(getContext(), "Error: Uploading profile picture"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }
}