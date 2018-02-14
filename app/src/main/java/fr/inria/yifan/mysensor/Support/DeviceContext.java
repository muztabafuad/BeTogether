package fr.inria.yifan.mysensor.Support;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.ArrayMap;

/**
 * This class represents the context map set of a sensing device.
 */

public class DeviceContext {

    private static final String TAG = "Device context";
    // Declare all contexts
    public boolean isInPocket;
    public boolean isInDoor;
    public float remainBattery;
    public String locationTime;
    public ArrayMap<String, Boolean> sensorArray;
    private Activity mActivity;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public DeviceContext(Activity activity) {
        mActivity = activity;
        //sensorArray = new ArrayMap<>();

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mActivity.registerReceiver(null, filter);

        // Are we charging / charged?
        assert batteryStatus != null;
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean acCharge = (chargePlug == BatteryManager.BATTERY_PLUGGED_AC);

        // Get the current battery capacity
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level / (float) scale;
    }

}

