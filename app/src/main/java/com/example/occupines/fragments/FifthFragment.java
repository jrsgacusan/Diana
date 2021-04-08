package com.example.occupines.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.occupines.GetNearbyPlaces;
import com.example.occupines.LoadingDialog;
import com.example.occupines.R;
import com.example.occupines.Utility;
import com.example.occupines.models.Property;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import static com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

public class FifthFragment extends Fragment implements OnMapReadyCallback,
        ConnectionCallbacks,
        OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = "FifthFragment";
    private static final int REQUEST_CODE = 101;

    final int PROXIMITY_RADIUS = 10000;
    GoogleMap map;
    SupportMapFragment mapFragment;
    SearchView searchView;
    Geocoder geocoder;
    double latitude, longitude;

    private FirebaseFirestore db;
    private LoadingDialog loadingDialog;

    private Button showRentals;
    private GoogleApiClient googleApiClient;
    private Marker currentUserLocationMarker;

    public FifthFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        geocoder = new Geocoder(getContext(), Locale.getDefault());
        db = FirebaseFirestore.getInstance();
        loadingDialog = new LoadingDialog(getActivity());
        currentUserLocationMarker = null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fifth, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkUserLocationPermission();
        }

        // Getting reference to the SupportMapFragment
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        // Getting GoogleMap object from the fragment
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        searchView = view.findViewById(R.id.sv_location);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = searchView.getQuery().toString();
                List<Address> geoResults = new ArrayList<>();

                try {
                    geoResults = geocoder.getFromLocationName(location, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (geoResults != null && geoResults.size() > 0) {
                    Address address = geoResults.get(0);
                    LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                    MarkerOptions userMarkerOptions = new MarkerOptions();
                    userMarkerOptions.position(latLng);
                    userMarkerOptions.title(location);
                    userMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    map.addMarker(userMarkerOptions).showInfoWindow();
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
                } else {
                    Utility.showToast(getContext(), "Place not found");
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        showRentals = view.findViewById(R.id.b_show);
        showRentals.setOnClickListener(v -> {
            String apartment = "apartment";
            Object[] transferData = new Object[2];
            GetNearbyPlaces getNearbyPlaces = new GetNearbyPlaces();

            String url = getUrl(latitude, longitude, apartment);
            transferData[0] = map;
            transferData[1] = url;
            getNearbyPlaces.execute(transferData);
            Utility.showToast(getContext(), "Searching for nearby Apartments");

            getDocuments();
        });

        return view;
    }

    private void getDocuments() {
        loadingDialog.start();
        map.clear();

        db.collection("properties")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            if (document.exists()) {
                                String documentId = document.getId();

                                Property propertyPost = new Property(
                                        document.getString("type"),
                                        Objects.requireNonNull(document.getDouble("price")),
                                        document.getString("location"),
                                        document.getString("owner"),
                                        document.getString("info"),
                                        documentId);

                                String location = propertyPost.getLocation();
                                List<Address> geoResults = new ArrayList<>();

                                try {
                                    geoResults = geocoder.getFromLocationName(location, 1);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                if (geoResults.size() > 0) {
                                    Address address = geoResults.get(0);
                                    LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                                    map.addMarker(new MarkerOptions().position(latLng)
                                            .title(propertyPost.getLocation())
                                            .snippet("₱ " + propertyPost.getPrice()));
                                }
                            } else {
                                Log.d(TAG, "No such document");
                            }
                        }
                        //Show whole Baguio City
                        LatLng latLng = new LatLng(16.402148394057043, 120.59555516012183);
                        map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13)); //could be 2 - 21
                        Utility.showToast(getContext(), "Showing nearby rentals");
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                    loadingDialog.dismiss();
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            buildGoogleApiClient();
            map.setMyLocationEnabled(true);
        }

        //get lat long for corners for specified place
        LatLng one = new LatLng(16.3833911236084, 120.57546615600587);
        LatLng two = new LatLng(16.4316396660511, 120.62464714050293);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        //add them to builder
        builder.include(one);
        builder.include(two);

        LatLngBounds bounds = builder.build();

        //get width and height to current display screen
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;

        // 20% padding
        int padding = (int) (width * 0.20);

        //set lat long bounds
        map.setLatLngBoundsForCameraTarget(bounds);

        //move camera to fill the bound to screen
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));

        //set zoom to level to current so that you won't be able to zoom out viz. move outside bounds
        map.setMinZoomPreference(map.getCameraPosition().zoom);

        //Set map view to display a normal view
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //Zoom to marker on click
        map.setOnMarkerClickListener(marker -> {
            map.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 18));
            marker.showInfoWindow();
            return true;
        });

        //Goto current loc
        map.setOnMyLocationButtonClickListener(() -> {
            if (currentUserLocationMarker != null) {
                currentUserLocationMarker.remove();
            }

            //Show current location
            LatLng latLng = new LatLng(latitude, longitude);
            MarkerOptions userMarkerOptions = new MarkerOptions();
            userMarkerOptions.position(latLng);
            userMarkerOptions.title("Current Location");
            userMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            map.addMarker(userMarkerOptions).showInfoWindow();
            map.moveCamera(CameraUpdateFactory.newLatLng(latLng));

            if (googleApiClient != null) {
                LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            }

            return true;
        });

        //Zoom controls
        map.getUiSettings().setZoomControlsEnabled(true);
        //Add bottom padding to give space for button
        map.setPadding(0, searchView.getHeight(), 0, showRentals.getHeight());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (googleApiClient == null) {
                        buildGoogleApiClient();
                    }
                    map.setMyLocationEnabled(true);
                }
            } else {
                Utility.showToast(getContext(), "Permission Denied...");
            }
        }
    }

    private String getUrl(double latitude, double longitude, String nearbyPlace) {
        StringBuilder googlePlaceUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlaceUrl.append("location=").append(latitude).append(",").append(longitude);
        googlePlaceUrl.append("&radius=").append(PROXIMITY_RADIUS);
        googlePlaceUrl.append("&type=").append(nearbyPlace);
        googlePlaceUrl.append("&sensor=true");
        googlePlaceUrl.append("&key=" + R.string.google_api_key);

        Log.d(TAG, "url = " + googlePlaceUrl.toString());

        return googlePlaceUrl.toString();
    }

    public void checkUserLocationPermission() {
        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(Objects.requireNonNull(getContext()))
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1100);
        locationRequest.setFastestInterval(1100);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        if (currentUserLocationMarker != null) {
            currentUserLocationMarker.remove();
        }

        //Show current location
        LatLng latLng = new LatLng(latitude, longitude);
        MarkerOptions userMarkerOptions = new MarkerOptions();
        userMarkerOptions.position(latLng);
        userMarkerOptions.title("Current Location");
        userMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        map.addMarker(userMarkerOptions).showInfoWindow();
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        if (googleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}