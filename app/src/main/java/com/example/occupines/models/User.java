package com.example.occupines.models;

import java.io.File;

public class User {

    private String id;
    private String username;
    private File localFile;

    public User(String id, String username, File localFile) {
        this.id = id;
        this.username = username;
        this.localFile = localFile;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public File getLocalFile() {
        return localFile;
    }

    public void setLocalFile(File localFile) {
        this.localFile = localFile;
    }
}
