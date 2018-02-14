package fr.inria.yifan.mysensor;

/*
* This activity provides functions related to the Wifi Direct service.
*/

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fr.inria.yifan.mysensor.Support.WifiDirectHelper;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_WIFI;
import static fr.inria.yifan.mysensor.Support.Configuration.SERVER_PORT;

public class NetworkActivity extends AppCompatActivity {

    private static final String TAG = "Wifi Direct activity";

    // Declare adapter and device list
    private ArrayAdapter<WifiP2pDevice> mAdapterWifi;
    private ArrayList<WifiP2pDevice> mDeviceList;

    // Wifi Direct helper
    private WifiDirectHelper mWifiDirectHelper;
    //private WifiP2PHelper mWifiP2PHelper;
    private Map<String, String> record;

    // Initially bind all views
    private void bindViews() {

        TextView welcomeView = findViewById(R.id.welcome_view);
        welcomeView.setText(R.string.hint_network);

        // Build an adapter to feed the list with the content of an array of strings
        mDeviceList = new ArrayList<>();
        mAdapterWifi = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mDeviceList);

        // Attache the adapter to the list view
        ListView listView = findViewById(R.id.list_view);
        listView.setAdapter(mAdapterWifi);
    }

    // Main activity initialization
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);
        bindViews();
        mWifiDirectHelper = new WifiDirectHelper(this);
        mWifiDirectHelper.setAdapterWifi(mAdapterWifi);
        //mWifiP2PHelper = new WifiP2PHelper(this);
        //mWifiP2PHelper.setAdapterWifi(mAdapterWifi);

        //  Create a string map containing information about your service.
        record = new HashMap<>();
        record.put("listenport", String.valueOf(SERVER_PORT));
        record.put("deviceid", "Device " + (int) (Math.random() * 1000));
        record.put("available", "visible");

        mWifiDirectHelper.startService(record);
        //mWifiP2PHelper.startService();
    }

    // Go to the detection activity
    public void goDetection(View view) {
        Intent goToDetection = new Intent();
        goToDetection.setClass(this, DetectionActivity.class);
        startActivity(goToDetection);
        finish();
    }

    // Go to the sensing activity
    public void goSensing(View view) {
        Intent goToSensing = new Intent();
        goToSensing.setClass(this, SensingActivity.class);
        startActivity(goToSensing);
        finish();
    }

    // Callback for user enabling Wifi switch
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ENABLE_REQUEST_WIFI: {
                mWifiDirectHelper.startService(record);
                //mWifiP2PHelper.startService();
            }
        }
    }
}
