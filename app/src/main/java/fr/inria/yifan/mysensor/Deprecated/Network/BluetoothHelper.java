package fr.inria.yifan.mysensor.Deprecated.Network;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.widget.Toast;

import java.util.Map;

import static fr.inria.yifan.mysensor.Deprecated.Support.Configuration.ENABLE_REQUEST_BT;

/**
 * This class provides functions functions related to the Bluetooth service.
 */

public class BluetoothHelper extends BroadcastReceiver {

    private static final String TAG = "Bluetooth helper";

    // Declare references and variables
    private Activity mActivity;
    //private ArrayAdapter<> mAdapterBluetooth;

    // Constructor
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public BluetoothHelper(Activity activity) {
        mActivity = activity;

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mActivity.registerReceiver(this, filter);
    }

    // Set the service record information and startService thr service
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void startService(Map<String, String> record) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            // Device supports Bluetooth
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(mActivity, "Please enable the Bluetooth", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mActivity.startActivityForResult(intent, ENABLE_REQUEST_BT);
            }
        }
    }

    // Close all registered services related to Bluetooth
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void stopService() {
        mActivity.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Discovery has found a device. Get the BluetoothDevice
            // object and its info from the Intent.
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress(); // MAC address
        }
    }
}
