package com.example.occupines.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

public class Property implements Parcelable {

    private File localFile;
    private String type;
    private double price;
    private String location;
    private String owner;
    private String info;
    private String id;

    public static final Creator<Property> CREATOR = new Creator<Property>() {
        @Override
        public Property createFromParcel(Parcel in) {
            return new Property(in);
        }

        @Override
        public Property[] newArray(int size) {
            return new Property[size];
        }
    };

    public Property(File localFile, String type, double price, String location, String owner, String info, String id) {
        this.localFile = localFile;
        this.type = type;
        this.price = price;
        this.location = location;
        this.owner = owner;
        this.info = info;
        this.id = id;
    }

    public Property(String type, double price, String location, String owner, String info, String id) {
        this.type = type;
        this.price = price;
        this.location = location;
        this.owner = owner;
        this.info = info;
        this.id = id;
    }

    protected Property(Parcel in) {
        type = in.readString();
        price = in.readDouble();
        location = in.readString();
        info = in.readString();
    }

    public File getLocalFile() {
        return localFile;
    }

    public void setLocalFile(File localFile) {
        this.localFile = localFile;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeDouble(price);
        dest.writeString(location);
        dest.writeString(info);
    }
}
