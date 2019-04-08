package fr.inria.yifan.mysensor.Context;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides functions related to the Wifi Direct service discovery.
 */

public class ServiceHelper extends BroadcastReceiver {

    private static final String TAG = "Service helper";

    private static final float nMax = 10f; // Maximum number of members for a group

    // Variables
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private String mMacAddress;
    private boolean mIsCoordinator;

    private HashMap<String, HashMap<String, String>> mNeighborContexts; // Neighbor address and its context message
    private HashMap<String, HashMap<String, String>> mNeighborIntents; // Neighbor address and its intents message
    private HashMap<String, String> mNeighborService; // Neighbor service and its address allocation message
    private HashMap<String, String> mSelfIntent; // Intents message of current device
    private List<String> mMyServices; // Services allocated for current device

    private ArrayAdapter<String> mAdapterNeighborList; // For the list shown in UI

    @SuppressWarnings("unchecked")
    // Listener for record information
    private WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
        /* Callback includes: fullDomain: full domain name: e.g "printer._ipp._tcp.local."
         * record: TXT record dta as a map of key/value pairs.
         * device: The device running the advertised service.
         */
        @Override
        public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
            //Log.e(TAG, "DnsSdTxtRecord available -" + record.toString());

            // Show the discovered device in the list
            mAdapterNeighborList.add(device.deviceAddress + " " + record);
            mAdapterNeighborList.notifyDataSetChanged();

            // Check the discovered message type
            String msgType = (String) record.get("MessageType");
            if (msgType != null) {
                switch (msgType) {
                    case "ContextInfo":
                        Log.e(TAG, "Received context message" + record);
                        // TODO context matching here
                        mNeighborContexts.put(device.deviceAddress, (HashMap) record);
                        break;
                    case "IntentValues":
                        Log.e(TAG, "Received intent message" + record);
                        // Put the neighbor intent into list
                        mNeighborIntents.put(device.deviceAddress, (HashMap) record);
                        // Check to be the coordinator or not
                        mIsCoordinator = beCoordinator();
                        break;
                    case "ServiceAllocation":
                        Log.e(TAG, "Received allocation message" + record);
                        // Retrieve the allocation message for me
                        findMyServices((HashMap) record);
                        break;
                }
            }
        }
    };

    /* Listener for service information
     * String instance name
     * String registrationType
     * WifiP2pDevice source device
     */
    private WifiP2pManager.DnsSdServiceResponseListener servListener = (instanceName, registrationType, resourceType) -> {
        //Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
    };

    @SuppressWarnings("unchecked")
