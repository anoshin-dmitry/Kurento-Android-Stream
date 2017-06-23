package com.example.max.websockettest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity implements View.OnClickListener {

    Button createButton;
    Button viewButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_create).setOnClickListener(this);
        findViewById(R.id.btn_view).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_create:
                startActivity(new Intent(MainActivity.this, CreateActivity.class));
                break;
            case R.id.btn_view:
                startActivity(new Intent(MainActivity.this, ViewerActivity.class));
                break;
        }
    }
}
