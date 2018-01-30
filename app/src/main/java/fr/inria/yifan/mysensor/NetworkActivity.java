package fr.inria.yifan.mysensor;


/*
* This activity provides functions related to the Wifi Direct service.
*/

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;

import java.util.HashMap;
import java.util.Map;

import fr.inria.yifan.mysensor.Support.WifiP2PHelper;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_WIFI;
import static fr.inria.yifan.mysensor.Support.Configuration.SERVER_PORT;

public class NetworkActivity extends AppCompatActivity {

    private static final String TAG = "Wifi Direct activity";

    // Declare all related views
    private ArrayAdapter<String> mAdapterWifi;

    // Wifi Direct helper
    private WifiP2PHelper mWifiP2PHelper;
    private Map<String, String> record;

    // Main activity initialization
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);
        mWifiP2PHelper = new WifiP2PHelper(this);

        //  Create a string map containing information about your service.
        record = new HashMap<>();
        record.put("listenport", String.valueOf(SERVER_PORT));
        record.put("deviceid", "Device " + (int) (Math.random() * 1000));
        record.put("available", "visible");

        mWifiP2PHelper.startService(record);
    }

    // Callback for user enabling Wifi switch
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ENABLE_REQUEST_WIFI: {
                mWifiP2PHelper.startService(record);
            }
        }
    }


    // Add to the custom adapter defined specifically for showing wifi devices.

}
