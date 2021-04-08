package com.example.occupines.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.occupines.R;
import com.example.occupines.activities.ChatActivity;
import com.example.occupines.models.User;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private final List<User> users;

    public UserAdapter(List<User> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
        return new UserAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        Picasso.get().load(user.getLocalFile())
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .priority(Picasso.Priority.HIGH)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .centerCrop()
                .resize(500, 500)
                .into(holder.profileImage);
        holder.username.setText(user.getUsername());
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.context, ChatActivity.class);
            intent.putExtra("id", user.getId());
            holder.context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final ImageView profileImage;
        public final TextView username;

        public final AppCompatActivity context;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            username = itemView.findViewById(R.id.username);
            this.context = (AppCompatActivity) itemView.getContext();
        }
    }
}
