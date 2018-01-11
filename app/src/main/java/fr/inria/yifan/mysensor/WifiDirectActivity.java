package fr.inria.yifan.mysensor;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

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

    private IntentFilter mIntentFilter;
    private Activity mActivity = this;

    // Declare Wifi Direct listener
    public WifiP2pManager.ActionListener mWifiListener = new WifiP2pManager.ActionListener() {
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

    public WifiP2pManager.PeerListListener mPeerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            mTextMessage2.setText("Wifi Direct peer list here");
            mTextMessage3.setText(wifiP2pDeviceList.toString());
        }
    };

    // Initially bind views
    private void bindViews() {
        mTextTitle = (TextView) findViewById(R.id.title);
        mTextMessage = (TextView) findViewById(R.id.message);
        mTextMessage2 = (TextView) findViewById(R.id.message2);
        mTextMessage3 = (TextView) findViewById(R.id.message3);
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

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WifiBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager.discoverPeers(mChannel, mWifiListener);
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

    // Convert a list into a string
    private String getListString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (String str : list) {
            sb.append(i + " " + str + ".\n");
            i++;
        }
        return sb.toString();
    }
}
