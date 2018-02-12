package fr.inria.yifan.mysensor.Support;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_WIFI;

/**
 * This class provides functions functions related to the Wifi Direct service..
 */

public class WifiDirectHelper {

    private static final String TAG = "Wifi Direct helper";

    // To store information from the peer
    private final HashMap<String, String> buddies = new HashMap<>();
    private Activity mActivity;
    private ArrayAdapter<WifiP2pDevice> mAdapterWifi;

    // Declare channel and Wifi Direct manager
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;

    // Constructor
    public WifiDirectHelper(Activity activity) {
        mActivity = activity;
    }

    // Add to the custom adapter defined specifically for showing wifi devices.
    public void setAdapterWifi(ArrayAdapter<WifiP2pDevice> adapter) {
        mAdapterWifi = adapter;
    }

    // Set the service record information and start thr service
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void startService(Map<String, String> record) {
        WifiManager wifi = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // Check if Wifi service on system is enabled
        assert wifi != null;
        if (wifi.isWifiEnabled()) {
            // Initialize Wifi direct components
            mManager = (WifiP2pManager) mActivity.getSystemService(Context.WIFI_P2P_SERVICE);
            Log.d(TAG, String.valueOf(mManager == null));
            mChannel = mManager.initialize(mActivity, mActivity.getMainLooper(), null);

            // Service information
            WifiP2pDnsSdServiceInfo serviceInfo;
            serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("_connect", "_presence._tcp", record);
            // Add the local service, sending the service info, network channel and listener
            mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
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
    }

    // Start to discovery neighbors for services
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void discoverService() {
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
        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice resourceType) {
                // Update the device name with the version from the DnsTxtRecord
                resourceType.deviceName = buddies.containsKey(resourceType.deviceAddress) ? buddies.get(resourceType.deviceAddress) : resourceType.deviceName;
                Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
            }
        };
        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);

        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, serviceRequest, new WifiP2pManager.ActionListener() {
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

}
