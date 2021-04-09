package com.example.occupines

import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.example.occupines.fragments.FormFragment
import com.example.occupines.fragments.ProfileFragment
import com.example.occupines.fragments.PropertyFragment
import com.example.occupines.models.Property
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import java.util.*

class PropertyItem(val property: Property, val propertyUid: String): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        var owner = viewHolder.itemView.findViewById<TextView>(R.id.ownerResult)!!
        var type= viewHolder.itemView.findViewById<TextView>(R.id.typeResult)
        var price= viewHolder.itemView.findViewById<TextView>(R.id.priceResult)
        var loc= viewHolder.itemView.findViewById<TextView>(R.id.locationResult)
        var info= viewHolder.itemView.findViewById<TextView>(R.id.infoResult)
        val delete = viewHolder.itemView.findViewById<Button>(R.id.delete)
        val edit = viewHolder.itemView.findViewById<Button>(R.id.edit)
//        val images = viewHolder.itemView.findViewById<ImageSlider>(R.id.imageSlider_activityDisplaySpecificService)
        initImageSlider(viewHolder, propertyUid)

        owner.text = property.owner
        type.text = property.type
        price.text = property.price.toString()
        loc.text = property.location
        info.text = property.info

        delete.setOnClickListener {
            deleteListing(propertyUid, viewHolder)
        }



        edit.setOnClickListener { v: View? ->
            (viewHolder.itemView.context as AppCompatActivity).supportFragmentManager
                    .beginTransaction()
                    .addToBackStack("PropertyFragment")
                    .replace(R.id.flFragment, FormFragment.newInstance(property, propertyUid))
                    .commit()
        }


    }

    private fun deleteListing(propertyUid: String, viewHolder: ViewHolder) {
        val currentUserUid= FirebaseAuth.getInstance().currentUser.uid
        val ref = FirebaseFirestore.getInstance()
        ref.collection("properties").document(currentUserUid).collection("listings").document(propertyUid).delete().addOnCompleteListener {
            Toast.makeText(viewHolder.itemView.context, "Listing deleted", Toast.LENGTH_SHORT).show()
        }
        val dbRef = FirebaseDatabase.getInstance().getReference("slider-images/$propertyUid")
        dbRef.removeValue()


        //go to the next fragment
        setCurrentFragment(PropertyFragment(), viewHolder)

    }

    override fun getLayout(): Int {
        return R.layout.property_template
    }

    private fun initImageSlider(viewHolder: ViewHolder, propertyUid: String) {

        val imageSlider: ImageSlider = viewHolder.itemView.findViewById(R.id.imageSlider_activityDisplaySpecificService)
        val remoteImages: ArrayList<SlideModel> = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("/slider-images/$propertyUid")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (it in snapshot.children) {
                    remoteImages.add(SlideModel(it.child("url").value.toString(), "", ScaleTypes.CENTER_CROP))
                }
                imageSlider.setImageList(remoteImages, ScaleTypes.CENTER_CROP)
                imageSlider.stopSliding()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        imageSlider.stopSliding()
    }

    private fun setCurrentFragment(fragment: Fragment, viewHolder: ViewHolder) {
        (viewHolder.itemView.context as AppCompatActivity).supportFragmentManager
                .beginTransaction()
                .addToBackStack(ProfileFragment.TAG)
                .replace(R.id.flFragment, fragment)
                .commit()
    }


}