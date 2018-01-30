package fr.inria.yifan.mysensor.Support;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides functions functions related to the Wifi Direct service..
 */

public class WifiP2PHelper {

    private static final String TAG = "Wifi Direct helper";

    // To store information from the peer
    private final HashMap<String, String> buddies = new HashMap<>();

    // Declare channel and Wifi Direct manager
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;
    private ArrayList<WifiP2pDevice> mDeviceList;

    // Constructor
    public WifiP2PHelper(Activity activity) {
        // Initialize Wifi direct components
        mManager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(activity, activity.getMainLooper(), null);
    }

    // Set the service record information
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void startService(Map<String, String> record) {
        // Service information
        WifiP2pDnsSdServiceInfo serviceInfo;
        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record);
        // Add the local service, sending the service info, network channel and listener
        mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,
            }
            @Override
            public void onFailure(int arg0) {
                // Command failed.
            }
        });
        discoverService();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void discoverService() {
        mDeviceList = new ArrayList<>();

        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            // Callback includes TXT record and device running the advertised service
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
                buddies.put(device.deviceAddress, (String) record.get("listenport"));
            }
        };
        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice resourceType) {
                // Update the device name with the version from the DnsTxtRecord
                resourceType.deviceName = buddies.containsKey(resourceType.deviceAddress) ? buddies.get(resourceType.deviceAddress) : resourceType.deviceName;
                mDeviceList.add(resourceType);
                Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
            }
        };
        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);

        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Success!
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
            }
        });

        mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Success!
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

}
