package fr.inria.yifan.mysensor.Support;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.ArrayMap;

/**
 * This class represents the context map set of a sensing device.
 */

public class SensorContext {

    public boolean isInPocket;

    // Declare all contexts
    public boolean isInDoor;
    public float remainBattery;
    public float locationTime;
    public ArrayMap<String, Boolean> sensorArray;
    private Activity mActivity;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public SensorContext(Activity activity) {
        mActivity = activity;
        //sensorArray = new ArrayMap<>();
    }

}

