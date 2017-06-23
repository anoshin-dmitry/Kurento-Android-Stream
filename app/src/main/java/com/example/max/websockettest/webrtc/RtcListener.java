package com.example.max.websockettest.webrtc;

import org.webrtc.MediaStream;

/**
 * Created by Max on 13-4-2015.
 * Implement this interface to be notified of events.
 */

public interface RtcListener{

    void onSocketCompleted();
    void onSocketFailed();
    void onStreamCreated();
    void onCreateFailed();
    void onViewFailed();
    void onStopCommunication();
    void onAddRemoteStream(MediaStream remoteStream);
    void onRemoveRemoteStream();

}