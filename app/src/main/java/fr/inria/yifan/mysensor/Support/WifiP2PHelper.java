package fr.inria.yifan.mysensor.Support;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_WIFI;

/*
* This activity provides functions related to the Wifi Direct service.
*/

@SuppressLint("Registered")
public class WifiP2PHelper extends BroadcastReceiver {

    private static final String TAG = "Wifi P2P helper";

    // Declare channel, manager and other references
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private Activity mActivity;
    private IntentFilter mIntentFilter;
    private ArrayAdapter<WifiP2pDevice> mAdapterWifi;

    // Constructor.
    public WifiP2PHelper(Activity activity) {
        super();
        this.mActivity = activity;
    }

    // Add to the custom adapter defined specifically for showing wifi devices.
    public void setAdapterWifi(ArrayAdapter<WifiP2pDevice> adapter) {
        mAdapterWifi = adapter;
    }

    // Start to discover peers
    public void startService() {
        WifiManager wifi = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // Check if Wifi service on system is enabled
        assert wifi != null;
        if (wifi.isWifiEnabled()) {
            // Initialize components and intents
            mManager = (WifiP2pManager) mActivity.getSystemService(Context.WIFI_P2P_SERVICE);
            mChannel = mManager.initialize(mActivity, mActivity.getMainLooper(), null);
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
            // Register the broadcast receiver with the intent values to be matched
            mActivity.registerReceiver(this, mIntentFilter);

            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                // Declare Wifi Direct discover peer action listener
                @Override
                public void onSuccess() {
                    // Success!
                    Toast.makeText(mActivity, "Discovery success", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reasonCode) {
                }
            });
        } else {
            try {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                Toast.makeText(mActivity, "Please enable the WiFi", Toast.LENGTH_SHORT).show();
                mActivity.startActivityForResult(intent, ENABLE_REQUEST_WIFI);
            } catch (Exception e) {
                Log.e(TAG, String.valueOf(e));
                Toast.makeText(mActivity, "Please enable the WiFi", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Connecting to a peer
    public void connectTo(final WifiP2pDevice device) {
        //obtain a peer from the WifiP2pDeviceList
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            // Declare Wifi Direct connect peer action listener
            @Override
            public void onSuccess() {
                //success logic
                Toast.makeText(mActivity, "Connected to " + device.deviceAddress, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                //failure logic
            }
        });
    }

    // Creating a broadcast receiver for Wi-Fi P2P intents
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        assert action != null;
        switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                switch (state) {
                    case WifiP2pManager.WIFI_P2P_STATE_ENABLED:
                        // Wifi P2P is enabled
                        break;
                    default:
                        // Wi-Fi P2P is not enabled
                        break;
                }
                break;
            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                // request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()
                mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                    // Declare Wifi P2P peer list listener
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                        mAdapterWifi.clear();
                        mAdapterWifi.addAll(wifiP2pDeviceList.getDeviceList());
                    }
                });
                break;
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                // Respond to new connection or disconnections
                break;
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                // Respond to this device's wifi state changing
                break;
        }
    }

}
