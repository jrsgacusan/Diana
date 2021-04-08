package com.example.occupines.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.occupines.R;
import com.example.occupines.activities.MainActivity;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class FirstFragment extends Fragment {

    public static ArrayList<String> checked = new ArrayList<>();
    public static String location = "";

    public FirstFragment() {
        // Required empty public constructor
    }

    //onCreateView starts after onCreate
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_first, container, false);
        //Get userImage reference
        ImageView userImage = view.findViewById(R.id.userImage);
        //Set current fragment to ProfileFragment on userImage click
        userImage.setOnClickListener(v -> setCurrentFragment(new ProfileFragment(), userImage));
        //Set userImage source to downloaded image from MainActivity
        setImage(userImage);

        SearchView searchView = view.findViewById(R.id.sv_location);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                location = searchView.getQuery().toString();
                reloadCurrentFragment();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    location = "";
                }
                return false;
            }
        });

        //Get burgerMenu reference
        ImageView burgerMenu = view.findViewById(R.id.burgerMenu);
        //Show About Us on burgerMenu click
        burgerMenu.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            View filterView = inflater.inflate(R.layout.filter_dialog, container, false);

            CheckBox houseCheck = filterView.findViewById(R.id.checkHouse);
            CheckBox apartmentCheck = filterView.findViewById(R.id.checkApartment);
            CheckBox boardingCheck = filterView.findViewById(R.id.checkBoarding);
            CheckBox ascCheck = filterView.findViewById(R.id.checkAscending);
            CheckBox descCheck = filterView.findViewById(R.id.checkDescending);

            if (checked.contains("House")) {
                houseCheck.setChecked(true);
            }
            if (checked.contains("Apartment")) {
                apartmentCheck.setChecked(true);
            }
            if (checked.contains("Boarding")) {
                boardingCheck.setChecked(true);
            }
            if (checked.contains("Ascending")) {
                ascCheck.setChecked(true);
            }
            if (checked.contains("Descending")) {
                descCheck.setChecked(true);
            }

            ArrayList<String> temp = new ArrayList<>(checked);

            houseCheck.setOnClickListener(v1 -> {
                if (houseCheck.isChecked()) {
                    temp.add("House");
                    temp.remove("Apartment");
                    temp.remove("Boarding");
                    apartmentCheck.setChecked(false);
                    boardingCheck.setChecked(false);
                } else {
                    temp.remove("House");
                }
            });

            apartmentCheck.setOnClickListener(v1 -> {
                if (apartmentCheck.isChecked()) {
                    temp.add("Apartment");
                    temp.remove("House");
                    temp.remove("Boarding");
                    houseCheck.setChecked(false);
                    boardingCheck.setChecked(false);
                } else {
                    temp.remove("Apartment");
                }
            });

            boardingCheck.setOnClickListener(v1 -> {
                if (boardingCheck.isChecked()) {
                    temp.add("Boarding");
                    temp.remove("House");
                    temp.remove("Apartment");
                    apartmentCheck.setChecked(false);
                    houseCheck.setChecked(false);
                } else {
                    temp.remove("Boarding");
                }
            });

            ascCheck.setOnClickListener(v1 -> {
                if (ascCheck.isChecked()) {
                    temp.add("Ascending");
                    temp.remove("Descending");
                    descCheck.setChecked(false);
                } else {
                    temp.remove("Ascending");
                }
            });

            descCheck.setOnClickListener(v1 -> {
                if (descCheck.isChecked()) {
                    temp.add("Descending");
                    temp.remove("Ascending");
                    ascCheck.setChecked(false);
                } else {
                    temp.remove("Descending");
                }
            });

            builder.setView(filterView)
                    .setPositiveButton("Filter", (dialog, which) -> {
                        reloadCurrentFragment();
                        checked = temp;
                    })
                    .setNegativeButton("", (dialog, which) -> {
                    })
                    .create()
                    .show();
        });

        return view;
    }

    //onViewCreated starts after onCreateView
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Show list of rental properties
        getChildFragmentManager().beginTransaction().replace(R.id.propertyPosts, new ListFragment()).commitNow();
        //Refresh on swipe down
        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(() -> {
            reloadCurrentFragment();
            refreshLayout.setRefreshing(false);
        });
    }

    //Replaces the current fragment
    private void setCurrentFragment(Fragment fragment, ImageView userImage) {
        assert getFragmentManager() != null;
        getFragmentManager()
                .beginTransaction()
                .addSharedElement(userImage, "profile")
                .addToBackStack("FirstFragment")
                .replace(R.id.flFragment, fragment)
                .commit();
    }

    private void reloadCurrentFragment() {
        assert getFragmentManager() != null;
        getFragmentManager()
                .beginTransaction()
                .detach(this)
                .attach(this)
                .commit();
    }

    //Sets the image of an ImageView
    private void setImage(ImageView userImage) {
        Picasso.get().load(MainActivity.localFile)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .centerCrop()
                .resize(500, 500)
                .into(userImage);
    }

}