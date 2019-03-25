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
    private HashMap<String, String> mServiceMsg;
    private FeatureHelper mFeatureHelper;

    public ServiceActivity() {
        mLock = new Object();
    }

    // Initially bind all views
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

        // Attach a listener to the ListView to react to item click events
        //listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        //@Override
        //public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //WifiP2pDevice device = mAdapterWifi.getItem(position);
        //mWifiDirectHelper.connectTo(device);
    }

    // Main activity initialization
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        bindViews();

        mFeatureHelper = new FeatureHelper(this);
        mFeatureHelper.startService();
        mFeatureHelper.getContext();

        mServiceHelper = new ServiceHelper(this, mAdapterDevices);
        // Create a service record message
        mServiceMsg = new HashMap<>();
        mServiceMsg.putAll(mFeatureHelper.getIntentValues(new int[]{1, 0, 1}));
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
        mFeatureHelper.stopService();
    }

    // Start the group engine
    private void startSearching() {
        isRunning = true;
        mServiceHelper.advertiseService(mServiceMsg);
        mServiceHelper.discoverService();
        mWelcomeView.setText(R.string.open_network);

        new Thread(() -> {
            while (isRunning) {
                // Delay
                synchronized (mLock) {
                    try {
                        mLock.wait(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(() -> {
                    // "Coordinator" "Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"
                    if (mServiceHelper.beCoordinator()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Is coordinator:\n" + "Locator: ").append(mServiceHelper.findTheRole("Locator"))
                                .append("\nProxy: ").append(mServiceHelper.findTheRole("Proxy"))
                                .append("\nAggregator: ").append(mServiceHelper.findTheRole("Aggregator"))
                                .append("\nTemperature: ").append(mServiceHelper.findTheRole("Temperature"))
                                .append("\nLight: ").append(mServiceHelper.findTheRole("Light"))
                                .append("\nPressure: ").append(mServiceHelper.findTheRole("Pressure"))
                                .append("\nHumidity: ").append(mServiceHelper.findTheRole("Humidity"))
                                .append("\nNoise: ").append(mServiceHelper.findTheRole("Noise"));
                        mServiceView.setText(sb);
                    }
                });

            }
        }).start();

    }

    // Stop the group engine
    private void stopSearching() {
        isRunning = false;
        mServiceHelper.stopDiscover();
        mServiceHelper.stopAdvertise();
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
