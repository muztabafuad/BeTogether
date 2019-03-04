package fr.inria.yifan.mysensor.Context;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.HashMap;
import java.util.Objects;

/**
 * This class deals with a HashMap containing all three context information.
 * Available key-value paris are: ("UserActivity", <"VEHICLE", "BICYCLE", "FOOT", "STILL", "UNKNOWN">),
 * (<"InPocket", "InDoor", "UnderGround">, <"True", "False", null>),
 * ("Location", <"GPS", "NETWORK", null>), ("Internet", <"WIFI", "Cellular", null>), ("Battery", <Float>),
 * ("CPU", <Float>); ("Memory", <Float>), (<"TemperatureAcc", "LightAcc", "PressureAcc", "HumidityAcc", "NoiseAcc">, <Float>),
 * (<"TemperaturePow", "LightPow", "PressurePow", "HumidityPow", "NoisePow">, <Float>)
 */

public class FeatureHelper {

    // Time interval between activity and GPS updates (milliseconds)
    static final int MIN_UPDATE_TIME = 500;

    // The lambda parameter for learning model update
    static final int LAMBDA = 10;

    private static final String TAG = "Feature helper";

    private UserActivity mUserActivity;
    private PhysicalEnvironment mPhysicalEnvironment;
    private DeviceAttribute mDeviceAttribute;

    private HashMap<String, String> mFeature;

    public FeatureHelper(Context context) {
        mFeature = new HashMap<>();
        mUserActivity = new UserActivity(context);
        mPhysicalEnvironment = new PhysicalEnvironment(context);
        mDeviceAttribute = new DeviceAttribute(context);
    }

    public void startService() {
        mUserActivity.startService();
        mPhysicalEnvironment.startService();
        mDeviceAttribute.startService();
    }

    public void stopService() {
        mUserActivity.stopService();
        mPhysicalEnvironment.stopService();
        mDeviceAttribute.stopService();
    }

    @SuppressWarnings("unchecked")
    @RequiresApi(api = Build.VERSION_CODES.M)
    public HashMap getContext() {
        mFeature.putAll(mUserActivity.getUserActivity());
        mFeature.putAll(mPhysicalEnvironment.getPhysicalEnv());
        mFeature.putAll(mDeviceAttribute.getDeviceAttr());
        return mFeature;
    }

    public void clearContext() {
        mFeature.clear();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean matchRules(HashMap<String, String> rules) {
        for (String key : rules.keySet()) {
            //Log.e(TAG, key);
            if (!Objects.equals(mFeature.get(key), rules.get(key))) {
                return false;
            }
        }
        return true;
    }

    public int getIntentValue() {
        return 0;
    }

    public void updateModels() {
        mPhysicalEnvironment.updateModels();
    }

}