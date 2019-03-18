package fr.inria.yifan.mysensor.Deprecated;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides functions functions related to the Wifi Direct service.
 */

public class WifiDirectHelper extends BroadcastReceiver {

    private static final String TAG = "Wifi Direct helper";

    // To store information from the peer
    private HashMap<String, String> peers;

    // Declare references and variables
    private Context mContext;
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
                Toast.makeText(mContext, "I am the group owner.", Toast.LENGTH_LONG).show();
                //WifiServer mWifiServer = new WifiServer();
            } else if (wifiP2pInfo.groupFormed) {
                // The other device acts as the peer (client).
                // In this case, you'll want to create a peer thread that connects to the group owner.
                Toast.makeText(mContext, "I am the group member of " + groupOwnerAddress, Toast.LENGTH_LONG).show();
                //WifiClient mWifiClient = new WifiClient(groupOwnerAddress);
            }
        }
    };

    // Constructor
    public WifiDirectHelper(Context context) {
        mContext = context;

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        // Register the broadcast receiver with the intent values to be matched
        mContext.registerReceiver(this, mIntentFilter);
    }

    // Add to the custom adapter defined specifically for showing wifi devices.
    public void setAdapterWifi(ArrayAdapter<WifiP2pDevice> adapter) {
        mAdapterWifi = adapter;
    }

    // Set the service record information and startService thr service
    public void startService(Map<String, String> record) {
        // Initialize Wifi direct components
        mManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(mContext, mContext.getMainLooper(), null);

        WifiP2pDnsSdServiceInfo mServiceInfo = WifiP2pDnsSdServiceInfo.newInstance("_connect", "_presence._tcp", record);
        // Add the local service, sending the service info, network channel and listener
        mManager.addLocalService(mChannel, mServiceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,
                Toast.makeText(mContext, "Registration success", Toast.LENGTH_SHORT).show();
                discoverService();
            }

            @Override
            public void onFailure(int arg0) {
                // Command failed.
                Toast.makeText(mContext, "Registration failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Close all registered services related to Wifi Direct
    public void stopService() {
        mContext.unregisterReceiver(this);
        if (mManager != null && mChannel != null) {
            mManager.stopPeerDiscovery(mChannel, null);
            mManager.clearLocalServices(mChannel, null);
            mManager.clearServiceRequests(mChannel, null);
        }
    }

    // Start to discovery neighbors for services
    private void discoverService() {
        WifiP2pManager.DnsSdServiceResponseListener servListener = (instanceName, registrationType, resourceType) -> {
            // Update the device name with the version from the DnsTxtRecord
            resourceType.deviceName = peers.containsKey(resourceType.deviceAddress) ? peers.get(resourceType.deviceAddress) : resourceType.deviceName;
            Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
        };
        // Callback includes TXT record and device running the advertised service
        WifiP2pManager.DnsSdTxtRecordListener txtListener = (fullDomain, record, device) -> {
            Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
            peers.put(device.deviceAddress, record.get("listenport"));
            if (mAdapterWifi.getPosition(device) == -1) {
                mAdapterWifi.add(device);
            }
        };
        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);

        WifiP2pDnsSdServiceRequest mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, mServiceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Success!
                Toast.makeText(mContext, "Service request success", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Toast.makeText(mContext, "Service request failed", Toast.LENGTH_SHORT).show();
            }
        });
        mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Success!
                Toast.makeText(mContext, "Discovery success", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(mContext, "Connected to " + device.deviceAddress, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                //failure logic
                Toast.makeText(mContext, "Failed in connecting " + device.deviceAddress, Toast.LENGTH_SHORT).show();
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
                // Declare Wifi P2P peer list listener
                mManager.requestPeers(mChannel, wifiP2pDeviceList -> {
                    mAdapterWifi.clear();
                    mAdapterWifi.addAll(wifiP2pDeviceList.getDeviceList());
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
