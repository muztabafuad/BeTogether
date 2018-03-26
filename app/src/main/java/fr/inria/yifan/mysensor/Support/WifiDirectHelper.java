package fr.inria.yifan.mysensor.Support;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_WIFI;

/**
 * This class provides functions functions related to the Wifi Direct service..
 */

public class WifiDirectHelper extends BroadcastReceiver {

    private static final String TAG = "Wifi Direct helper";

    // To store information from the peer
    private final HashMap<String, String> buddies = new HashMap<>();

    // Declare references and variables
    private Activity mActivity;
    private GroupServer mGroupServer;
    private GroupClient mGroupClient;
    private ArrayAdapter<WifiP2pDevice> mAdapterWifi;

    // Declare channel and Wifi Direct manager
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager.ConnectionInfoListener mConnectionListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            // Inet Address from WifiP2pInfo struct.
            InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            // After the group negotiation, we can determine the group owner (server).
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                // Do whatever tasks are specific to the group owner.
                // One common case is creating a group owner thread and accepting incoming connections.
                Toast.makeText(mActivity, "I am the group owner.", Toast.LENGTH_LONG).show();
                mGroupServer = new GroupServer();
            } else if (wifiP2pInfo.groupFormed) {
                // The other device acts as the peer (client).
                // In this case, you'll want to create a peer thread that connects to the group owner.
                Toast.makeText(mActivity, "I am the group member of " + groupOwnerAddress, Toast.LENGTH_LONG).show();
                mGroupClient = new GroupClient(groupOwnerAddress);
            }
        }
    };

    // Constructor
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public WifiDirectHelper(Activity activity) {
        super();
        mActivity = activity;

        WifiManager wifi = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // Check if Wifi service on system is enabled
        assert wifi != null;
        if (wifi.isWifiEnabled()) {
            // Initialize Wifi direct components
            mManager = (WifiP2pManager) mActivity.getSystemService(Context.WIFI_P2P_SERVICE);
            mChannel = mManager.initialize(mActivity, mActivity.getMainLooper(), null);
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

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        // Register the broadcast receiver with the intent values to be matched
        mActivity.registerReceiver(this, mIntentFilter);
    }

    // Add to the custom adapter defined specifically for showing wifi devices.
    public void setAdapterWifi(ArrayAdapter<WifiP2pDevice> adapter) {
        mAdapterWifi = adapter;
    }

    // Set the service record information and start thr service
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void startService(Map<String, String> record) {
        WifiP2pDnsSdServiceInfo mServiceInfo = WifiP2pDnsSdServiceInfo.newInstance("_connect", "_presence._tcp", record);
        // Add the local service, sending the service info, network channel and listener
        mManager.addLocalService(mChannel, mServiceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,
                Toast.makeText(mActivity, "Registration success", Toast.LENGTH_SHORT).show();
                discoverService();
            }

            @Override
            public void onFailure(int arg0) {
                // Command failed.
                Toast.makeText(mActivity, "Registration failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Close all registered services related to Wifi Direct
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void stopService() {
        mActivity.unregisterReceiver(this);
        mManager.stopPeerDiscovery(mChannel, null);
        mManager.clearLocalServices(mChannel, null);
        mManager.clearServiceRequests(mChannel, null);
    }

    // Start to discovery neighbors for services
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void discoverService() {
        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice resourceType) {
                // Update the device name with the version from the DnsTxtRecord
                resourceType.deviceName = buddies.containsKey(resourceType.deviceAddress) ? buddies.get(resourceType.deviceAddress) : resourceType.deviceName;
                Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
            }
        };
        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            // Callback includes TXT record and device running the advertised service
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
                buddies.put(device.deviceAddress, (String) record.get("listenport"));
                if (mAdapterWifi.getPosition(device) == -1) {
                    mAdapterWifi.add(device);
                }
            }
        };
        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);

        WifiP2pDnsSdServiceRequest mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, mServiceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Success!
                Toast.makeText(mActivity, "Service request success", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Toast.makeText(mActivity, "Service request failed", Toast.LENGTH_SHORT).show();
            }
        });
        mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Success!
                Toast.makeText(mActivity, "Discovery success", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                switch (code) {
                    case WifiP2pManager.P2P_UNSUPPORTED:
                        Log.d(TAG, "P2P isn't supported on this device.");
                        break;
                    case WifiP2pManager.BUSY:
                        Log.d(TAG, "The system is to busy to process the request.");
                        break;
                    case WifiP2pManager.ERROR:
                        Log.d(TAG, "The operation failed due to an internal error.");
                }
            }
        });
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
                Toast.makeText(mActivity, "Failed in connecting " + device.deviceAddress, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        assert action != null;
        switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                Log.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION");
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
                Log.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                // request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()
                Log.e(TAG, String.valueOf(mManager == null));
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
                Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
                // Respond to new connection or disconnections
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    // We are connected with the other device, request connection info to find group owner IP
                    mManager.requestConnectionInfo(mChannel, mConnectionListener);
                }
                break;
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
                // Respond to this device's wifi state changing
                break;
            case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                Log.d(TAG, "WIFI_P2P_DISCOVERY_CHANGED_ACTION");
                break;
        }
    }
}
