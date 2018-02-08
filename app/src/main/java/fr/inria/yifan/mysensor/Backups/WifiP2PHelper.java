package fr.inria.yifan.mysensor.Backups;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;

/*
* This activity provides functions related to the Wifi Direct service.
*/

@SuppressLint("Registered")
public class WifiP2PHelper extends BroadcastReceiver {

    // Declare channel, manager and other references
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private Activity mActivity;
    private IntentFilter mIntentFilter;

    public WifiP2PHelper(Activity activity) {
        super();
        this.mActivity = activity;
        mManager = (WifiP2pManager) mActivity.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(mActivity, mActivity.getMainLooper(), null);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        /* register the broadcast receiver with the intent values to be matched */
        mActivity.registerReceiver(this, mIntentFilter);
    }

    public void startService() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reasonCode) {
            }
        });
    }

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
                break;
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                // Respond to new connection or disconnections
                break;
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                // Respond to this device's wifi state changing
                break;
        }
    }

    // Declare all views used

    // Declare Wifi Direct connect peer action listener

    // Declare Wifi Direct peer list listener

    // Declare Wifi Direct discover peer action listener

    // Declare a broadcast receiver

    // Clear all views content

    // Main activity initialization

}
