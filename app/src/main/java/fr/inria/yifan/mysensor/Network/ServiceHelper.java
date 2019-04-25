package fr.inria.yifan.mysensor.Network;

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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.inria.yifan.mysensor.Sensing.CrowdSensor;

import static fr.inria.yifan.mysensor.SensingActivity.SAMPLE_DELAY;
import static fr.inria.yifan.mysensor.SensingActivity.SAMPLE_NUMBER;

/**
 * This class provides functions related to the Wifi Direct service discovery and the role allocation.
 * Service roles: "Coordinator" "Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise".
 */

public class ServiceHelper extends BroadcastReceiver {

    private static final String TAG = "Service helper";

    private static final int nMax = 10; // Maximum number of members for a group

    // Variables
    private Context mContext;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    private HashMap<String, HashMap<String, String>> mNeighborContexts; // Neighbor MAC address and its context message
    private HashMap<String, HashMap<String, String>> mNeighborIntents; // Neighbor MAC address and its intents message
    private HashMap<String, List<String>> mNeighborService; // Neighbor MAC address and its service allocation list
    private HashMap<String, String> mNeighborAddress; // Neighbor MAC address and its IP address (for socket)

    private String mMacAddress; // MAC address of current device
    private HashMap<String, String> mSelfContext; // Context message of current device
    private HashMap<String, String> mSelfIntent; // Intents message of current device
    private List<String> mMyServices; // Services allocated for current device
    private boolean mIsCoordinator; // Coordinator flag of current device

    private ArrayAdapter<String> mAdapterNeighborList; // For the list shown in UI
    //private List<String> mMyConnect; // Connected devices for current device
    //private WifiP2pInfo mMyP2PInfo; // Wifi-Direct connection information

