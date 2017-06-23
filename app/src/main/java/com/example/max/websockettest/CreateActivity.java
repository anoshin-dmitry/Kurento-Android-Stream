package com.example.max.websockettest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.max.websockettest.webrtc.RtcListener;
import com.example.max.websockettest.webrtc.WebRtcClient;
import com.example.max.websockettest.webrtc.commands.RegisterNameCommand;
import com.example.max.websockettest.webrtc.commands.RejectCallCommand;

import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.Random;
import java.util.logging.Handler;


public class CreateActivity extends Activity implements RtcListener, View.OnClickListener {

    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;

    private WebRtcClient rtcClient;

    private static final int LOCAL_X = 0;
    private static final int LOCAL_Y = 0;
    private static final int LOCAL_WIDTH = 100;
    private static final int LOCAL_HEIGHT = 100;

    private static final int CAMERA_WIDTH = 640;
    private static final int CAMERA_HEIGHT = 480;

    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;

    public static final int SOCKET_OPEN_SUCCESS = 1;
    public static final int SOCKET_OPEN_FAIL = 2;
    public static final int SOCKET_NOT_READY = 3;

    private static int SOCKET_STATE = 0;

    TextView sessionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        requestPermissionForCameraAndMicrophone();

        SOCKET_STATE = SOCKET_NOT_READY;

        vsv = (GLSurfaceView) findViewById(R.id.glview_call);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {
                init();
            }
        });

        // local and remote render
        localRender = VideoRendererGui.create(LOCAL_X, LOCAL_Y, LOCAL_WIDTH, LOCAL_HEIGHT, scalingType, true);

        findViewById(R.id.btn_start_stream).setOnClickListener(this);
        findViewById(R.id.btn_cancel_stream).setOnClickListener(this);
        sessionView = (TextView) findViewById(R.id.tv_session_id);

    }

    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        rtcClient = new WebRtcClient(CreateActivity.this, CreateActivity.this, "wss://kms.searchandmap.com:8443/cast", localRender, CAMERA_WIDTH, CAMERA_HEIGHT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_switch:
                rtcClient.switchCamera();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.btn_start_stream:
                startStream();
                break;
            case R.id.btn_cancel_stream:
                stopStream();
                break;
            default:
                Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT).show();
        }
    }

    public void startStream() {
        if (SOCKET_STATE != SOCKET_OPEN_SUCCESS) {
            Toast.makeText(this, "Socket is not opened", Toast.LENGTH_SHORT).show();
            return;
        }
        vsv.onResume();
        vsv.setVisibility(View.VISIBLE);
        long second = System.currentTimeMillis();
        String sessionId = String.valueOf(second);
        sessionView.setText("SESSION ID : " + sessionId);
        rtcClient.createStream(sessionId);

    }

    public void stopStream() {
        sessionView.setText("SESSION ID");
        rtcClient.stopStream();
    }

    @Override
    public void onSocketCompleted() {
        SOCKET_STATE = SOCKET_OPEN_SUCCESS;
    }

    @Override
    public void onSocketFailed() {
        SOCKET_STATE = SOCKET_OPEN_FAIL;
    }

    @Override
    public void onStreamCreated() {

    }

    @Override
    public void onCreateFailed() {
        Toast.makeText(this, "Stream Create Failed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onViewFailed() {
        Toast.makeText(this, "Stream View Failed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStopCommunication() {
        rtcClient.webRTCApp.nbmWebRTCPeer.stopLocalMedia();
//        rtcClient.webRTCApp.nbmWebRTCPeer.close(true);
        rtcClient.webRTCApp.nbmWebRTCPeer.close();

        rtcClient.webRTCApp = null;

        vsv.onPause();
        vsv.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream) {

    }

    @Override
    public void onRemoveRemoteStream() {

    }

    private void requestPermissionForCameraAndMicrophone(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.RECORD_AUDIO)){
            Toast.makeText(this,
                    R.string.permissions_needed,
                    Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    CAMERA_MIC_PERMISSION_REQUEST_CODE);
        }
    }
}
