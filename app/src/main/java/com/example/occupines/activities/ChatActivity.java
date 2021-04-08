package com.example.occupines.activities;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.occupines.LoadingDialog;
import com.example.occupines.R;
import com.example.occupines.Utility;
import com.example.occupines.adapters.MessageAdapter;
import com.example.occupines.models.Chat;
import com.example.occupines.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity {

    //Setup global variables
    private static final String TAG = "ChatActivity";
    private static final String COLLECTION = "messages";

    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private LoadingDialog loadingDialog;

    private TextView username;
    private ImageView userImage;

    private MessageAdapter messageAdapter;
    private List<Chat> chats;
    private User user;
    private RecyclerView recyclerView;

    //ChatActivity starts here
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        //Initialize objects
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        loadingDialog = new LoadingDialog(this);

        // 1. get a reference to recyclerView
        recyclerView = findViewById(R.id.chatRecyclerView);
        recyclerView.setHasFixedSize(true);
        // 2. set layoutManger
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        // this is data for recycler view
        chats = new ArrayList<>();
        // 5. set item animator to DefaultAnimator
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        //Get references
        username = findViewById(R.id.username);
        userImage = findViewById(R.id.userImage);

        //Get arguments from previous fragment/activity
        Bundle extras = getIntent().getExtras();
        String userId = "";
        if (extras != null) userId = extras.getString("id");

        //Call getUserInfo method
        getUserInfo(userId);

        //Get sendButton reference
        ImageButton sendButton = findViewById(R.id.sendButton);
        String finalUserId = userId;
        //Set sendButton on click event
        sendButton.setOnClickListener(v -> {
            //Get chatMessage reference
            EditText chatMessage = findViewById(R.id.chatMessage);
            //Get String from chatMessage
            String message = chatMessage.getText().toString().trim();
            //Check if message is empty
            if (!message.isEmpty()) {
                //Call sendMessage method
                sendMessage(currentUser.getUid(), finalUserId, message);
            } else Utility.showToast(this, "Empty message not allowed");
            //Set chatMessage to empty
            chatMessage.setText("");
        });
    }

    private void getUserInfo(String userId) {
        loadingDialog.start();

        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        assert document != null;
                        if (document.exists()) {
                            String documentId = document.getId();
                            StorageReference profileImageRef = storageRef
                                    .child("images")
                                    .child(documentId)
                                    .child("profile");

                            try {
                                File localFile = File.createTempFile(documentId, "jpg");
                                Log.d(TAG, Uri.fromFile(localFile).toString());
                                profileImageRef.getFile(localFile).addOnCompleteListener(task1 -> {
                                    Picasso.get().load(localFile)
                                            .placeholder(R.drawable.ic_user)
                                            .error(R.drawable.ic_user)
                                            .priority(Picasso.Priority.HIGH)
                                            .networkPolicy(NetworkPolicy.OFFLINE)
                                            .centerInside()
                                            .fit()
                                            .into(userImage);

                                    username.setText(document.getString("username"));
                                    user = new User(userId, document.getString("username"), localFile);

                                    readMessages(currentUser.getUid(), userId, user);
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
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }

    //Creates new document in messages collection
    private void sendMessage(String sender, String receiver, String msg) {
        //Map key value pairs
        Map<String, Object> message = new HashMap<>();
        message.put("sender", sender);
        message.put("receiver", receiver);
        message.put("message", msg);
        message.put("isSeen", false);
        message.put("createdAt", FieldValue.serverTimestamp());

        //Upload to firestore
        db.collection("messages").document()
                .set(message)
                .addOnSuccessListener(aVoid -> Utility.showToast(this, "Message sent"))
                .addOnFailureListener(e -> {
                    Utility.showToast(this, "Error: Submission failed");
                    Log.w(TAG, "Error writing document", e);
                });
    }

    //Gets individual messages and turns it into conversation
    private void readMessages(String myId, String userId, User user) {
        db.collection(COLLECTION)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        loadingDialog.dismiss();
                        return;
                    }
                    //Redraw on data change
                    chats.clear();
                    for (QueryDocumentSnapshot document : Objects.requireNonNull(value)) {
                        if (document.getBoolean("isSeen") != null) {
                            Chat chat = new Chat(
                                    document.getString("sender"),
                                    document.getString("receiver"),
                                    document.getString("message"),
                                    document.getBoolean("isSeen"));

                            //Check if the current user is the receiver or sender of the message
                            if (chat.getReceiver().equals(myId) && chat.getSender().equals(userId) ||
                                    chat.getReceiver().equals(userId) && chat.getSender().equals(myId)) {
                                //Show conversation in screen
                                chats.add(chat);
                                // 3. create an adapter
                                messageAdapter = new MessageAdapter(chats, user);
                                // 4. set adapter
                                recyclerView.setAdapter(messageAdapter);
                            }
                            if (chat.getReceiver().equals(myId) && chat.getSender().equals(userId)) {
                                seenMessage(document.getId());
                            }
                        }
                    }
                    loadingDialog.dismiss();
                });
    }

    //Updates document field isSeen to true in messages document
    private void seenMessage(String documentId) {
        db.collection("messages").document(documentId)
                .update("isSeen", true);
    }

    @Override
    protected void onDestroy() {
        //Set values to null to prevent memory leak
        messageAdapter = null;
        recyclerView.setAdapter(null);
        super.onDestroy();
    }
}