package com.example.occupines.models;

import java.time.LocalDate;

public class Event {

    private final LocalDate date;
    private String id;
    private int notificationId;

    private String userId;
    private String text;

    public Event(String id, String userId, String text, LocalDate date) {
        this.id = id;
        this.userId = userId;
        this.text = text;
        this.date = date;
    }

    public Event(int notificationId, String userId, String text, LocalDate date) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.text = text;
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public int getNotificationId() {
        return notificationId;
    }
}
