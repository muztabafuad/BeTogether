package fr.inria.yifan.mysensor;

/*
 * This activity provides functions related to the service discovery.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import fr.inria.yifan.mysensor.Context.ContextHelper;
import fr.inria.yifan.mysensor.Deprecated.SensingActivity;
import fr.inria.yifan.mysensor.Network.ServiceHelper;

public class ServiceActivity extends AppCompatActivity {
// TODO

    public static final int ENABLE_REQUEST_WIFI = 1004;
    private static final String TAG = "Service activity";

    private final Object mLock; // Thread locker
    private boolean isRunning;

    // Declare adapter and device list
    private TextView mWelcomeView;
    private ArrayAdapter<String> mAdapterDevices;
    private TextView mServiceView;

    // Service helper
    private ServiceHelper mServiceHelper;
    private HashMap<String, String> mContextMsg;
    private HashMap<String, String> mIntentsMsg;
    private ContextHelper mContextHelper;

    // Variables
    private BatteryManager mBatteryManager;
    private float mStartBattery;
    private long mStartTime;

    // Constructor
    public ServiceActivity() {
        mLock = new Object();
    }

    // Initially bind all views
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void bindViews() {
        mWelcomeView = findViewById(R.id.welcome_view);
        mWelcomeView.setText(R.string.hint_discovery);
        mServiceView = findViewById(R.id.service_view);

        Button mContextButton = findViewById(R.id.context_button);
        mContextButton.setOnClickListener(view -> contextExchanging());

        Button mIntentButton = findViewById(R.id.intent_button);
        mIntentButton.setOnClickListener(view -> intentExchanging());

        Button mServiceButton = findViewById(R.id.service_button);
        mServiceButton.setOnClickListener(view -> serviceExchanging());

        Button mStopButton = findViewById(R.id.stop_button);
        mStopButton.setOnClickListener(view -> stopExchanging());

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

        mContextHelper = new ContextHelper(this);
        mServiceHelper = new ServiceHelper(this, mAdapterDevices);
        // Create record messages for intents exchange and service allocation
        mContextMsg = new HashMap<>();
        mIntentsMsg = new HashMap<>();

        mContextHelper.startService(); // Start the context detection service

        // Get the remaining battery in mAh
        mBatteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        mContextHelper.stopService();
        stopExchanging();
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

    // Start the exchanging of context message
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressWarnings("unchecked")
    private void contextExchanging() {

        mWelcomeView.setText(R.string.open_context);

        // Record the battery and time when start
        mStartBattery = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000f;
        mStartTime = System.currentTimeMillis();

        // Get the current context information
        HashMap contexts = mContextHelper.getContext();
        // Fill the context information message
        mContextMsg.put("MessageType", "ContextInfo");
        mContextMsg.put("UserActivity", (String) contexts.get("UserActivity"));
        mContextMsg.put("DurationUA", (String) contexts.get("DurationUA"));
        mContextMsg.put("InDoor", (String) contexts.get("InDoor"));
        mContextMsg.put("DurationDoor", (String) contexts.get("DurationDoor"));
        mContextMsg.put("UnderGround", (String) contexts.get("UnderGround"));
        mContextMsg.put("DurationGround", (String) contexts.get("DurationGround"));
        Log.e(TAG, mContextMsg.toString());

        mServiceHelper.advertiseService(mContextMsg); // Advertise the service
        mServiceHelper.discoverService(); // Discovery the service
    }

    // Start the exchanging of intent message
    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressWarnings("unchecked")
    private void intentExchanging() {

        mWelcomeView.setText(R.string.open_intents);

        // Fill the intent information message
        mIntentsMsg.put("MessageType", "IntentValues");
        mIntentsMsg.putAll(mContextHelper.getIntentValues(mServiceHelper.getHistoryConnect()));
        Log.e(TAG, mIntentsMsg.toString());

        mServiceHelper.advertiseService(mIntentsMsg); // Advertise the service
        mServiceHelper.discoverService(); // Discovery the service
    }

    // Start the exchanging of service message
    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressWarnings("unchecked")
    private void serviceExchanging() {

        mWelcomeView.setText(R.string.open_service);

        // I am the coordinator
        if (mServiceHelper.isCoordinator()) {

            String allocation = mServiceHelper.getAllocationMsg().toString();
            mServiceHelper.connectAllMembers();

            isRunning = true;
            new Thread(() -> {
                while (isRunning) {
                    runOnUiThread(() -> mServiceView.setText("I am the coordinator: " + mServiceHelper.getMyServices()
                            + "\nService allocation are: "
                            + allocation));
                    //+ "\nMy connected devices are: "
                    //+ mServiceHelper.getMyConnects()));
                    //+ "\nMy collaborative power consumption is: "
                    //+ mContextHelper.getPowerTotal(mServiceHelper.getMyServices(), 10, false)
                    //+ "\nMy individual power consumption is: "
                    //+ mContextHelper.getPowerTotal(Arrays.asList("Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"), 10, true)));
                    // Delay
                    synchronized (mLock) {
                        try {
                            mLock.wait(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } else {
            isRunning = true;
            new Thread(() -> {
                while (isRunning) {
                    runOnUiThread(() -> mServiceView.setText("My services allocated are: " + mServiceHelper.getMyServices()));
                    //+ "\nMy connected devices are: "
                    //+ mServiceHelper.getMyConnects()));
                    //+ "\nMy collaborative power consumption is: "
                    //+ mContextHelper.getPowerTotal(mServiceHelper.getMyServices(), 10, false)
                    //+ "\nMy individual power consumption is: "
                    //+ mContextHelper.getPowerTotal(Arrays.asList("Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"), 10, true)));
                    // Delay
                    synchronized (mLock) {
                        try {
                            mLock.wait(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    // Stop the group engine
    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void stopExchanging() {
        isRunning = false;
        mServiceHelper.stopDiscover();
        mServiceHelper.stopAdvertise();
        mServiceHelper.stopConnection();
        mAdapterDevices.clear();

        float currentBattery = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000f;
        long currentTime = System.currentTimeMillis();

        mWelcomeView.setText("Power energy consumed in mA: " + (mStartBattery - currentBattery) +
                "\nTime consumed in s: " + (currentTime - mStartTime) / 1000);
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
