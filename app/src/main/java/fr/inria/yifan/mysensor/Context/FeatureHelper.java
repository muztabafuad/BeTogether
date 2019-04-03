package fr.inria.yifan.mysensor.Context;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static fr.inria.yifan.mysensor.Context.DeviceAttribute.CPUPow;
import static fr.inria.yifan.mysensor.Context.DeviceAttribute.CellTxPow;
import static fr.inria.yifan.mysensor.Context.DeviceAttribute.GPSPow;
import static fr.inria.yifan.mysensor.Context.DeviceAttribute.WifiScanPow;
import static fr.inria.yifan.mysensor.Context.DeviceAttribute.WifiTxPow;

/**
 * This class provides a HashMap containing all three context information: UA, PE and DA.
 * Available key-value paris are:
 * ("UserActivity", <"VEHICLE", "BICYCLE", "FOOT", "STILL", "UNKNOWN">), ("DurationUA", <Float>)
 * (<"InPocket", "InDoor", "UnderGround">, <"True", "False", null>), ("DurationDoor", <Float>), ("DurationGround", <Float>)
 * ("Location", <"GPS", "NETWORK", null>), ("LocationAcc", <Float>), ("LocationPower", <Float>)
 * ("Internet", <"WIFI", "Cellular", null>), ("UpBandwidth", <Float>), ("InternetPower", <Float>)
 * ("Battery", <Float>), ("CPU", <Float>), ("CPUPow", <Float>), ("Memory", <Float>),
 * (<"TemperatureAcc", "LightAcc", "PressureAcc", "HumidityAcc", "NoiseAcc">, <Float>),
 * (<"TemperaturePow", "LightPow", "PressurePow", "HumidityPow", "NoisePow">, <Float>)
 */

public class FeatureHelper {

    // Time interval between activity and GPS updates in milliseconds
    static final int MIN_UPDATE_TIME = 500;

    // The lambda parameter for learning model update
    static final int LAMBDA = 10;

    private static final String TAG = "Feature helper";

    // Variables
    private UserActivity mUserActivity;
    private PhysicalEnvironment mPhysicalEnvironment;
    private DeviceAttribute mDeviceAttribute;
    private DurationPredict mDurationPredict;
    private HashMap<String, String> mContext;
    private HashMap<String, String> mIntents;

    // Variables used for duration prediction update
    private String previousUA;
    private Calendar startTimeUA;
    private float durationUA;
    private String previousIndoor;
    private Calendar startTimeDoor;
    private float durationDoor;
    private String previousUnderground;
    private Calendar startTimeGround;
    private float durationGround;

    // Constructor
    public FeatureHelper(Context context) {
        mContext = new HashMap<>();
        mIntents = new HashMap<>();
        mUserActivity = new UserActivity(context);
        mPhysicalEnvironment = new PhysicalEnvironment(context);
        mDeviceAttribute = new DeviceAttribute(context);
        mDurationPredict = new DurationPredict(context);
    }

    public void startService() {
        try {
            mUserActivity.startService();
            mPhysicalEnvironment.startService();
            mDeviceAttribute.startService();
        } catch (Exception e) {
            // Pass
        }
    }

    public void stopService() {
        try {
            mUserActivity.stopService();
            mPhysicalEnvironment.stopService();
            mDeviceAttribute.stopService();
        } catch (Exception e) {
            // Pass
        }
    }

