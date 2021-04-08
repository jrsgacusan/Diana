package com.example.occupines.adapters;

import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.occupines.LoadingDialog;
import com.example.occupines.R;
import com.example.occupines.Utility;
import com.example.occupines.fragments.FormFragment;
import com.example.occupines.models.Property;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Objects;

public class PropertyAdapter extends RecyclerView.Adapter<PropertyAdapter.ViewHolder> {
    private final ArrayList<Property> listData;

    // RecyclerView recyclerView;
    public PropertyAdapter(ArrayList<Property> listData) {
        this.listData = listData;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.property_template, parent, false);
        return new ViewHolder(listItem, listData);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Property myListData = listData.get(position);

        holder.type.setText(myListData.getType());
        holder.price.setText(String.valueOf(myListData.getPrice()));
        holder.location.setText(myListData.getLocation());
        holder.owner.setText(myListData.getOwner());
        holder.info.setText(myListData.getInfo());
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
        public final Button delete;
        public final Button edit;
        private final AppCompatActivity context;
        private final String userId;
        private final FirebaseFirestore db;
        private final StorageReference storageRef;
        private final LoadingDialog loadingDialog;

        public ViewHolder(View itemView, ArrayList<Property> listData) {
            super(itemView);

            this.context = ((AppCompatActivity) itemView.getContext());
            this.userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            this.db = FirebaseFirestore.getInstance();
            this.storageRef = FirebaseStorage.getInstance().getReference();
            this.loadingDialog = new LoadingDialog(context);

            this.photo = itemView.findViewById(R.id.photoResult);
            this.type = itemView.findViewById(R.id.typeResult);
            this.price = itemView.findViewById(R.id.priceResult);
            this.location = itemView.findViewById(R.id.locationResult);
            this.owner = itemView.findViewById(R.id.ownerResult);
            this.info = itemView.findViewById(R.id.infoResult);

            this.delete = itemView.findViewById(R.id.delete);
            this.edit = itemView.findViewById(R.id.edit);

            delete.setOnClickListener(v -> {
                DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            deleteData();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Delete selected property?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            });

            edit.setOnClickListener(v ->
                    context.getSupportFragmentManager()
                            .beginTransaction()
                            .addToBackStack("PropertyFragment")
                            .replace(R.id.flFragment, FormFragment.newInstance(listData.get(0)))
                            .commit());
            initImageSlider(itemView);
        }

        private void initImageSlider(View itemView) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            ImageSlider imageSlider = itemView.findViewById(R.id.imageSlider_activityDisplaySpecificService);
            ArrayList<SlideModel> remoteImages = new ArrayList();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("/slider-images/"+uid);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot it : snapshot.getChildren()) {
                        remoteImages.add(new SlideModel(it.child("url").getValue().toString(), "", ScaleTypes.CENTER_CROP));
                    }
                    imageSlider.setImageList(remoteImages, ScaleTypes.CENTER_CROP);
                    imageSlider.stopSliding();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
            imageSlider.stopSliding();

        }

        private void deleteData() {
            loadingDialog.start();
            db.collection("properties").document(userId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        StorageReference propertyImageRef = storageRef.child("images").child(userId).child("property");
                        propertyImageRef.delete();
                        //Complete
                        Utility.showToast(itemView.getContext(), "Property deleted.");
                        loadingDialog.dismiss();
                        //Change fragment
                        context.getSupportFragmentManager().popBackStack();
                    });
        }
    }
}
