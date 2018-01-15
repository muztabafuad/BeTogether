package fr.inria.yifan.mysensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.aware.WifiAwareManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

/*
* This activity provides functions related to the Wifi Aware service.
*/

public class WifiAwareActivity extends AppCompatActivity {

    // Declare all views used
    private TextView mTextTitle;
    private TextView mTextMessage;
    private TextView mTextMessage2;
    private TextView mTextMessage3;

    // Declare Wifi Aware managers
    private WifiAwareManager mManager;

    // Declare intent filter for Wifi Aware
    private IntentFilter mIntentFilter = new IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);

    // Declare a broadcast receiver
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mManager.isAvailable()) {
                mTextMessage2.setText("Wifi Aware is available.");
            } else {
                mTextMessage3.setText("Wifi Aware is unavailable.");
            }
        }
    };

    // Initially bind views
    private void bindViews() {
        mTextTitle = findViewById(R.id.title);
        mTextMessage = findViewById(R.id.message);
        mTextMessage2 = findViewById(R.id.message2);
        mTextMessage3 = findViewById(R.id.message3);
    }

    // Clear all views content
    private void initialView() {
        mTextTitle.setText("WiFi Aware");
        mTextMessage.setText("...");
        mTextMessage2.setText("...");
        mTextMessage3.setText("...");
    }

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        bindViews();
        initialView();
        testWifiAware();
    }

    private void testWifiAware() {
        boolean flag = getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE);
        mTextMessage.setText("Wifi Aware support in this device: " + flag);
        //TODO
        mManager = (WifiAwareManager) getSystemService(Context.WIFI_AWARE_SERVICE);
        this.registerReceiver(mReceiver, mIntentFilter);
    }

    // register the broadcast receiver with the intent values to be matched
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    // unregister the broadcast receiver
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

}
