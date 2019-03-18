package fr.inria.yifan.mysensor.Context;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class provides functions functions related to the Wifi Direct service discovery.
 */

public class ServiceHelper {

    private static final String TAG = "Service helper";

    // Variables
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;
    private WifiP2pDnsSdServiceInfo mServiceInfo;
    private WifiP2pDnsSdServiceRequest mServiceRequest;

    private String mDeviceId;
    //private HashMap<String, String> mDevices;
    private ArrayAdapter<String> mAdapterDevices;

    private WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
        /* Callback includes: fullDomain: full domain name: e.g "printer._ipp._tcp.local."
         * record: TXT record dta as a map of key/value pairs. device: The device running the advertised service.
         */
        @Override
        public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
            Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
            mAdapterDevices.add(device.deviceName + " " + record.toString());
            mAdapterDevices.notifyDataSetChanged();
            //mDevices.put(device.deviceAddress, (String) record.get("device_id"));
        }
    };

    private WifiP2pManager.DnsSdServiceResponseListener servListener = (instanceName, registrationType, resourceType) -> {
        // Update the device name with the human-friendly version from the DnsTxtRecord, assuming one arrived.
        //resourceType.deviceName = mDevices.containsKey(resourceType.deviceAddress) ? mDevices.get(resourceType.deviceAddress) : resourceType.deviceName;
        // Add to the custom adapter defined specifically for showing wifi devices.
        //mAdapterDevices.add(instanceName);
        //mAdapterDevices.notifyDataSetChanged();
        Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
    };

    // Constructor
    @SuppressLint("HardwareIds")
    @SuppressWarnings("unchecked")
    public ServiceHelper(Context context, ArrayAdapter adapter) {
        mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(context, context.getMainLooper(), null);
        //mDevices = new HashMap<>();
        mDeviceId = UUID.randomUUID().toString();
        mAdapterDevices = adapter;
    }

    // Start the service
    @SuppressWarnings("unchecked")
    public void startService(HashMap service) {
        // Service information. Pass it an instance name, service type _protocol._transport layer , and the map containing information other devices will want once they connect to this one.
        mServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(mDeviceId, "_presence._tcp", service);
        // Add the local service, sending the service info, network channel, and listener that will be used to indicate success or failure of the request.
        mManager.addLocalService(mChannel, mServiceInfo, null);
    }

    // Stop the service
    public void stopService() {
        mManager.removeLocalService(mChannel, mServiceInfo, null);
        mManager.removeServiceRequest(mChannel, mServiceRequest, null);
    }

    // Discovery services
    public void discoverService() {
        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
        mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, mServiceRequest, null);
        mManager.discoverServices(mChannel, null);
    }

    public void onFoundDevice() {

    }

}