    @SuppressWarnings("unchecked")
    // Listener for record information
    private WifiP2pManager.DnsSdTxtRecordListener mTxtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
        /* Callback includes: fullDomain: full domain name: e.g "printer._ipp._tcp.local."
         * record: TXT record dta as a map of key/value pairs.
         * device: The device running the advertised service.
         */
        @Override
        public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
            Log.e(TAG, "DnsSdTxtRecord available -" + record.toString());
            // Check the discovered message type
            String msgType = (String) record.get("MessageType");
            assert msgType != null;
            switch (msgType) {
                // Context message is discovered
                case "ContextInfo":
                    // Check if matched as a neighbor
                    if (matchContext((HashMap<String, String>) record)) {
                        // Show the discovered context info in the list
                        if (!mNeighborContexts.containsKey(device.deviceAddress)) {
                            mAdapterNeighborList.add(device.deviceAddress + " " + record);
                            mAdapterNeighborList.notifyDataSetChanged();
                        }
                        // Put the neighbor context into list
                        mNeighborContexts.put(device.deviceAddress, (HashMap<String, String>) record);
                    }
                    break;
                // Intent message is discovered
                case "IntentValues":
                    // Show the discovered intent info in the list
                    if (!mNeighborIntents.containsKey(device.deviceAddress)) {
                        mAdapterNeighborList.add(device.deviceAddress + " " + record);
                        mAdapterNeighborList.notifyDataSetChanged();
                    }
                    // Put the neighbor intent into list
                    mNeighborIntents.put(device.deviceAddress, (HashMap<String, String>) record);
                    // Check to be the coordinator or not
                    mIsCoordinator = beCoordinator();
                    break;
                default:
                    Log.e(TAG, "Service discovered is not corrected.");
                    break;
            }
        }
    };

    /* Listener for service information
     * String instance name
     * String registrationType
     * WifiP2pDevice source device
     */
    private WifiP2pManager.DnsSdServiceResponseListener mServiceListener = (instanceName, registrationType, resourceType) -> {
        //Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
    };

    // Listener for Wifi-Direct group
    private WifiP2pManager.GroupInfoListener mGroupListener = group -> {
        Log.e(TAG, group.toString());

        // Record my connected devices
        //for (WifiP2pDevice member : group.getClientList()) {
        //    mMyConnect.add(member.deviceName + " " + member.deviceAddress);
        //}
        //WifiP2pDevice owner = group.getOwner();
        //mMyConnect.add(owner.deviceName + " " + owner.deviceAddress);
    };

    // When the network connection is changed
    private WifiP2pManager.ConnectionInfoListener mConnectListener = info -> {
        // A Wifi-Direct group was created
        if (info.groupFormed) {
            // I am the coordinator
            if (info.isGroupOwner) {
                mIsCoordinator = true;
                Log.e(TAG, "I am the coordinator");
                NetworkHelper helper = new NetworkHelper(true) {
                    @Override
                    JSONObject serverCallbackReceiveReply(JSONObject msg, String source) {
                        return handleReceivedMsg(msg, source);
                    }

                    @Override
                    void clientCallbackReceiveReply(JSONObject msg, String source) {
                    }
                };
            }
            // I am the collaborative member
            else {
                mIsCoordinator = false;
                Log.e(TAG, "I am a collaborator");
                NetworkHelper helper = new NetworkHelper(false) {
                    @Override
                    JSONObject serverCallbackReceiveReply(JSONObject msg, String source) {
                        return null;
                    }

                    @Override
                    void clientCallbackReceiveReply(JSONObject msg, String source) {
                        handleReceivedMsg(msg, source);
                    }
                };
                // First time connected to the server/coordinator, send hello message
                try {
                    JSONObject hello = new JSONObject();
                    hello.put("Type", "Hello");
                    hello.put("MAC", mMacAddress);
                    // Send to the coordinator and wait for receiving service allocation
                    helper.sendAndReceive(info.groupOwnerAddress.getHostAddress(), hello);
                    Log.e(TAG, "Sending message" + hello);
                } catch (Exception e) {
                    Log.e(TAG, "Can't put JSON: " + e);
                }
            }
        }
    };

    // Constructor
    public ServiceHelper(Context context, ArrayAdapter<String> adapter) {
        mContext = context;
        mManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(mContext, mContext.getMainLooper(), null);

        mNeighborContexts = new HashMap<>();
        mNeighborIntents = new HashMap<>();
        mNeighborService = new HashMap<>();
        mNeighborAddress = new HashMap<>();

        mMacAddress = null;
        mSelfContext = new HashMap<>();
        mSelfIntent = new HashMap<>();
        mMyServices = new ArrayList<>();
        mIsCoordinator = false;

        mAdapterNeighborList = adapter;
    }

    @SuppressWarnings("ConstantConditions")
    // Advertise the service with a HashMap message
    public void advertiseService(HashMap<String, String> service) {
        String msgType = service.get("MessageType");
        // Context message
        if (msgType.equals("ContextInfo")) {
            mSelfContext.putAll(service);
            Log.e(TAG, mSelfContext.toString());
        }
        // Intent message
        else if (msgType.equals("IntentValues")) {
            mSelfIntent.putAll(service);
            Log.e(TAG, mSelfIntent.toString());
        }
        // Service information.
        // Pass it an instance name, service type _protocol._transport layer, and the map containing information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo mServiceInfo = WifiP2pDnsSdServiceInfo.newInstance("crowdsensor", "_crowdsensing._tcp", service);
        mManager.addLocalService(mChannel, mServiceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //Log.d(TAG, "Success in advertisement.");
            }

            @Override
            public void onFailure(int i) {
                //Log.d(TAG, "Failed in advertisement.");
            }
        });
    }

    // Discovery neighboring services
    public void discoverService() {
        mManager.setDnsSdResponseListeners(mChannel, mServiceListener, mTxtListener);
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

    // Stop connected groups
    public void stopConnection() {
        mManager.removeGroup(mChannel, null);
        mManager.cancelConnect(mChannel, null);
        //if (!mMyConnect.isEmpty()) {
        //    mMyConnect.clear();
        //}
    }

    @SuppressWarnings("ConstantConditions")
    // Check if the context matches for the neighbor
    private boolean matchContext(HashMap<String, String> context) {
        return mSelfContext.get("UserActivity").equals(context.get("UserActivity")) &&
                Float.parseFloat(mSelfContext.get("DurationUA")) > 10f && Float.parseFloat(context.get("DurationUA")) > 10f &&
                mSelfContext.get("InDoor").equals(context.get("InDoor")) &&
                Float.parseFloat(mSelfContext.get("DurationDoor")) > 10f && Float.parseFloat(context.get("DurationDoor")) > 10f &&
                mSelfContext.get("UnderGround").equals(context.get("UnderGround")) &&
                Float.parseFloat(mSelfContext.get("DurationGround")) > 10f && Float.parseFloat(context.get("DurationGround")) > 10f;
    }

    // Get the history connection time of neighbors as a list
    public int[] getHistoryConnect() {
        int[] history = new int[mNeighborContexts.size()];
        for (int i = 0; i < mNeighborContexts.size(); i++) {
            history[i] = 1; // Assumed to be 1 minute for experiment
        }
        return history;
    }

    @SuppressWarnings("ConstantConditions")
    // Look at self whether should be the coordinator or not
    private boolean beCoordinator() {
        // Ranking top k number
        int k = Math.max(1, (mNeighborIntents.size() / nMax));
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

    // Return the coordinator flag
    public boolean isCoordinator() {
        return mIsCoordinator;
    }

    @SuppressWarnings("ConstantConditions")
    // Given a role, look up the best crowdsensor address (for coordinator)
    // "Coordinator" "Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"
    public String findBestCollaborator(String role) {
        float maxIntent = Float.parseFloat(mSelfIntent.get(role));
        String bestRole = "Self";
        float neighborIntent;
        // Iteration over all collaborative neighbors
        for (String neighborAddr : mNeighborIntents.keySet()) {
            neighborIntent = Float.parseFloat(mNeighborIntents.get(neighborAddr).get(role));
            if (neighborIntent > maxIntent) {
                maxIntent = neighborIntent;
                bestRole = neighborAddr;
            }
        }
        return bestRole;
    }

    // Each sensing service may be applied on multiple crowdsensors (for coordinator)
    // "Temperature", "Light", "Pressure", "Humidity", "Noise"
    public List<String> findMoreCollaborators(String role) {
        // TODO
        return null;
    }

    @SuppressWarnings("ConstantConditions")
    // Return the message containing neighbor address and its roles (for coordinator)
    public HashMap<String, List<String>> getAllocationMsg() {
        // Iteration over all collaborative roles
        for (String role : new String[]{"Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"}) {
            String collaborator = findBestCollaborator(role);
            if (collaborator.equals("Self")) {
                mMyServices.add(role);
            } else if (mNeighborService.containsKey(collaborator)) {
                List<String> services = mNeighborService.get(collaborator);
                services.add(role);
                mNeighborService.put(collaborator, services);
            } else {
                List<String> services = new ArrayList<>();
                services.add(role);
                mNeighborService.put(collaborator, services);
            }
        }
        return mNeighborService;
    }

    // Look up my services allocated in service message (for non-coordinator)
    private void readMyServices(HashMap<String, String> record) {
        mMyServices = new ArrayList<>();
        for (String role : record.keySet()) {
            String address = record.get(role);
            if (mMacAddress.equals(address)) {
                mMyServices.add(role);
            }
        }
    }

    // Get my current service list
    public List<String> getMyServices() {
        return mMyServices;
    }

    // Connect to a role as the coordinator (the coordinator is the group owner)
    // "Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"
    public void connectToRole(String role) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = findBestCollaborator(role);
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 15; // Max value

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

    // Connect to all neighbors as the coordinator (the coordinator is the group owner)
    public void connectAllMembers() {
        WifiP2pConfig config = new WifiP2pConfig();
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 15;

        Log.e(TAG, "Connection info: " + config);
        for (String member : mNeighborService.keySet()) {
            config.deviceAddress = member;
            mNeighborAddress.put(member, "192.168.49.1");
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

    /*
    // Create a group as the coordinator (the coordinator is the group owner)
    public void createGroup() {
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Device is ready to accept incoming connections from peers.
                Log.e(TAG, "Create group succeed.");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Create group failed: " + reason);
            }
        });
    }
    */

    // Get my current connection list
    ///public List<String> getMyConnects() {
    //return mMyConnect;
    //}

    @SuppressWarnings("unchecked")
    // Handle different type of messages and return a reply message (can be null)
    public JSONObject handleReceivedMsg(JSONObject msg, String source) {
        try {
            CrowdSensor crowdSensor;

            switch ((String) msg.get("Type")) {

                // Hello message handled by the server
                case "Hello":
                    // New client is connected, update the IP address to a MAC address
                    mNeighborAddress.put((String) msg.get("MAC"), source);
                    Log.e(TAG, "MAC IP pair updated: " + mNeighborAddress.toString());

                    // Reply a service allocation message
                    JSONObject allocation = new JSONObject();
                    allocation.put("Type", "ServiceAllocation");
                    allocation.put("Service", mNeighborService.get(msg.get("MAC")));
                    Log.e(TAG, "Replying message" + allocation);
                    return allocation;

                // Allocation message handled by clients
                case "ServiceAllocation":
                    Log.e(TAG, "Received message: " + msg + "from" + source);
                    mMyServices = (List<String>) msg.get("Service");
                    crowdSensor = new CrowdSensor(mContext) {
                        // When the work is finished
                        @Override
                        public void onWorkFinished(JSONObject result) {
                            Log.e(TAG, "Work finished: " + result);
                            // Send the sensing data to the server
                        }
                    };
                    crowdSensor.startWorkingThread(mMyServices, SAMPLE_NUMBER, SAMPLE_DELAY);
                    break;

                // Raw sensing data handled by aggregator
                case "RawData":
                    JSONObject result = CrowdSensor.doAggregation(null);
                    break;

                // Aggregated sensing data handled by proxy
                case "AggregatedData":
                    CrowdSensor.doProxyUpload(msg);
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
                break;
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                // The state of Wi-Fi P2P connectivity has changed.
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                // We are connected with the other device.
                if (networkInfo != null && networkInfo.isConnected()) {
                    // Retrieve the group information
                    mManager.requestGroupInfo(mChannel, mGroupListener);
                    // Retrieve the connection information
                    mManager.requestConnectionInfo(mChannel, mConnectListener);
                }
                break;
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                // This device's details have changed, read self MAC address
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                mMacAddress = device.deviceAddress;
                //Log.d(TAG, "Device WiFi P2p MAC Address: " + mMacAddress);
                break;
        }
    }
}
