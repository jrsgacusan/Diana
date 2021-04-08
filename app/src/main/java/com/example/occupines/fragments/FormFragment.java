package com.example.occupines.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.occupines.LoadingDialog;
import com.example.occupines.R;
import com.example.occupines.Utility;
import com.example.occupines.models.ImageSliderModel;
import com.example.occupines.models.Property;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class FormFragment extends Fragment {

    private static final String TAG = "FormFragment";
    private static final int PICK_IMAGE = 123;
    private static final String COLLECTION = "properties";

    private StorageReference storageRef;
    private FirebaseAuth mAuth;
    private LoadingDialog loadingDialog;

    private Uri imagePath;



    private View view;
    private Spinner type;
    private EditText price;
    private EditText location;
    private EditText info;
    private Button submit;

    //Variables added
    private ListView listView;
    private ArrayList<Uri> uriArrayList = new ArrayList();
    private ArrayAdapter<Uri> uriArrayListAdapter;
    private ImageButton imageBtn;

    public FormFragment() {
        // Required empty public constructor
    }

    public static FormFragment newInstance(Property property) {
        FormFragment fragment = new FormFragment();
        Bundle args = new Bundle();
        args.putParcelable("property", property);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        loadingDialog = new LoadingDialog(getActivity());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_form, container, false);
        init();
        initListeners();
        return view;
    }

    private void initListeners() {
        imageBtn.setOnClickListener(v -> {
            pickImageFromGallery();
        });

        submit.setOnClickListener(v -> {
            Log.i(TAG, "Submit button clicked.");
            performSubmit();
        });
    }

    private void performSubmit() {
        String typeString = type.getSelectedItem().toString();
        String priceString = price.getText().toString();
        String locationString = location.getText().toString();
        String infoString = info.getText().toString();
        //Check if fields are empty
        if (!priceString.isEmpty() && !locationString.isEmpty() && !infoString.isEmpty()) {
            //Check if user already picked an image from gallery
            if (!uriArrayList.isEmpty()) {
                //Call submitProperty method
                submitProperty(typeString, Double.parseDouble(priceString), locationString, infoString);
            } else {
                Utility.showToast(getContext(), "Please choose an image.");
            }
        } else {
            Utility.showToast(getContext(), "Some fields are empty.");
        }
    }


    private void init() {
        price = view.findViewById(R.id.price);
        location = view.findViewById(R.id.location);
        info = view.findViewById(R.id.info);
        imageBtn = view.findViewById(R.id.imageButton);
        submit = view.findViewById(R.id.submit);
        type = view.findViewById(R.id.spinner);
        listView = view.findViewById(R.id.listView_imagesToBeUploaded);


        uriArrayListAdapter = new ArrayAdapter(view.getContext(), android.R.layout.simple_list_item_1, uriArrayList);
        listView.setAdapter(uriArrayListAdapter);


        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.types, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        type.setAdapter(adapter);
    }

    private void pickImageFromGallery() {
        Intent profileIntent;
        profileIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        profileIntent.setType("image/*");
        String[] mimeType = {"image/jpeg", "image/png", "image/jpg"};
        profileIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeType);
        //Open gallery to select photo
        startActivityForResult(Intent.createChooser(profileIntent, "Select Image."), PICK_IMAGE);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Check if data given from other fragment is empty
        //It is not empty if FormFragment was called from edit property
        if (getArguments() != null) {
            Property property = getArguments().getParcelable("property");

            switch (property.getType()) {
                case "House":
                    type.setSelection(0);
                    break;
                case "Apartment":
                    type.setSelection(1);
                    break;
                case "Boarding":
                    type.setSelection(2);
                    break;
            }

            price.setText(String.valueOf(property.getPrice()));
            location.setText(property.getLocation());
            info.setText(property.getInfo());

            submit.setText(R.string.save);
            submit.setOnClickListener(v -> updateProperty(
                    type.getSelectedItem().toString(),
                    Double.parseDouble(price.getText().toString()),
                    location.getText().toString(),
                    info.getText().toString()
            ));
        }
    }

    //Takes image from gallery
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE && resultCode == AppCompatActivity.RESULT_OK && data.getData() != null) {
            imagePath = data.getData();
            uriArrayList.add(data.getData());
            uriArrayListAdapter.notifyDataSetChanged();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //Updates property from edit property
    private void updateProperty(String type, double price, String location, String info) {
        loadingDialog.start();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> property = new HashMap<>();
        property.put("type", type);
        property.put("price", price);
        property.put("location", location);
        property.put("info", info);
        property.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION).document(Objects.requireNonNull(mAuth.getUid()))
                .update(property)
                .addOnCompleteListener(task -> loadingDialog.dismiss())
                .addOnSuccessListener(aVoid -> {

                })
                .addOnFailureListener(e -> {
                    Utility.showToast(getContext(), "Error: Submission failed");
                    Log.w(TAG, "Error writing document", e);
                });
    }

    //Submits property to firestore properties collection
    private void submitProperty(String type, double price, String location, String info) {
        loadingDialog.start();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> property = new HashMap<>();
        property.put("type", type);
        property.put("price", price);
        property.put("location", location);
        property.put("owner", Objects.requireNonNull(mAuth.getCurrentUser()).getDisplayName());
        property.put("info", info);
        property.put("createdAt", FieldValue.serverTimestamp());
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("slider-images/"+mAuth.getUid());
        dbRef.removeValue();
        db.collection(COLLECTION).document(Objects.requireNonNull(mAuth.getUid()))
                .set(property)
                .addOnCompleteListener(task -> loadingDialog.dismiss() )
                .addOnSuccessListener(aVoid -> {
                    for (Uri uri : uriArrayList) {
                        String fileName = UUID.randomUUID().toString();
                        StorageReference ref = FirebaseStorage.getInstance().getReference("property/" + fileName);
                        ref.putFile(uri)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {

                                                String key = dbRef.push().getKey();
                                                Log.i(TAG, "URI:" + uri.toString());
                                                ImageSliderModel model = new ImageSliderModel(uri.toString(), key);
                                                dbRef.child(key).setValue(model);

                                            }
                                        });
                                    }
                                });

                    }
                    Toast.makeText(view.getContext(), "Property listed", Toast.LENGTH_LONG).show();
                    assert getFragmentManager() != null;
                    getFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Utility.showToast(getContext(), "Error: Submission failed");
                    Log.w(TAG, "Error writing document", e);
                });
    }

    //Downloads the photo from firebase storage when editing
    private void downloadImage(Property property) {
//        Picasso.get().load(property.getLocalFile())
//                .placeholder(R.drawable.ic_camera)
//                .error(R.drawable.ic_camera)
//                .priority(Picasso.Priority.HIGH)
//                .networkPolicy(NetworkPolicy.OFFLINE)
//                .centerInside()
//                .fit()
//                .into(photo);
    }

    //Uploads the photo chosen from gallery to firebase storage
    private void uploadImage() {
        //images/User id/property.jpg
        StorageReference pathReference = storageRef.child("images").child(Objects.requireNonNull(mAuth.getUid())).child("property");
        UploadTask uploadTask = pathReference.putFile(Utility.compressImage(Objects.requireNonNull(getContext()), imagePath));
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            assert getFragmentManager() != null;
            getFragmentManager().popBackStack();
        });
    }

}