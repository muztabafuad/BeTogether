package fr.inria.yifan.mysensor.Support;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;

/**
 * This class represents the context map set of a sensing device.
 */

public class DeviceContext {

    private static final String TAG = "Device context";

    // Declare references
    private Activity mActivity;

    // Declare all contexts
    private int rssiDbm;
    private boolean isInPocket;
    private boolean isInDoor;
    private float hasBattery;
    private long locationTime;
    private ArrayMap<Sensor, Boolean> sensorArray;
    private TelephonyManager mTelephonyManager;

    // Declare phone state listener
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            rssiDbm = signalStrength.getGsmSignalStrength() * 2 - 113; // -> dBm
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public DeviceContext(Activity activity) {
        mActivity = activity;

        mTelephonyManager = (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);
        assert mTelephonyManager != null;

        //IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //Intent batteryStatus = mActivity.registerReceiver(null, filter);

        // Are we charging / charged?
        //assert batteryStatus != null;
        //int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        //boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
        //int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        //boolean acCharge = (chargePlug == BatteryManager.BATTERY_PLUGGED_AC);

        // Get the current battery capacity
        //int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        //int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        //float batteryPct = level / (float) scale;
    }

    // Start the context service
    public void start() {
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    // Unregister the listeners
    public void stop() {

    }
}
