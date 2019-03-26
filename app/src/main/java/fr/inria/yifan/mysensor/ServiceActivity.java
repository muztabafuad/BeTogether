package fr.inria.yifan.mysensor;

/*
 * This activity provides functions related to the service discovery.
 */

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import fr.inria.yifan.mysensor.Context.FeatureHelper;
import fr.inria.yifan.mysensor.Context.ServiceHelper;
import fr.inria.yifan.mysensor.Deprecated.SensingActivity;

public class ServiceActivity extends AppCompatActivity {
// TODO

    public static final int ENABLE_REQUEST_WIFI = 1004;
    private static final String TAG = "Service activity";

    private final Object mLock; // Thread locker
    private boolean isRunning;

    // Declare adapter and device list
    private ArrayAdapter<String> mAdapterDevices;
    private TextView mServiceView;
    private TextView mWelcomeView;

    // Service helper
    private ServiceHelper mServiceHelper;
    private HashMap<String, String> mIntentsMsg;
    private HashMap<String, String> mServiceMsg;
    private FeatureHelper mFeatureHelper;

    public ServiceActivity() {
        mLock = new Object();
    }

    // Initially bind all views
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void bindViews() {
        mWelcomeView = findViewById(R.id.welcome_view);
        mWelcomeView.setText(R.string.hint_discovery);
        mServiceView = findViewById(R.id.service_view);

        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(view -> startSearching());

        Button stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(view -> stopSearching());

        // Build an adapter to feed the list with the content of an array of strings
        ArrayList<String> mNeighborList = new ArrayList<>();
        mAdapterDevices = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mNeighborList);
        // Attache the adapter to the list view
        ListView listView = findViewById(R.id.list_view);
        listView.setAdapter(mAdapterDevices);
    }

    // Main activity initialization
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        bindViews();

        mFeatureHelper = new FeatureHelper(this);
        mServiceHelper = new ServiceHelper(this, mAdapterDevices);
        // Create record messages for intents exchange and service allocation
        mIntentsMsg = new HashMap<>();
        mServiceMsg = new HashMap<>();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(mServiceHelper, intentFilter);
    }

    // Stop thread when exit!
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mServiceHelper);
    }

    @Override
    protected void onStop() {
        super.onStop();
        isRunning = false;
    }

    // Start the group engine
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressWarnings("unchecked")
    private void startSearching() {
        isRunning = true;
        mWelcomeView.setText(R.string.open_network);

        mFeatureHelper.startService(); // Start the context detection service
        mFeatureHelper.getContext(); // Get the current context information

        new Thread(() -> {
            // Fill the service intents message
            mIntentsMsg.put("MessageType", "IntentValues");
            mIntentsMsg.putAll(mFeatureHelper.getIntentValues(new int[]{1, 0, 1}));

            mServiceHelper.advertiseService(mIntentsMsg); // Advertise the service
            mServiceHelper.discoverService(); // Discovery the service

            // Wait 10s for the service discovery
            synchronized (mLock) {
                try {
                    mLock.wait(100000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            mServiceHelper.stopDiscover();
            mServiceHelper.stopAdvertise();

            // "Coordinator" "Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"
            if (mServiceHelper.isCoordinator()) { // Check if I am the coordinator
                StringBuilder sb = new StringBuilder();
                sb.append("I am coordinator:\n" + "Locator: ").append(mServiceHelper.findBestRole("Locator"))
                        .append(", Proxy: ").append(mServiceHelper.findBestRole("Proxy"))
                        .append(", Aggregator: ").append(mServiceHelper.findBestRole("Aggregator"))
                        .append(", Temperature: ").append(mServiceHelper.findBestRole("Temperature"))
                        .append(", Light: ").append(mServiceHelper.findBestRole("Light"))
                        .append(", Pressure: ").append(mServiceHelper.findBestRole("Pressure"))
                        .append(", Humidity: ").append(mServiceHelper.findBestRole("Humidity"))
                        .append(", Noise: ").append(mServiceHelper.findBestRole("Noise"));
                runOnUiThread(() -> mServiceView.setText(sb));

                // Fill the service allocation message
                mServiceMsg.put("MessageType", "ServiceAllocation");
                mServiceMsg.putAll(mServiceHelper.getAllocationMsg());
                mServiceHelper.advertiseService(mServiceMsg); // Advertise the service

            } else {

                mServiceHelper.discoverService();

                // Wait 10s for the service discovery
                synchronized (mLock) {
                    try {
                        mLock.wait(100000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(() -> mServiceView.setText(mServiceHelper.getMyService().toString()));
            }
        }).start();
    }

    // Stop the group engine
    private void stopSearching() {
        isRunning = false;
        mServiceHelper.stopDiscover();
        mServiceHelper.stopAdvertise();
        mFeatureHelper.stopService();
        mAdapterDevices.clear();
        mWelcomeView.setText(R.string.hint_discovery);
        mServiceView.setText(null);
    }

    // Check whether the Wifi is turned on
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
