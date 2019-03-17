package fr.inria.yifan.mysensor.Context;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

/**
 * This class deals with a HashMap containing all three context information.
 * Available key-value paris are: ("UserActivity", <"VEHICLE", "BICYCLE", "FOOT", "STILL", "UNKNOWN">),
 * (<"InPocket", "InDoor", "UnderGround">, <"True", "False", null>),
 * ("Location", <"GPS", "NETWORK", null>), ("LocationAcc", <Float>), ("LocationPower", <Float>
 * ("Internet", <"WIFI", "Cellular", null>), ("UpBandwidth", <Float>), ("InternetPower", <Float>
 * ("Battery", <Float>), ("CPU", <Float>); ("Memory", <Float>),
 * (<"TemperatureAcc", "LightAcc", "PressureAcc", "HumidityAcc", "NoiseAcc">, <Float>),
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
    private DurationPredict mDurationPredict;
    private HashMap<String, String> mFeature;
    private String previousUA;
    private Calendar startTimeUA;
    private float durationUA;
    private String previousIndoor;
    private Calendar startTimeDoor;
    private float durationDoor;
    private String previousUnderground;
    private Calendar startTimeGround;
    private float durationGround;

    // Constructor initialization
    public FeatureHelper(Context context) {
        mFeature = new HashMap<>();
        mUserActivity = new UserActivity(context);
        mPhysicalEnvironment = new PhysicalEnvironment(context);
        mDeviceAttribute = new DeviceAttribute(context);
        mDurationPredict = new DurationPredict(context);
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

    // Get the most recent context hash map
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressWarnings("unchecked")
    public HashMap getContext() {
        mFeature.putAll(mUserActivity.getUserActivity());
        mFeature.putAll(mPhysicalEnvironment.getPhysicalEnv());
        mFeature.putAll(mDeviceAttribute.getDeviceAttr());

        // User activity duration update
        String currentUA = (String) mUserActivity.getUserActivity().get("UserActivity");
        if (previousUA == null) {
            startTimeUA = Calendar.getInstance();
            previousUA = currentUA;
        } else if (currentUA != null && !currentUA.equals(previousUA)) {
            Log.e(TAG, "Activity is: " + currentUA + ", time is: " + startTimeUA.getTimeInMillis());
            mDurationPredict.updateActivityModel(startTimeUA, previousUA);
            startTimeUA = Calendar.getInstance();
            previousUA = currentUA;
        }
        durationUA = mDurationPredict.predictActivityDuration(currentUA);
        mFeature.put("DurationUA", String.valueOf(durationUA));

        // In-door/Out-door duration update
        String currentDoor = (String) mPhysicalEnvironment.getPhysicalEnv().get("InDoor");
        if (previousIndoor == null) {
            startTimeDoor = Calendar.getInstance();
            previousIndoor = currentDoor;
        } else if (currentDoor != null && !currentDoor.equals(previousIndoor)) {
            Log.e(TAG, "Indoor is: " + currentDoor + ", time is: " + startTimeDoor.getTimeInMillis());
            mDurationPredict.updateDoorModel(startTimeDoor, previousIndoor);
            startTimeDoor = Calendar.getInstance();
            previousIndoor = currentDoor;
        }
        durationDoor = mDurationPredict.predictDoorDuration(currentDoor);
        mFeature.put("DurationDoor", String.valueOf(durationDoor));

        // Under-ground/On-ground duration update
        String currentGround = (String) mPhysicalEnvironment.getPhysicalEnv().get("UnderGround");
        if (previousUnderground == null) {
            startTimeGround = Calendar.getInstance();
            previousUnderground = currentGround;
        } else if (currentGround != null && !currentGround.equals(previousUnderground)) {
            Log.e(TAG, "Underground is: " + currentGround + ", time is: " + startTimeGround.getTimeInMillis());
            mDurationPredict.updateGroundModel(startTimeGround, previousUnderground);
            startTimeGround = Calendar.getInstance();
            previousUnderground = currentGround;
        }
        durationGround = mDurationPredict.predictGroundDuration(currentGround);
        mFeature.put("DurationGround", String.valueOf(durationGround));
        return mFeature;
    }

    // Clear the current context hahs map
    public void clearContext() {
        mFeature.clear();
    }

    // Check if the indicated rules is matched
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

    // Calculate the intent value to be a "Coordinator"
    @RequiresApi(api = Build.VERSION_CODES.N)
    public float getCoordinatorIntent(int[] historyNeighbors) {
        float d = sigmoidFunction(Math.min(durationUA, Math.min(durationDoor, durationGround)), 0.1f, 10f);
        float delta = sigmoidFunction(Math.max(0, historyNeighbors.length - 1), 1f, 4f);
        float h = sigmoidFunction(Arrays.stream(historyNeighbors).sum(), 1f, 3f);
        return d + delta + h;
    }

    // Calculate the intent value to be a role
    // "Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"
    @RequiresApi(api = Build.VERSION_CODES.M)
    public float getRoleIntent(String role) {
        float bat = (float) mDeviceAttribute.getDeviceAttr().get("Battery");
        float b = sigmoidFunction(bat, 0.001f, 2500f);
        switch (role) {
            case "Locator":
                float locAcc = (float) mDeviceAttribute.getDeviceAttr().get("LocationAcc");
                float locPow = (float) mDeviceAttribute.getDeviceAttr().get("LocationPower");
                float l = -sigmoidFunction(locAcc, 0.1f, 10f) - sigmoidFunction(locPow, 0.1f, 20f);
                return l + b;
            case "Proxy":
                float p;
                if (mDeviceAttribute.getDeviceAttr().get("Internet") == "Wifi") {
                    p = 1f;
                } else {
                    p = 0.6f;
                }
                float netBw = (float) mDeviceAttribute.getDeviceAttr().get("UpBandwidth");
                float netPow = (float) mDeviceAttribute.getDeviceAttr().get("InternetPower");
                float n = p * sigmoidFunction(netBw, 0.00001f, 200000f) - sigmoidFunction(netPow, 0.1f, 20f);
                return n + b;
            case "Aggregator":
                float cpu = (float) mDeviceAttribute.getDeviceAttr().get("CPU");
                float ram = (float) mDeviceAttribute.getDeviceAttr().get("Memory");
                return sigmoidFunction(cpu, 0.001f, 1000f) + sigmoidFunction(ram, 0.001f, 2000f) + b;
            case "Temperature":
                float tacc = (float) mDeviceAttribute.getDeviceAttr().get("TemperatureAcc");
                float tpow = (float) mDeviceAttribute.getDeviceAttr().get("TemperaturePow");
                return sigmoidFunction(tacc, 0.05f, 50f) - sigmoidFunction(tpow, 0.5f, 0.1f);
            case "Light":
                float lacc = (float) mDeviceAttribute.getDeviceAttr().get("LightAcc");
                float lpow = (float) mDeviceAttribute.getDeviceAttr().get("LightPow");
                return sigmoidFunction(lacc, 0.05f, 50f) - sigmoidFunction(lpow, 0.5f, 0.1f);
            case "Pressure":
                float pacc = (float) mDeviceAttribute.getDeviceAttr().get("PressureAcc");
                float ppow = (float) mDeviceAttribute.getDeviceAttr().get("Pressurepow");
                return sigmoidFunction(pacc, 0.05f, 50f) - sigmoidFunction(ppow, 0.5f, 0.1f);
            case "Humidity":
                float hacc = (float) mDeviceAttribute.getDeviceAttr().get("HumidityAcc");
                float hpow = (float) mDeviceAttribute.getDeviceAttr().get("HumidityPow");
                return sigmoidFunction(hacc, 0.05f, 50f) - sigmoidFunction(hpow, 0.5f, 0.1f);
            case "Noise":
                float nacc = (float) mDeviceAttribute.getDeviceAttr().get("NoiseAcc");
                float npow = (float) mDeviceAttribute.getDeviceAttr().get("NoisePow");
                return sigmoidFunction(nacc, 0.05f, 50f) - sigmoidFunction(npow, 0.5f, 0.1f);
        }
        return 0;
    }

    // The logistic function ranging from -1 to 1
    private float sigmoidFunction(float x, float k, float x0) {
        return (float) ((Math.exp(2 * k * (x - x0)) - 1) / (Math.exp(2 * k * (x - x0)) + 1));
    }

    // Update the learning models for physical environments
    public void updateModels() {
        mPhysicalEnvironment.updateModels();
    }

}