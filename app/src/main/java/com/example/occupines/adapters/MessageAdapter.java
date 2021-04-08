package com.example.occupines.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.occupines.R;
import com.example.occupines.models.Chat;
import com.example.occupines.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_Right = 1;

    private final List<Chat> chats;
    private final User user;

    public MessageAdapter(List<Chat> chats, User user) {
        this.chats = chats;
        this.user = user;
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == MSG_TYPE_Right) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_right, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_left, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        Chat chat = chats.get(position);
        holder.showMessage.setText(chat.getMessage());

        if (holder.getItemViewType() == MSG_TYPE_LEFT) {
            Picasso.get().load(user.getLocalFile())
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .priority(Picasso.Priority.HIGH)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .centerCrop()
                    .resize(500, 500)
                    .into(holder.profileImage);
        }

        if (holder.seen != null) {
            if (position == chats.size() - 1) {
                if (chat.isSeen()) {
                    holder.seen.setText(R.string.seen);
                } else {
                    holder.seen.setText(R.string.delivered);
                }
            } else {
                holder.seen.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    @Override
    public int getItemViewType(int position) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;
        if (chats.get(position).getSender().equals(currentUser.getUid())) {
            return MSG_TYPE_Right;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final ImageView profileImage;
        public final TextView showMessage;
        public final TextView seen;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            showMessage = itemView.findViewById(R.id.showMessage);
            seen = itemView.findViewById(R.id.seen);
        }
    }
}
