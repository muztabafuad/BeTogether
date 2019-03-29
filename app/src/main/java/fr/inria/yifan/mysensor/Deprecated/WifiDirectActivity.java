package fr.inria.yifan.mysensor.Deprecated;

/*
 * This activity provides functions related to the Nearby service.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import fr.inria.yifan.mysensor.ContextActivity;
import fr.inria.yifan.mysensor.R;

@SuppressLint("Registered")
public class WifiDirectActivity extends AppCompatActivity {

    public static final int ENABLE_REQUEST_WIFI = 1004;
    private static final String TAG = "Nearby activity";
    // Declare adapter and device list
    private ArrayAdapter<WifiP2pDevice> mAdapterWifi;

    // Wifi direct helper
    private WifiDirectHelper mWifiDirectHelper;
    //private Map<String, String> record;

    // Initially bind all views
    private void bindViews() {
        final TextView welcomeView = findViewById(R.id.welcome_view);
        welcomeView.setText(R.string.hint_discovery);

        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(view -> {
            mWifiDirectHelper.startService(null);
            welcomeView.setText(R.string.hint_discovery);
        });

        // Build an adapter to feed the list with the content of an array of strings
        ArrayList<WifiP2pDevice> mDeviceList = new ArrayList<>();
        mAdapterWifi = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mDeviceList);

        // Attache the adapter to the list view
        ListView listView = findViewById(R.id.list_view);
        listView.setAdapter(mAdapterWifi);
        // attach a listener to the ListView to react to item click events
        //listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        //@Override
        //public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //WifiP2pDevice device = mAdapterWifi.getItem(position);
        //mWifiDirectHelper.connectTo(device);
    }
    //});
    //}

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        bindViews();
        mWifiDirectHelper = new WifiDirectHelper(this);
        mWifiDirectHelper.setAdapterWifi(mAdapterWifi);
    }

    // Stop thread when exit!
    @Override
    protected void onPause() {
        super.onPause();
        mWifiDirectHelper.stopService();
    }

    private void checkWifiActive() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // Check if Wifi service on system is enabled
        if (!wifi.isWifiEnabled()) {
            Toast.makeText(this, "Please enable the WiFi", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            startActivityForResult(intent, ENABLE_REQUEST_WIFI);
        }
    }

    // Go to the context activity
    public void goContext(View view) {
        Intent goToContext = new Intent();
        goToContext.setClass(this, ContextActivity.class);
        startActivity(goToContext);
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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ENABLE_REQUEST_WIFI: {
                checkWifiActive();
            }
        }
    }
}
