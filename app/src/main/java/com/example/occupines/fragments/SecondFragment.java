package com.example.occupines.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.occupines.LoadingDialog;
import com.example.occupines.R;
import com.example.occupines.adapters.MyListAdapter;
import com.example.occupines.models.Property;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class SecondFragment extends Fragment {

    private static final String TAG = "SecondFragment";

    private FirebaseUser user;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private LoadingDialog loadingDialog;

    private TextView noLikes;
    private RecyclerView recyclerView;
    private MyListAdapter mAdapter;
    private ArrayList<String> itemsData;
    private ArrayList<Property> properties;

    public SecondFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        loadingDialog = new LoadingDialog(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_second, container, false);
        noLikes = view.findViewById(R.id.noLikes);
        // 1. get a reference to recyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        // 2. set layoutManger
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        // this is data for recycler view
        itemsData = new ArrayList<>();
        properties = new ArrayList<>();
        getDocuments();
        // 3. create an adapter
        mAdapter = new MyListAdapter(properties, user.getUid());
        // 4. set adapter
        recyclerView.setAdapter(mAdapter);
        // 5. set item animator to DefaultAnimator
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        return view;
    }

    private void getDocuments() {
        loadingDialog.start();
        itemsData.clear();
        db.collection("likes")
                .whereEqualTo(user.getUid(), true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            if (document.exists()) {
                                String documentId = document.getId();
                                itemsData.add(documentId);
                            } else {
                                Log.d(TAG, "No such document");
                            }
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                    if (itemsData.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        noLikes.setVisibility(View.VISIBLE);
                        loadingDialog.dismiss();
                    } else {
                        getPropertiesInfo(itemsData);
                    }
                });
    }

    private void getPropertiesInfo(ArrayList<String> itemsData) {
        properties.clear();

        for (String id : itemsData) {
            db.collection("properties").document(id).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = Objects.requireNonNull(task.getResult());
                    if (document.exists()) {
                        String documentId = document.getId();
                        StorageReference propertyImageRef = storageRef
                                .child("images")
                                .child(documentId)
                                .child("property");

                        try {
                            File localFile = File.createTempFile(documentId, "jpg");
                            Log.d(TAG, Uri.fromFile(localFile).toString());
                            propertyImageRef.getFile(localFile).addOnCompleteListener(task1 -> {
                                Property propertyPost = new Property(
                                        localFile,
                                        document.getString("type"),
                                        Objects.requireNonNull(document.getDouble("price")),
                                        document.getString("location"),
                                        document.getString("owner"),
                                        document.getString("info"),
                                        documentId);

                                properties.add(propertyPost);
                                if (mAdapter != null) mAdapter.notifyDataSetChanged();
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
                    loadingDialog.dismiss();

                } else {
                    Log.w(TAG, "Error getting documents.", task.getException());
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        mAdapter = null;
        recyclerView.setAdapter(null);
        super.onDestroyView();
    }
}