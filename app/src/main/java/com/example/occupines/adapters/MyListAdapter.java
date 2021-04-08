package com.example.occupines.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.occupines.R;
import com.example.occupines.Utility;
import com.example.occupines.activities.ChatActivity;
import com.example.occupines.models.Property;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MyListAdapter extends RecyclerView.Adapter<MyListAdapter.ViewHolder> {
    private static final String TAG = "MyListAdapter";
    private final ArrayList<Property> listData;
    private final String userId;
    private final FirebaseFirestore db;

    // RecyclerView recyclerView;
    public MyListAdapter(ArrayList<Property> listData, String userId) {
        this.listData = listData;
        this.userId = userId;
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.post_template, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Property myListData = listData.get(position);
        Picasso.get().load(myListData.getLocalFile())
                .placeholder(R.drawable.ic_camera)
                .error(R.drawable.ic_camera)
                .priority(Picasso.Priority.HIGH)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .centerInside()
                .fit()
                .into(holder.photo);
        holder.type.setText(myListData.getType());
        holder.price.setText(new DecimalFormat("#,##0.00").format(myListData.getPrice()));
        holder.location.setText(myListData.getLocation());
        holder.owner.setText(myListData.getOwner());
        holder.info.setText(myListData.getInfo());

        setLikeButton(myListData.getId(), holder.likeButton);

        if (myListData.getId().equals(userId)) {
            holder.messageButton.setVisibility(View.GONE);
            holder.likeButton.setVisibility(View.GONE);
        } else {
            holder.messageButton.setOnClickListener(v -> {
                Intent intent = new Intent(holder.context, ChatActivity.class);
                intent.putExtra("id", myListData.getId());
                holder.context.startActivity(intent);
            });
            holder.likeButton.setOnClickListener(v -> {
                if (holder.likeButton.getText().toString().equals("Like")) {
                    holder.likeButton.setText(R.string.liked);
                    likePost(holder.context, myListData.getId(), true);
                } else if (holder.likeButton.getText().toString().equals("Liked")) {
                    holder.likeButton.setText(R.string.like);
                    likePost(holder.context, myListData.getId(), false);
                }
            });
        }
    }

    //Update document in likes collection
    private void likePost(Context context, String propertyId, boolean bool) {
        Map<String, Object> like = new HashMap<>();
        like.put(userId, bool);

        db.collection("likes").document(propertyId)
                .set(like, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Liked post"))
                .addOnFailureListener(e -> {
                    Utility.showToast(context, "Error: Like failed");
                    Log.w(TAG, "Error writing document", e);
                });
    }

    //Update document in likes collection
    private void setLikeButton(String propertyId, TextView likeButton) {
        db.collection("likes").document(propertyId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        assert document != null;
                        if (document.exists()) {
                            if (document.getBoolean(userId) != null) {
                                if (Objects.requireNonNull(document.getBoolean(userId))) {
                                    likeButton.setText(R.string.liked);
                                } else {
                                    likeButton.setText(R.string.like);
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView photo;
        public final TextView type;
        public final TextView price;
        public final TextView location;
        public final TextView owner;
        public final TextView info;

        public final Button messageButton;
        public final Button likeButton;

        public final AppCompatActivity context;

        public ViewHolder(View itemView) {
            super(itemView);
            this.photo = itemView.findViewById(R.id.photoResult);
            this.type = itemView.findViewById(R.id.typeResult);
            this.price = itemView.findViewById(R.id.priceResult);
            this.location = itemView.findViewById(R.id.locationResult);
            this.owner = itemView.findViewById(R.id.ownerResult);
            this.info = itemView.findViewById(R.id.infoResult);
            this.messageButton = itemView.findViewById(R.id.message);
            this.likeButton = itemView.findViewById(R.id.like);
            this.context = (AppCompatActivity) itemView.getContext();
        }
    }
}
