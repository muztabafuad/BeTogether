package fr.inria.yifan.mysensor;


/*
* This activity provides functions related to the Wifi Direct service.
*/

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;

import java.util.HashMap;
import java.util.Map;

import fr.inria.yifan.mysensor.Support.WifiP2PHelper;

import static fr.inria.yifan.mysensor.Support.Configuration.SERVER_PORT;

public class NetworkActivity extends AppCompatActivity {

    private static final String TAG = "Wifi Direct activity";

    // Declare all related views
    private ArrayAdapter<String> mAdapterWifi;

    // Wifi Direct helper
    private WifiP2PHelper mWifiP2PHelper;

    // Main activity initialization
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);
        mWifiP2PHelper = new WifiP2PHelper(this);

        //  Create a string map containing information about your service.
        Map<String, String> record = new HashMap<>();
        record.put("listenport", String.valueOf(SERVER_PORT));
        record.put("buddyname", "John Doe" + (int) (Math.random() * 1000));
        record.put("available", "visible");
        mWifiP2PHelper.startService(record);
    }

    // Add to the custom adapter defined specifically for showing wifi devices.

}
