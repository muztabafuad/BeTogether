package fr.inria.yifan.mysensor.Context;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * This class provides functions related to the Wifi Direct service discovery.
 */

public class ServiceHelper {

    private static final String TAG = "Service helper";

    private static final float nMax = 10f; // Maximum number of members for a group

    // Variables
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pDnsSdServiceInfo mServiceInfo;
    private WifiP2pDnsSdServiceRequest mServiceRequest;

    private HashMap<String, String> mSelfIntent;
    private HashMap<String, HashMap<String, String>> mNeighborList; // Neighbor address and neighbor intents
    private ArrayAdapter<String> mAdapterNeighborList; // For the UI

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
            if (!mNeighborList.containsKey(device.deviceAddress)) {
                mAdapterNeighborList.add(device.deviceAddress + " " + record);
                mAdapterNeighborList.notifyDataSetChanged();
            }
            mNeighborList.put(device.deviceAddress, (HashMap) record);
            // "Coordinator" "Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"
            if (beCoordinator()) {
                Log.e(TAG, "Is coordinator.");
                Log.e(TAG, "Locator: " + findTheRole("Locator"));
                Log.e(TAG, "Proxy: " + findTheRole("Proxy"));
                Log.e(TAG, "Aggregator: " + findTheRole("Aggregator"));
                Log.e(TAG, "Temperature: " + findTheRole("Temperature"));
                Log.e(TAG, "Light: " + findTheRole("Light"));
                Log.e(TAG, "Pressure: " + findTheRole("Pressure"));
                Log.e(TAG, "Humidity: " + findTheRole("Humidity"));
                Log.e(TAG, "Noise: " + findTheRole("Noise"));
            }
        }
    };

    /* Listener for service information
     * String instance name
     * String registrationType
     * WifiP2pDevice source device
     */
    private WifiP2pManager.DnsSdServiceResponseListener servListener = (instanceName, registrationType, resourceType) -> {
        // Update the device name with the human-friendly version from the DnsTxtRecord, assuming one arrived.
        //Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
    };

    @SuppressWarnings("unchecked")
    // Constructor
    public ServiceHelper(Context context, ArrayAdapter adapter) {
        mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(context, context.getMainLooper(), null);
        mAdapterNeighborList = adapter;
    }

    @SuppressWarnings("unchecked")
    // Advertise the service with a HashMap message
    public void advertiseService(HashMap service) {
        // Service information. Pass it an instance name, service type _protocol._transport layer, and the map containing information other devices will want once they connect to this one.
        mServiceInfo = WifiP2pDnsSdServiceInfo.newInstance("crowdsensor", "_crowdsensing._tcp", service);
        mManager.addLocalService(mChannel, mServiceInfo, null);
        mSelfIntent = new HashMap<>();
        mSelfIntent.putAll(service);
    }

    // Discovery neighboring services
    public void discoverService() {
        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
        mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, mServiceRequest, null);
        mManager.discoverServices(mChannel, null);
        mNeighborList = new HashMap<>();
    }

    // Stop advertising the service
    public void stopAdvertise() {
        mManager.removeLocalService(mChannel, mServiceInfo, null);
    }

    // Stop discovering the service
    public void stopDiscover() {
        mManager.removeServiceRequest(mChannel, mServiceRequest, null);
    }

    @SuppressWarnings("ConstantConditions")
    // Look at self whether should be the coordinator or not
    public boolean beCoordinator() {
        // Ranking top k
        int k = Math.max(1, (int) (mNeighborList.size() / nMax));
        // Better than other _k
        int _k = mNeighborList.size() + 1 - k;
        int counter = 0;
        String selfIntent = mSelfIntent.get("Coordinator");
        for (String neighborAddr : mNeighborList.keySet()) {
            String neighborIntents = mNeighborList.get(neighborAddr).get("Coordinator");
            if (Float.parseFloat(selfIntent) >= Float.parseFloat(neighborIntents)) {
                counter += 1;
                if (counter == _k) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("ConstantConditions")
    // Given a role, look up the best crowdsensor address
    // "Coordinator" "Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"
    public String findTheRole(String role) {
        String selfIntent = mSelfIntent.get(role);
        for (String neighborAddr : mNeighborList.keySet()) {
            String neighborIntents = mNeighborList.get(neighborAddr).get(role);
            if (Float.parseFloat(neighborIntents) > Float.parseFloat(selfIntent)) {
                return neighborAddr;
            }
        }
        return "Self";
    }

    // Connect to a role as the coordinator (the coordinator is the group owner)
    // "Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"
    public void connectToRole(String role) {

    }

}
