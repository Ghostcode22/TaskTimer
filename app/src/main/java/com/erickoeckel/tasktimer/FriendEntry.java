package com.erickoeckel.tasktimer;

public class FriendEntry {
    public String friendUid;
    public com.google.firebase.Timestamp since;

    public FriendEntry() {}
    public FriendEntry(String friendUid, com.google.firebase.Timestamp since) {
        this.friendUid = friendUid; this.since = since;
    }
}
