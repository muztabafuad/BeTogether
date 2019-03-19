package fr.inria.yifan.mysensor;

/*
 * This activity provides functions related to the service discovery.
 */

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
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
    // Declare adapter and device list
    private ArrayAdapter<String> mAdapterDevices;

    // Service helper
    private ServiceHelper mServiceHelper;
    private HashMap<String, Object> mService;
    private FeatureHelper mFeatureHelper;

    // Initially bind all views
    private void bindViews() {
        final TextView welcomeView = findViewById(R.id.welcome_view);
        welcomeView.setText(R.string.hint_discovery);

        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(view -> {
            mServiceHelper.startService(mService);
            mServiceHelper.discoverService();
            welcomeView.setText(R.string.open_network);
        });

        Button stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(view -> {
            mServiceHelper.stopService();
            welcomeView.setText(R.string.hint_discovery);
        });

        // Build an adapter to feed the list with the content of an array of strings
        ArrayList<String> mDeviceList = new ArrayList<>();
        mAdapterDevices = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mDeviceList);

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
        mServiceHelper = new ServiceHelper(this, mAdapterDevices);
        mService = new HashMap<>();
        mService.put("Service", "Crowdsensing");
        mFeatureHelper = new FeatureHelper(this);
        mFeatureHelper.startService();
        mService.putAll(mFeatureHelper.getIntentValues(new int[]{0, 0, 0, 0, 0}));
    }

    // Stop thread when exit!
    @Override
    protected void onPause() {
        super.onPause();
        mFeatureHelper.stopService();
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
