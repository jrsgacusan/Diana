package com.example.occupines.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.example.occupines.LoadingDialog;
import com.example.occupines.PropertyItem;
import com.example.occupines.R;
import com.example.occupines.adapters.PropertyAdapter;
import com.example.occupines.models.Property;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.ViewHolder;

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
    private GroupAdapter<ViewHolder> adapter = new GroupAdapter<ViewHolder>();
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

        View view = inflater.inflate(R.layout.fragment_property, container, false);


        recyclerView = view.findViewById(R.id.recyclerViewProperty);
        recyclerView.addItemDecoration(new DividerItemDecoration(view.getContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);
        fetchProperties();


        return view;
    }

    private void fetchProperties() {
        adapter.clear();
        String currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Task<QuerySnapshot> propertyRef = db.collection("properties").document(currentUserUid).collection("listings").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            for (QueryDocumentSnapshot listing: task.getResult()){
                                Log.i(TAG,listing.getId() + String.valueOf(listing.getData()));
                                Property fetchedListing = new Property(
                                        listing.getString("type"),
                                        listing.getDouble("price"),
                                        listing.getString("location"),
                                        listing.getString("owner"),
                                        listing.getString("info"),
                                        listing.getId()
                                );

                                adapter.add(new PropertyItem(fetchedListing, listing.getId()));
                            }
                        }
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