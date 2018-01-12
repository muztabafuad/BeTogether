package fr.inria.yifan.mysensor;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

public class WifiDirectActivity extends AppCompatActivity {

    // Declare views
    private TextView mTextTitle;
    private TextView mTextMessage;
    private TextView mTextMessage2;
    private TextView mTextMessage3;

    // Declare sensors and managers
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiBroadcastReceiver mReceiver;

    // Declare other references
    private IntentFilter mIntentFilter;
    private Activity mActivity = this;

    // Declare Wifi Direct discover peer action listener
    private WifiP2pManager.ActionListener mWifiDiscoverListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Toast.makeText(mActivity, "Wifi Direct peer discovered", Toast.LENGTH_SHORT).show();
            mTextMessage.setText("Wifi Direct peer discovered");
        }

        @Override
        public void onFailure(int reasonCode) {
            Toast.makeText(mActivity, "Wifi Direct peer not discovered", Toast.LENGTH_SHORT).show();
        }
    };
    // Declare Wifi Direct connect peer action listener
    private WifiP2pManager.ActionListener mWifiConnectListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Toast.makeText(mActivity, "Wifi Direct peer connected", Toast.LENGTH_SHORT).show();
            mTextMessage.setText("Wifi Direct peer connected");
        }

        @Override
        public void onFailure(int reasonCode) {
            Toast.makeText(mActivity, "Wifi Direct peer not connected", Toast.LENGTH_SHORT).show();
        }
    };
    // Declare Wifi Direct peer list listener
    public WifiP2pManager.PeerListListener mPeerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            mTextMessage2.setText("Wifi Direct peer list here:");
            mTextMessage3.setText(wifiP2pDeviceList.toString());
            //obtain a peer from the WifiP2pDeviceList
            for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList()) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                mManager.connect(mChannel, config, mWifiConnectListener);
            }
        }
    };

    // Initially bind views
    private void bindViews() {
        mTextTitle = findViewById(R.id.title);
        mTextMessage = findViewById(R.id.message);
        mTextMessage2 = findViewById(R.id.message2);
        mTextMessage3 = findViewById(R.id.message3);
    }

    // Clear all views content
    private void initialView() {
        mTextTitle.setText("WiFi Direct");
        mTextMessage.setText("...");
        mTextMessage2.setText("...");
        mTextMessage3.setText("...");
    }

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        bindViews();
        initialView();

        // Initialize Wifi direct components
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WifiBroadcastReceiver(mManager, mChannel, this);

        // Intent filter for receive broadcast
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Discover peers that are available
        mManager.discoverPeers(mChannel, mWifiDiscoverListener);
    }

    // register the broadcast receiver with the intent values to be matched
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    // unregister the broadcast receiver
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

}
