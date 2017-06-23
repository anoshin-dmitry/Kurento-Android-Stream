package com.example.max.websockettest.webrtc;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.util.Log;
import android.util.Size;

import com.example.max.websockettest.webrtc.commands.AcceptCallCommand;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection.IceConnectionState;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMMediaConfiguration;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMPeerConnection;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMWebRTCPeer;

public class MyWebRTCApp implements NBMWebRTCPeer.Observer {
    private final static String TAG = WebRtcClient.class.getCanonicalName();

    VideoRenderer.Callbacks localRender;

    public NBMWebRTCPeer nbmWebRTCPeer;
    Context mContext;
    WebRtcClient client;

    private String sessionId;

    private boolean backCamera;
    private boolean bLocalMedia = false;
    public boolean isCreator = true;

    public MyWebRTCApp(WebRtcClient client, Context context, VideoRenderer.Callbacks localRender, int width, int height, boolean backCamera)
    {
        mContext = context;
        this.localRender = localRender;
        this.client = client;
        this.backCamera = backCamera;

        Point displaySize = new Point();

        NBMMediaConfiguration.NBMVideoFormat receiverVideoFormat = new NBMMediaConfiguration.NBMVideoFormat(width, height, ImageFormat.JPEG, 30);
        NBMMediaConfiguration mediaConfiguration = new NBMMediaConfiguration(NBMMediaConfiguration.NBMRendererType.OPENGLES, NBMMediaConfiguration.NBMAudioCodec.OPUS,
                    0, NBMMediaConfiguration.NBMVideoCodec.VP8, 0, receiverVideoFormat, NBMMediaConfiguration.NBMCameraPosition.FRONT);

        nbmWebRTCPeer = new NBMWebRTCPeer(mediaConfiguration, mContext, localRender, this);
        nbmWebRTCPeer.addIceServer("stun:77.72.174.163:3478");
        nbmWebRTCPeer.initialize();

        bLocalMedia = false;
    }

    public void setCamera(boolean backCamera) {
        if (!bLocalMedia)
            return;
        if (backCamera)
            nbmWebRTCPeer.selectCameraPosition(NBMMediaConfiguration.NBMCameraPosition.BACK);
        else
            nbmWebRTCPeer.selectCameraPosition(NBMMediaConfiguration.NBMCameraPosition.FRONT);
    }

    void generateOffer(String sessionId, boolean isCreator) {
        this.sessionId = sessionId;
        this.isCreator = isCreator;
        bLocalMedia = false;
        nbmWebRTCPeer.generateOffer(sessionId, isCreator);
    }

    void processAnswer(String answer) {
        SessionDescription sdpAnswer = new SessionDescription(SessionDescription.Type.ANSWER, answer);
        nbmWebRTCPeer.processAnswer(sdpAnswer, sessionId);
    }

    void addRemoteIceCandidate(IceCandidate iceCandidate) {
        nbmWebRTCPeer.addRemoteIceCandidate(iceCandidate, sessionId);
    }

    @Override
    public void onInitialize() {

    }

    /* Observer methods and the rest of declarations */
    public void onLocalSdpOfferGenerated(SessionDescription localSdpOffer, NBMPeerConnection connection) {
        try {
            if (isCreator) {
                JSONObject message = new JSONObject();
                message.put("id", "presenter");
                message.put("session", sessionId);
                message.put("sdpOffer", localSdpOffer.description);
                client.sendMessage(message.toString());
                this.bLocalMedia = true;
                setCamera(this.backCamera);
            } else {
                JSONObject message = new JSONObject();
                message.put("id", "viewer");
                message.put("session", sessionId);
                message.put("sdpOffer", localSdpOffer.description);
                client.sendMessage(message.toString());
            }
        } catch (Exception ex) {

        }
        Log.d(TAG, "onLocalSdpOfferGenerated");
    }
    public void onLocalSdpAnswerGenerated(SessionDescription localSdpAnswer, NBMPeerConnection connection) {
    }
    public void onIceCandidate(IceCandidate localIceCandidate, NBMPeerConnection connection) {
        try {
            Log.d("TAG", "onIceCandidate");
            JSONObject payload = new JSONObject();
            payload.put("sdpMLineIndex", localIceCandidate.sdpMLineIndex);
            payload.put("sdpMid", localIceCandidate.sdpMid);
            payload.put("candidate", localIceCandidate.sdp);

            JSONObject response = new JSONObject();
            response.put("id", "onIceCandidate");
            response.put("candidate",payload);

            client.sendMessage(response.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void onIceStatusChanged(IceConnectionState state, NBMPeerConnection connection) {
        Log.d(TAG, "onIceStatusChanged");
    }
    public void onRemoteStreamAdded(MediaStream stream, NBMPeerConnection connection) {
        Log.d(TAG, "onRemoteStreamAdded");
        client.mListener.onAddRemoteStream(stream);
    }
    public void onRemoteStreamRemoved(MediaStream stream, NBMPeerConnection connection) {
        Log.d(TAG, "onRemoteStreamRemoved");
        client.mListener.onRemoveRemoteStream();
    }
    public void onPeerConnectionError(String error) {
        Log.d(TAG, "onPeerConnectionError");
    }
    public void onDataChannel(DataChannel dataChannel, NBMPeerConnection connection) {  }

    @Override
    public void onBufferedAmountChange(long l, NBMPeerConnection connection, DataChannel channel) {

    }

    @Override
    public void onStateChange(NBMPeerConnection connection, DataChannel channel) {

    }

    @Override
    public void onMessage(DataChannel.Buffer buffer, NBMPeerConnection connection, DataChannel channel) {

    }

    public void onBufferedAmountChange(long l, NBMPeerConnection connection) {  }
    public void onStateChange(NBMPeerConnection connection) {  }
    public void onMessage(DataChannel.Buffer buffer, NBMPeerConnection connection) {  }

}