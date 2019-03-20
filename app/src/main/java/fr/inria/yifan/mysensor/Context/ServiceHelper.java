package fr.inria.yifan.mysensor.Context;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.widget.ArrayAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class provides functions related to the Wifi Direct service discovery.
 */

public class ServiceHelper {

    private static final String TAG = "Service helper";

    // Variables
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pDnsSdServiceInfo mServiceInfo;
    private WifiP2pDnsSdServiceRequest mServiceRequest;

    private String mDeviceId;
    private HashMap<String, HashMap> mNeighborList;
    private ArrayAdapter<String> mAdapterNeighborList;

    // Listener for record information
    private WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
        /* Callback includes: fullDomain: full domain name: e.g "printer._ipp._tcp.local."
         * record: TXT record dta as a map of key/value pairs.
         * device: The device running the advertised service.
         */
        @Override
        public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
            //Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
            if (!mNeighborList.containsKey(device.deviceAddress)) {
                mAdapterNeighborList.add(device.deviceAddress + " " + record);
                mAdapterNeighborList.notifyDataSetChanged();
            }
            mNeighborList.put(device.deviceAddress, (HashMap) record);
        }
    };

    // Listener for service information
    private WifiP2pManager.DnsSdServiceResponseListener servListener = (instanceName, registrationType, resourceType) -> {
        // Update the device name with the human-friendly version from the DnsTxtRecord, assuming one arrived.
        //resourceType.deviceName = mDevices.containsKey(resourceType.deviceAddress) ? mDevices.get(resourceType.deviceAddress) : resourceType.deviceName;
        // Add to the custom adapter defined specifically for showing wifi devices.
        //mAdapterNeighbors.add(instanceName);
        //mAdapterNeighbors.notifyDataSetChanged();
        //Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
    };

    @SuppressWarnings("unchecked")
    // Constructor
    public ServiceHelper(Context context, ArrayAdapter adapter) {
        mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(context, context.getMainLooper(), null);
        mDeviceId = UUID.randomUUID().toString();
        mNeighborList = new HashMap<>();
        mAdapterNeighborList = adapter;
    }

    @SuppressWarnings("unchecked")
    // Advertise the service with a HashMap message
    public void advertiseService(HashMap service) {
        // Service information. Pass it an instance name, service type _protocol._transport layer, and the map containing information other devices will want once they connect to this one.
        mServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(mDeviceId, "_presence._udp", service);
        mManager.addLocalService(mChannel, mServiceInfo, null);
    }

    // Discovery neighboring services
    public void discoverService() {
        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
        mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, mServiceRequest, null);
        mManager.discoverServices(mChannel, null);
    }

    // Stop advertising the service
    public void stopAdvertise() {
        mManager.removeLocalService(mChannel, mServiceInfo, null);
    }

    // Stop discovering the service
    public void stopDiscover() {
        mManager.removeServiceRequest(mChannel, mServiceRequest, null);
    }

}
