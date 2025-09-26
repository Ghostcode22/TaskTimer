package com.erickoeckel.tasktimer;

public class FriendRequest {
    public String id;
    public String fromUid;
    public String toUid;
    public String status;
    public com.google.firebase.Timestamp createdAt;
    public com.google.firebase.Timestamp updatedAt;

    public FriendRequest() {}
}
