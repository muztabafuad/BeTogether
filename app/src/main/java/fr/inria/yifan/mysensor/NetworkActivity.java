package fr.inria.yifan.mysensor;


/*
* This activity provides functions related to the Wifi Direct service.
*/

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;

import java.util.HashMap;
import java.util.Map;

import static fr.inria.yifan.mysensor.Support.Configuration.SERVER_PORT;

public class NetworkActivity extends AppCompatActivity {

    private static final String TAG = "Wifi Direct activity";

    // Declare all related views
    private ArrayAdapter<String> mAdapterWifi;

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);

        //  Create a string map containing information about your service.
        Map<String, String> record = new HashMap<>();
        record.put("listenport", String.valueOf(SERVER_PORT));
        record.put("buddyname", "John Doe" + (int) (Math.random() * 1000));
        record.put("available", "visible");
    }

    // Add to the custom adapter defined specifically for showing wifi devices.

}
