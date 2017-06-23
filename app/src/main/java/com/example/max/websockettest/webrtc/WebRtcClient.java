package com.example.max.websockettest.webrtc;

import android.content.Context;
import android.hardware.Camera;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.max.websockettest.R;
import com.example.max.websockettest.webrtc.commands.RejectCallCommand;
import com.example.max.websockettest.webrtc.commands.RtcCommandBase;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by Max on 10-4-2015.
 */
public class WebRtcClient implements AsyncHttpClient.WebSocketConnectCallback, WebSocket.StringCallback {
    private final static String TAG = WebRtcClient.class.getCanonicalName();

    private static final int CALLSTATE_NONE = 0;
    private static final int CALLSTATE_STREAM = 1;

    private int callState = CALLSTATE_NONE;

    public RtcListener mListener;
    private WebSocket socket;

    public String sessionId;

    public MyWebRTCApp webRTCApp;

    Context mContext;

    VideoRenderer.Callbacks localRender;
    int videoWidth;
    int videoHeight;

    boolean backCamera = false;

    public WebRtcClient(Context context, RtcListener listener, String host, VideoRenderer.Callbacks localRender, int width, int height) {

        mContext = context;

        mListener = listener;

        this.localRender = localRender;
        this.videoWidth = width;
        this.videoHeight = height;

        AsyncHttpClient client = AsyncHttpClient.getDefaultInstance();

        TrustManager[] byPassTrustManagers = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
        } };

        SSLContext sslContext=null;
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sslContext.init(null, byPassTrustManagers, new SecureRandom());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        client.getSSLSocketMiddleware().setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        client.getSSLSocketMiddleware().setSSLContext(getSslContext());
        client.getSSLSocketMiddleware().setTrustManagers(byPassTrustManagers);

        client.websocket(host, null, this);

    }

    @Override
    public void onCompleted(Exception ex, WebSocket webSocket) {
        socket = webSocket;
        if (socket != null) {
            socket.setStringCallback(this);
            mListener.onSocketCompleted();
        } else {
            mListener.onSocketFailed();
        }
    }

    @Override
    public void onStringAvailable(final String s) {
        System.out.println("I got a string: " + s);
        JSONObject parsedMessage = null;
        try {
            parsedMessage = new JSONObject(s);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (parsedMessage != null) {
            String messageId = "";
            try {
                messageId = parsedMessage.getString("id");
                if(parsedMessage.has("from")) {
                    String from = parsedMessage.getString("from");

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            switch (messageId) {
                case "presenterResponse":
                    presenterResponse(parsedMessage);
                    break;
                case "viewerResponse":
                    viewerResponse(parsedMessage);
                    break;
                case "stopCommunication":
                    stopCommunication();
                    break;
                case "iceCandidate":
                    iceCandidate(parsedMessage);
                    break;
                default:
                    Log.i(TAG, "JSON Request not handled");
            }
        } else {
            Log.i(TAG, "Invalid JSON");
        }
    }

    private void setCallState(int newState) {
        callState = newState;
    }

    /*
    * Send Command
    */

    public void sendMessage(RtcCommandBase message) {
        try {
            sendMessage(message.compile());
        } catch(JSONException jse) {
            Log.e(TAG, jse.getMessage());
        }
    }

    public void sendMessage(String message) {
        if(socket != null && socket.isOpen()) {
            socket.send(message);
        }
    }

    boolean isSocketOpen() {
        if (socket == null || !socket.isOpen())
            return false;
        return  true;
    }

    public void createStream(String sessionId) {
        if (!isSocketOpen())
            return;
        if (callState != CALLSTATE_NONE)
            return;

        webRTCApp = new MyWebRTCApp(this, mContext, localRender, videoWidth, videoHeight, backCamera);
        callState = CALLSTATE_STREAM;
        this.sessionId = sessionId;
        webRTCApp.isCreator = true;
        webRTCApp.generateOffer(sessionId, true);
    }

    public void viewStream(String sessionId) {
        if (!isSocketOpen())
            return;
        if (callState != CALLSTATE_NONE)
            return;

        webRTCApp = new MyWebRTCApp(this, mContext, localRender, videoWidth, videoHeight, backCamera);
        callState = CALLSTATE_STREAM;
        this.sessionId = sessionId;
        webRTCApp.generateOffer(sessionId, false);
    }

    public void stopStream() {
        if (callState == CALLSTATE_NONE)
            return;

        try {
            JSONObject message = new JSONObject();
            message.put("id", "stop");
            sendMessage(message.toString());
        } catch (Exception ex) {

        }
        stopCommunication();
    }
    /*
     * JSON RPC Delegate
     */
    private void presenterResponse(JSONObject message) {
        try {
            String response = message.getString("response");
            if (response.equalsIgnoreCase("accepted")) {
                webRTCApp.processAnswer(message.getString("sdpAnswer"));
                mListener.onStreamCreated();
            } else {
                mListener.onCreateFailed();
                setCallState(CALLSTATE_NONE);
            }
        } catch (JSONException e) {
            setCallState(CALLSTATE_NONE);
            e.printStackTrace();
        }
    }

    private void viewerResponse(JSONObject message) {

        try {
            String response = message.getString("response");
            if (response.equalsIgnoreCase("accepted")) {
                webRTCApp.processAnswer(message.getString("sdpAnswer"));
            } else {

                setCallState(CALLSTATE_NONE);
            }
        } catch (JSONException e) {
            mListener.onViewFailed();
            setCallState(CALLSTATE_NONE);
            e.printStackTrace();
        }
    }

    private void iceCandidate(JSONObject message) {
        if (message != null) {
            try {
                JSONObject data = message.getJSONObject("candidate");

                String sdpMid = data.getString("sdpMid");
                int sdpMLineIndex = data.getInt("sdpMLineIndex");
                String sdp = data.getString("candidate");

                IceCandidate candidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
                webRTCApp.addRemoteIceCandidate(candidate);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopCommunication() {
        setCallState(CALLSTATE_NONE);
        mListener.onStopCommunication();
    }

    /**
     * Get SSL Context
     */
    public SSLContext getSslContext() {

        TrustManager[] byPassTrustManagers = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
        } };

        SSLContext sslContext=null;

        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sslContext.init(null, byPassTrustManagers, new SecureRandom());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        return sslContext;
    }

    public void switchCamera() {
        if (backCamera) backCamera = false;
        else backCamera = true;

        if (webRTCApp != null)
            webRTCApp.setCamera(backCamera);
    }
}