// Constructor
    public ServiceHelper(Context context, ArrayAdapter adapter) {
        mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(context, context.getMainLooper(), null);
        mAdapterNeighborList = adapter;
        mIsCoordinator = false;

        mNeighborContexts = new HashMap<>();
        mNeighborIntents = new HashMap<>();
        mNeighborService = new HashMap<>();
        mSelfIntent = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
// Advertise the service with a HashMap message
    public void advertiseService(HashMap service) {
        String msgType = (String) service.get("MessageType");
        assert msgType != null;
        if (msgType.equals("IntentValues")) {
            mSelfIntent.putAll(service);
            //Log.e(TAG, mSelfIntent.toString());
        }
        // Service information. Pass it an instance name, service type _protocol._transport layer, and the map containing information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo mServiceInfo = WifiP2pDnsSdServiceInfo.newInstance("crowdsensor", "_crowdsensing._tcp", service);
        mManager.addLocalService(mChannel, mServiceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e(TAG, "Success in advertisement.");
            }

            @Override
            public void onFailure(int i) {
                Log.e(TAG, "Failed in advertisement.");
            }
        });
    }

    // Discovery neighboring services
    public void discoverService() {
        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
        WifiP2pDnsSdServiceRequest mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, mServiceRequest, null);

        mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e(TAG, "Success in discovery.");
            }

            @Override
            public void onFailure(int i) {
                Log.e(TAG, "Failed in discovery.");
            }
        });
    }

    // Stop advertising the service
    public void stopAdvertise() {
        mManager.clearLocalServices(mChannel, null);
    }

    // Stop discovering the service
    public void stopDiscover() {
        mManager.clearServiceRequests(mChannel, null);
    }

    // Get the history connection times of neighbors as a list
    public int[] getHistoryConnect() {
        int[] history = new int[mNeighborContexts.size()];
        for (int i = 0; i < mNeighborContexts.size(); i++) {
            history[i] = 1; // Assumed to be 1 for experiment
        }
        return history;
    }

    @SuppressWarnings("ConstantConditions")
    // Look at self whether should be the coordinator or not
    private boolean beCoordinator() {
        // Ranking top k number
        int k = Math.max(1, (int) (mNeighborIntents.size() / nMax));
        // Better than other _k neighbors
        int _k = mNeighborIntents.size() + 1 - k;
        int counter = 0;
        String selfIntent = mSelfIntent.get("Coordinator");
        for (String neighborAddr : mNeighborIntents.keySet()) {
            String neighborIntent = mNeighborIntents.get(neighborAddr).get("Coordinator");
            if (Float.parseFloat(selfIntent) > Float.parseFloat(neighborIntent)) {
                counter++;
                if (counter == _k) {
                    return true;
                }
            }
        }
        return false;
    }

    // Return the flag
    public boolean isCoordinator() {
        return mIsCoordinator;
    }

    @SuppressWarnings("ConstantConditions")
    // Given a role, look up the best crowdsensor address
    // "Coordinator" "Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"
    public String findBestRole(String role) {
        float maxIntent = Float.parseFloat(mSelfIntent.get(role));
        String maxNeighbor = "Self";
        float neighborIntent;

        for (String neighborAddr : mNeighborIntents.keySet()) {
            neighborIntent = Float.parseFloat(mNeighborIntents.get(neighborAddr).get(role));
            if (neighborIntent > maxIntent) {
                maxIntent = neighborIntent;
                maxNeighbor = neighborAddr;
            }
        }
        return maxNeighbor;
    }

    // Each sensing service may use multiple crowdsensors
    // "Temperature", "Light", "Pressure", "Humidity", "Noise"
    public List<String> findMoreRole(String role) {
        // TODO
        return null;
    }

    // Return the message containing neighbor address and its role
    public HashMap getAllocationMsg() {
        mMyServices = new ArrayList<>();
        // One neighbor for one role
        for (String role : new String[]{"Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"}) {
            String neighbor = findBestRole(role);
            if (!neighbor.equals("Self")) {
                mNeighborService.put(role, neighbor);
            } else {
                mMyServices.add(role);
            }
        }
        return mNeighborService;
    }

    // Look up my services allocated in message
    private void findMyServices(HashMap<String, String> record) {
        mMyServices = new ArrayList<>();
        for (String role : record.keySet()) {
            String address = record.get(role);
            if (mMacAddress.equals(address)) {
                mMyServices.add(role);
            }
        }
    }

    // Get my service list
    public List<String> getMyServices() {
        return mMyServices;
    }

    // Connect to a role as the coordinator (the coordinator is the group owner)
    // "Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"
    public void connectToRole(String role) {

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = findBestRole(role);
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 15;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
                Log.e(TAG, "Connect succeed.");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Connect failed to " + config.deviceAddress + ", because: " + reason);
            }
        });
    }

    // Connect to all group members
    public void connectAllMembers() {
        WifiP2pConfig config = new WifiP2pConfig();
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 15;

        for (String member : mNeighborService.values()) {
            config.deviceAddress = member;
            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
                    Log.e(TAG, "Connect succeed.");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Connect failed to " + config.deviceAddress + ", because: " + reason);
                }
            });
        }
    }

    // Create a group as the coordinator (the coordinator is the group owner)
    public void createGroup() {
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Device is ready to accept incoming connections from peers.
                Log.e(TAG, "Create group succeed.");
                mManager.requestGroupInfo(mChannel, wifiP2pGroup -> Log.e(TAG, wifiP2pGroup.toString()));
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Create group failed: " + reason);
            }
        });
    }

    // Callback when receive Wifi P2P broadcast
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        assert action != null;
        switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                // A change in the Wi-Fi P2P status.
                break;
            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                // A change in the list of available peers.
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.isConnected()) {
                    // We are connected with the other device.
                    mManager.requestGroupInfo(mChannel, wifiP2pGroup -> Log.e(TAG, wifiP2pGroup.toString()));
                }
                break;
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                // The state of Wi-Fi P2P connectivity has changed.
                break;
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                // This device's details have changed.
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                mMacAddress = device.deviceAddress;
                //Log.d(TAG, "Device WiFi P2p MAC Address: " + mMacAddress);
                break;
        }
    }
}
