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
import com.example.occupines.adapters.UserAdapter;
import com.example.occupines.models.Chat;
import com.example.occupines.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ThirdFragment extends Fragment {

    //Setup global variables
    private static final String TAG = "ThirdFragment";
    private static final String COLLECTION = "messages";

    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private LoadingDialog loadingDialog;

    private TextView noMessage;
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> users;
    private Set<String> usersList;

    public ThirdFragment() {
        // Required empty public constructor
    }

    //Instantiate objects
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        loadingDialog = new LoadingDialog(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_third, container, false);
        noMessage = view.findViewById(R.id.noMessage);
        // 1. get a reference to recyclerView
        recyclerView = view.findViewById(R.id.messagesRecyclerView);
        // 2. set layoutManger
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // this is data for recycler view
        users = new ArrayList<>();
        usersList = new LinkedHashSet<>();

        getMessages();

        // 3. create an adapter
        userAdapter = new UserAdapter(users);
        // 4. set adapter
        recyclerView.setAdapter(userAdapter);
        // 5. set item animator to DefaultAnimator
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        return view;
    }

    //Gets chat messages from firestore
    private void getMessages() {
        //Start loading animation
        loadingDialog.start();

        //Get users that you have chat from messages collection from firestore
        db.collection(COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        loadingDialog.dismiss();
                        return;
                    }
                    //Redraw on data change
                    usersList.clear();
                    //Loop through the documents
                    for (QueryDocumentSnapshot document : Objects.requireNonNull(value)) {
                        //Get the chat data of a document
                        if (document.getBoolean("isSeen") != null) {
                            Chat chat = new Chat(
                                    document.getString("sender"),
                                    document.getString("receiver"),
                                    document.getString("message"),
                                    document.getBoolean("isSeen"));

                            //If the receiver of the message is the current user
                            //Then add the message sender to usersList
                            if (chat.getReceiver().equals(currentUser.getUid())) {
                                usersList.add(chat.getSender());
                            }
                            //If the sender of the message is the current user
                            //Then add the message receiver to usersList
                            if (chat.getSender().equals(currentUser.getUid())) {
                                usersList.add(chat.getReceiver());
                            }
                            /*  NOTE: usersList uses a Set interface
                                      Set interface is an unordered collection or
                                      list in which duplicates are not allowed   */
                        }
                    }
                    Log.d(TAG, "usersList: " + usersList.size());
                    //Tell current user if current user has no messages
                    if (usersList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        noMessage.setVisibility(View.VISIBLE);
                        //Stop loading animation
                        loadingDialog.dismiss();
                    } else {
                        //Call getUserInfo method
                        getUsersInfo(usersList);
                    }
                });
    }

    //Gets user info from firestore
    private void getUsersInfo(Set<String> usersList) {
        //Remove all elements of users
        users.clear();

        //Loop through usersList and get id
        for (String id : usersList) {
            //Get user info to show in recycler view
            db.collection("users").document(id).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    //Check if document is not null
                    assert document != null;
                    if (document.exists()) {
                        //Get id of new user
                        String userId = document.getId();
                        //Path to profile picture of user
                        StorageReference profileImageRef = storageRef
                                .child("images")
                                .child(userId)
                                .child("profile");
                        try {
                            File localFile = File.createTempFile(userId, "jpg");
                            Log.d(TAG, Uri.fromFile(localFile).toString());
                            //Get profile picture of new user
                            profileImageRef.getFile(localFile).addOnCompleteListener(task1 -> {
                                //Save new user object
                                User newUser = new User(document.getId(), document.getString("username"), localFile);
                                //Check if new user is already in the list
                                if (users.size() < usersList.size() && !users.contains(newUser)) {
                                    //Add this new user in the recycler view
                                    users.add(newUser);
                                    Log.d(TAG, "users: " + users.size());

                                    if (userAdapter != null) userAdapter.notifyDataSetChanged();
                                }
                            });
                            //Delete temporary file
                            if (localFile.delete()) {
                                Log.d(TAG, "Temp file deleted");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                    //Stop loading animation
                    loadingDialog.dismiss();
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        //Set values to null to prevent memory leak
        userAdapter = null;
        recyclerView.setAdapter(null);
        super.onDestroyView();
    }
}