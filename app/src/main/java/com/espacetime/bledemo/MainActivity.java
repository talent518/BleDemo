package com.espacetime.bledemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onCentralClick(View view) {
        Intent intent = new Intent(this, CentralActivity.class);
        startActivity(intent);
    }

    public void onPerpheralClick(View view) {
        Intent intent = new Intent(this, PeripheralActivity.class);
        startActivity(intent);
    }

}
