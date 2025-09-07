package com.erickoeckel.tasktimer;

public class Task {
    public String id;
    public String title;
    public boolean done;

    public Task() {}
    public Task(String id, String title, boolean done) {
        this.id = id; this.title = title; this.done = done;
    }
}

