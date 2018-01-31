package fr.inria.yifan.mysensor.Backups;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import fr.inria.yifan.mysensor.R;

/*
* This activity provides functions related to the Wifi Direct service.
*/

@SuppressLint("Registered")
public class WifiP2PHelper {

    // Declare all views used
    private TextView mTextTitle;
    private TextView mTextMessage;
    private TextView mTextMessage2;
    private TextView mTextMessage3;

    // Declare channel and manager
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    // Declare other references
    private IntentFilter mIntentFilter;
    private Activity mActivity;
    // Declare Wifi Direct connect peer action listener
    private WifiP2pManager.ActionListener mWifiConnectListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Toast.makeText(mActivity, "Wifi Direct peer connected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailure(int reasonCode) {
            Toast.makeText(mActivity, "Wifi Direct peer not connected", Toast.LENGTH_SHORT).show();
        }
    };
    // Declare Wifi Direct peer list listener
    private WifiP2pManager.PeerListListener mPeerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            mTextMessage3.setText(wifiP2pDeviceList.toString());
            //obtain a peer from the WifiP2pDeviceList
            for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList()) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                Toast.makeText(mActivity, "Connecting to " + device.deviceAddress, Toast.LENGTH_SHORT).show();
                mManager.connect(mChannel, config, mWifiConnectListener);
            }
        }
    };
    // Declare Wifi Direct discover peer action listener
    private WifiP2pManager.ActionListener mWifiDiscoverListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Toast.makeText(mActivity, "Wifi Direct peer discovered", Toast.LENGTH_SHORT).show();
            mManager.requestPeers(mChannel, mPeerListListener);
        }

        @Override
        public void onFailure(int reasonCode) {
            Toast.makeText(mActivity, "Wifi Direct peer not discovered", Toast.LENGTH_SHORT).show();
        }
    };
    // Declare a broadcast receiver
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Toast.makeText(mActivity, "Wifi Direct is enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mActivity, "Wifi Direct is disabled", Toast.LENGTH_SHORT).show();
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                if (mManager != null) {
                    mManager.requestPeers(mChannel, mPeerListListener);
                }
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                // Respond to new connection or disconnections
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // Respond to this device's wifi state changing
            }
        }
    };

    // Clear all views content
    private void initialView() {
        mTextTitle.setText(R.string.title_network);
        mTextMessage.setText("...");
        mTextMessage2.setText("...");
        mTextMessage3.setText("...");
    }

    // Main activity initialization
    protected void onCreate(Bundle savedInstanceState) {
        initialView();

        // Intent filter for receive broadcast
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Start to discover peers that are available
        mManager.discoverPeers(mChannel, mWifiDiscoverListener);
    }

}
