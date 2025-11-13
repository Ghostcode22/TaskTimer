package com.erickoeckel.tasktimer;

public class Task {
    private String id;
    private String title;
    private boolean done;
    private String dueDate;

    @com.google.firebase.firestore.ServerTimestamp
    private java.util.Date createdAt;

    public Task() {}

    public Task(String id, String title) {
        this.id = id;
        this.title = title;
        this.done = false;
        this.dueDate = null;
        this.createdAt = null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public java.util.Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.util.Date d) { this.createdAt = d; }
}