    // Get the most recent context hash map
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressWarnings("unchecked")
    public HashMap getContext() {
        // Read all three contexts into the HashMap
        mContext.putAll(mUserActivity.getUserActivity());
        mContext.putAll(mPhysicalEnvironment.getPhysicalEnv());
        mContext.putAll(mDeviceAttribute.getDeviceAttr());

        // User Activity duration update automatically
        String currentUA = (String) mUserActivity.getUserActivity().get("UserActivity");
        assert currentUA != null;
        if (previousUA == null) {
            // First record
            startTimeUA = Calendar.getInstance();
            previousUA = currentUA;
        } else if (!currentUA.equals("Null") && !currentUA.equals(previousUA)) {
            // User activity changed
            //Log.d(TAG, "Activity is: " + currentUA + ", time is: " + startTimeUA.getTimeInMillis());
            mDurationPredict.updateActivityModel(startTimeUA, previousUA);
            startTimeUA = Calendar.getInstance();
            previousUA = currentUA;
        }
        durationUA = mDurationPredict.predictActivityDuration(currentUA);
        mContext.put("DurationUA", String.valueOf(durationUA));

        // In-door/Out-door duration update automatically
        String currentDoor = (String) mPhysicalEnvironment.getPhysicalEnv().get("InDoor");
        assert currentDoor != null;
        if (previousIndoor == null) {
            // First record
            startTimeDoor = Calendar.getInstance();
            previousIndoor = currentDoor;
        } else if (!currentDoor.equals("Null") && !currentDoor.equals(previousIndoor)) {
            // Indoor state changed
            //Log.d(TAG, "Indoor is: " + currentDoor + ", time is: " + startTimeDoor.getTimeInMillis());
            mDurationPredict.updateDoorModel(startTimeDoor, previousIndoor);
            startTimeDoor = Calendar.getInstance();
            previousIndoor = currentDoor;
        }
        durationDoor = mDurationPredict.predictDoorDuration(currentDoor);
        mContext.put("DurationDoor", String.valueOf(durationDoor));

        // Under-ground/On-ground duration update automatically
        String currentGround = (String) mPhysicalEnvironment.getPhysicalEnv().get("UnderGround");
        assert currentGround != null;
        if (previousUnderground == null) {
            // First record
            startTimeGround = Calendar.getInstance();
            previousUnderground = currentGround;
        } else if (!currentGround.equals("Null") && !currentGround.equals(previousUnderground)) {
            // Underground state changed
            //Log.d(TAG, "Underground is: " + currentGround + ", time is: " + startTimeGround.getTimeInMillis());
            mDurationPredict.updateGroundModel(startTimeGround, previousUnderground);
            startTimeGround = Calendar.getInstance();
            previousUnderground = currentGround;
        }
        durationGround = mDurationPredict.predictGroundDuration(currentGround);
        mContext.put("DurationGround", String.valueOf(durationGround));
        // Return the contexts result
        return mContext;
    }

    // Clear the current context HashMap
    public void clearContext() {
        mContext.clear();
    }

    // Check if the given context rules are matched
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean matchRules(HashMap<String, String> rules) {
        for (String key : rules.keySet()) {
            //Log.e(TAG, key);
            if (!Objects.equals(mContext.get(key), rules.get(key))) {
                return false;
            }
        }
        return true;
    }

    // Calculate and get the intent values for all roles:
    // "Coordinator" "Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"
    @RequiresApi(api = Build.VERSION_CODES.M)
    public HashMap getIntentValues(int[] historyNeighbors) {

        // Battery metric
        float bat = (float) mDeviceAttribute.getDeviceAttr().get("Battery");
        float b = sigmoidFunction(bat, 0.001f, 1000f);

        // Coordinator
        float d = sigmoidFunction(Math.min(durationUA, Math.min(durationDoor, durationGround)), 0.1f, 10f);
        float delta = sigmoidFunction(Math.max(0, historyNeighbors.length - 1), 1f, 3f);
        int sum = 0;
        for (int i : historyNeighbors) sum += i;
        float h = sigmoidFunction(sum, 1f, 3f);
        mIntents.put("Coordinator", String.valueOf(d + delta + h + b));

        // Locator
        float locAcc = (float) mDeviceAttribute.getDeviceAttr().get("LocationAcc");
        float locPow = (float) mDeviceAttribute.getDeviceAttr().get("LocationPower");
        float l = -sigmoidFunction(locAcc, 0.1f, 10f) - sigmoidFunction(locPow, 0.1f, 30f);
        mIntents.put("Locator", String.valueOf(l + b));

        // Proxy
        float p = mDeviceAttribute.getDeviceAttr().get("Internet") == "Wifi" ? 1f : 0.6f;
        float netBw = (float) mDeviceAttribute.getDeviceAttr().get("UpBandwidth");
        float netPow = (float) mDeviceAttribute.getDeviceAttr().get("InternetPower");
        float n = p * sigmoidFunction(netBw, 0.00001f, 200000f) - sigmoidFunction(netPow, 0.05f, 100f);
        mIntents.put("Proxy", String.valueOf(n + b));

        // Aggregator
        float cpu = (float) mDeviceAttribute.getDeviceAttr().get("CPU");
        float ram = (float) mDeviceAttribute.getDeviceAttr().get("Memory");
        float cpow = (float) mDeviceAttribute.getDeviceAttr().get("CPUPow");
        float cp = sigmoidFunction(cpu, 0.001f, 1000f) - sigmoidFunction(cpow, 0.01f, 100f);
        mIntents.put("Aggregator", String.valueOf(cp + sigmoidFunction(ram, 0.001f, 2000f)));

        // Temperature
        float tacc = (float) mDeviceAttribute.getDeviceAttr().get("TemperatureAcc");
        float tpow = (float) mDeviceAttribute.getDeviceAttr().get("TemperaturePow");
        mIntents.put("Temperature", String.valueOf(sigmoidFunction(tacc, 0.05f, 50f) - sigmoidFunction(tpow, 1f, 0.1f)));

        // Light
        float lacc = (float) mDeviceAttribute.getDeviceAttr().get("LightAcc");
        float lpow = (float) mDeviceAttribute.getDeviceAttr().get("LightPow");
        mIntents.put("Light", String.valueOf(sigmoidFunction(lacc, 0.05f, 50f) - sigmoidFunction(lpow, 1f, 0.1f)));

        // Pressure
        float pacc = (float) mDeviceAttribute.getDeviceAttr().get("PressureAcc");
        float ppow = (float) mDeviceAttribute.getDeviceAttr().get("PressurePow");
        mIntents.put("Pressure", String.valueOf(sigmoidFunction(pacc, 0.05f, 50f) - sigmoidFunction(ppow, 1f, 0.1f)));

        // Humidity
        float hacc = (float) mDeviceAttribute.getDeviceAttr().get("HumidityAcc");
        float hpow = (float) mDeviceAttribute.getDeviceAttr().get("HumidityPow");
        mIntents.put("Humidity", String.valueOf(sigmoidFunction(hacc, 0.05f, 50f) - sigmoidFunction(hpow, 1f, 0.1f)));

        // Noise
        float nacc = (float) mDeviceAttribute.getDeviceAttr().get("NoiseAcc");
        float npow = (float) mDeviceAttribute.getDeviceAttr().get("NoisePow");
        mIntents.put("Noise", String.valueOf(sigmoidFunction(nacc, 0.05f, 50f) - sigmoidFunction(npow, 1f, 0.1f)));

        // Return the intents result
        return mIntents;
    }

