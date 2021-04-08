package com.example.occupines.adapters;

import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.occupines.AppDatabase;
import com.example.occupines.R;
import com.example.occupines.RoomDAO;
import com.example.occupines.Utility;
import com.example.occupines.fragments.FourthFragment;
import com.example.occupines.models.Event;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;


public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private static List<Event> events;

    public EventAdapter(List<Event> events) {
        EventAdapter.events = events;
    }

    @NonNull
    @Override
    public EventAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_item_view, parent, false);
        return new EventAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventAdapter.ViewHolder holder, int position) {
        holder.bind(events.get(position));
        holder.setOnClickListener(events.get(position));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView itemEventText;

        public final AppCompatActivity context = (AppCompatActivity) itemView.getContext();

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemEventText = itemView.findViewById(R.id.itemEventText);
        }

        private void deleteEvent(Event event) {
            AppDatabase appDatabase = AppDatabase.getAppDatabase(context);
            RoomDAO roomDAO = appDatabase.getRoomDAO();
            roomDAO.deleteById(event.getNotificationId());

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("events").document(event.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        events.remove(event);
                        FourthFragment.updateAdapterForDate(event.getDate());
                        Utility.showToast(context, "Event deleted");
                    })
                    .addOnFailureListener(e -> {
                        Utility.showToast(context, "Error: Submission failed");
                        Log.w("EventAdapter", "Error writing document", e);
                    });
        }

        public void setOnClickListener(Event event) {
            itemView.setOnClickListener(v -> {
                DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            deleteEvent(event);
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Delete this event?").setPositiveButton("Delete", dialogClickListener)
                        .setNegativeButton("Cancel", dialogClickListener).show();
            });
        }

        public void bind(Event event) {
            itemEventText.setText(event.getText());
        }
    }
}
