package com.example.occupines.models;

public class ImageSliderModel {

    public String url;
    public String uid;

    public ImageSliderModel() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public ImageSliderModel(String url, String key) {
        this.url = url;
        this.uid = key;
    }
}
