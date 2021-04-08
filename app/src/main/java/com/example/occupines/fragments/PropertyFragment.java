package com.example.occupines.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.occupines.LoadingDialog;
import com.example.occupines.R;
import com.example.occupines.adapters.PropertyAdapter;
import com.example.occupines.models.Property;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class PropertyFragment extends Fragment {

    private static final String TAG = "PropertyFragment";
    private static final String COLLECTION = "properties";

    private String userId;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private LoadingDialog loadingDialog;

    private RecyclerView recyclerView;
    private PropertyAdapter mAdapter;
    private ArrayList<Property> itemsData;

    public PropertyFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        loadingDialog = new LoadingDialog(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_property, container, false);

        // 1. get a reference to recyclerView
        recyclerView = view.findViewById(R.id.recyclerViewProperty);
        // 2. set layoutManger
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        // this is data for recycler view
        itemsData = new ArrayList<>();
        getData();
        // 3. create an adapter
        mAdapter = new PropertyAdapter(itemsData);
        // 4. set adapter
        recyclerView.setAdapter(mAdapter);
        // 5. set item animator to DefaultAnimator
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        return view;
    }

    private void getData() {
        loadingDialog.start();
        itemsData.clear();
        db.collection(COLLECTION).document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        assert document != null;
                        if (document.exists()) {
                            StorageReference propertyImageRef = storageRef
                                    .child("images")
                                    .child(userId)
                                    .child("property");

                            try {
                                File localFile = File.createTempFile(userId, "jpg");
                                propertyImageRef.getFile(localFile).addOnCompleteListener(task1 -> {
                                    Property propertyPost = new Property(
                                            localFile,
                                            document.getString("type"),
                                            Objects.requireNonNull(document.getDouble("price")),
                                            document.getString("location"),
                                            document.getString("owner"),
                                            document.getString("info"),
                                            userId);

                                    itemsData.add(propertyPost);
                                    if (mAdapter != null) mAdapter.notifyDataSetChanged();
                                    loadingDialog.dismiss();
                                });
                                if (localFile.delete()) {
                                    Log.d(TAG, "Temp file deleted");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                });
    }

    @Override
    public void onDestroyView() {
        mAdapter = null;
        recyclerView.setAdapter(null);
        super.onDestroyView();
    }
}