    // Calculate and get the power consumption for a given role in one time slot:
    // "Coordinator" "Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"
    // Active time: how much time the components are active, in seconds
    // Individual: work by itself or using a neighboring collaboration
    @RequiresApi(api = Build.VERSION_CODES.M)
    private float getPowerOneRole(String service, int activeTime, boolean individual) {
        // Two power mode for each role
        float individualPow;
        float collaboratePow;
        switch (service) {
            case "Coordinator":
                individualPow = 0; // Self as coordinator, no additional power
                collaboratePow = WifiScanPow + WifiTxPow; // 1 discovery + 1 Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Locator":
                individualPow = GPSPow * activeTime; // Self as locator, GPS power
                collaboratePow = individualPow + WifiTxPow; // + 1 Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Proxy":
                individualPow = CellTxPow; // Self as proxy, Cell transmission
                collaboratePow = individualPow + WifiTxPow; // + 1 Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Aggregator":
                individualPow = CPUPow; // Self as aggregator, CPU power
                collaboratePow = individualPow + WifiTxPow; // + 1 Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Temperature":
                individualPow = (float) mDeviceAttribute.getDeviceAttr().get("TemperaturePow") * activeTime; // Self as sensor
                collaboratePow = individualPow + WifiTxPow; // + 1 Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Light":
                individualPow = (float) mDeviceAttribute.getDeviceAttr().get("LightPow") * activeTime; // Self as sensor
                collaboratePow = individualPow + WifiTxPow; // + 1 Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Pressure":
                individualPow = (float) mDeviceAttribute.getDeviceAttr().get("PressurePow") * activeTime; // Self as sensor
                collaboratePow = individualPow + WifiTxPow; // + 1 Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Humidity":
                individualPow = (float) mDeviceAttribute.getDeviceAttr().get("HumidityPow") * activeTime; // Self as sensor
                collaboratePow = individualPow + WifiTxPow; // + 1 Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Noise":
                individualPow = (float) mDeviceAttribute.getDeviceAttr().get("NoisePow") * activeTime; // Self as sensor
                collaboratePow = individualPow + WifiTxPow; // + 1 Wifi transmission
                return individual ? individualPow : collaboratePow;
        }
        //Log.e(TAG, "Wrong service is given!");
        return 0f;
    }

    // Calculate and get the total power consumption for several roles in one time slot:
    // Roles: set of roles applied
    // Active time: how much time the components are active, in seconds
    // Individual: work by itself or using a neighboring collaboration
    @RequiresApi(api = Build.VERSION_CODES.M)
    public float getPowerTotal(List<String> roles, int activeTime, boolean individual) {
        float sum = 0;
        if (roles != null) {
            for (String role : roles) {
                sum += getPowerOneRole(role, activeTime, individual);
            }
        }
        return sum;
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