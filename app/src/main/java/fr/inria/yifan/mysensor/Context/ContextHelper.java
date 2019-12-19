package fr.inria.yifan.mysensor.Context;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

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
 * This class provides a HashMap containing all three context information: User Activity, Physical Environment and Device Attributes.
 * Available key-value paris are:
 * ("UserActivity", <"VEHICLE", "BICYCLE", "FOOT", "STILL", "UNKNOWN">), ("DurationUA", <Float>)
 * (<"InPocket", "InDoor", "UnderGround">, <"True", "False", "Null">), ("DurationDoor", <Float>), ("DurationGround", <Float>)
 * ("Location", <"GPS", "NETWORK", "Null">), ("LocationAcc", <Float>), ("LocationPower", <Float>), ("Bearing", <Float>)
 * ("Internet", <"WIFI", "Cellular", "Null">), ("UpBandwidth", <Float>), ("InternetPower", <Float>)
 * ("Battery", <Float>), ("CPU", <Float>), ("CPUPow", <Float>), ("Memory", <Float>),
 * (<"TemperatureAcc", "LightAcc", "PressureAcc", "HumidityAcc", "NoiseAcc">, <Float>),
 * (<"TemperaturePow", "LightPow", "PressurePow", "HumidityPow", "NoisePow">, <Float>)
 */

public class ContextHelper {

    // Time interval between activity and GPS updates in milliseconds
    static final int MIN_UPDATE_TIME = 1000;

    // The lambda parameter for learning model update
    static final int LAMBDA = 10;

    private static final String TAG = "Context helper";

    // Variables
    private UserActivity mUserActivity;
    private PhysicalEnvironment mPhysicalEnvironment;
    private DurationPredict mDurationPredict;
    private DeviceAttribute mDeviceAttribute;

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
    public ContextHelper(Context context) {
        mContext = new HashMap<>();
        mIntents = new HashMap<>();
        mUserActivity = new UserActivity(context);
        mPhysicalEnvironment = new PhysicalEnvironment(context);
        mDurationPredict = new DurationPredict(context);
        mDeviceAttribute = new DeviceAttribute(context);
    }

    // Start the service
    public void startService() {
        try {
            mUserActivity.startService();
            mPhysicalEnvironment.startService();
            mDeviceAttribute.startService();
        } catch (Exception e) {
            Log.e(TAG, String.valueOf(e));
        }
    }

    // Stop the service
    public void stopService() {
        try {
            mUserActivity.stopService();
            mPhysicalEnvironment.stopService();
            mDeviceAttribute.stopService();
        } catch (Exception e) {
            Log.e(TAG, String.valueOf(e));
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

        // Read the duration prediction and update the models
        readUpdateDUModels();

        // Return the contexts result
        return mContext;
    }

    // Clear the current context HashMap
    public void clearContext() {
        mContext.clear();
    }

    // Check if the given context rules are matched
    @RequiresApi(api = Build.VERSION_CODES.M)
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
    // Input is the historical connection time of neighbors
    @RequiresApi(api = Build.VERSION_CODES.M)
    public HashMap getIntentValues(int[] historyNeighbors) {

        // Battery metric
        float bat = (float) mDeviceAttribute.getDeviceAttr().get("Battery");
        float b = sigmoidFunction(bat, 0.001f, 1000f);

        // Test
        //mIntents.put("Battery", String.valueOf(bat));

        // Coordinator utility
        float delta = sigmoidFunction(Math.max(0, historyNeighbors.length - 4), 1f, 1f);
        float d = sigmoidFunction(Math.min(durationUA, Math.min(durationDoor, durationGround)), 0.1f, 10f);
        float sum = 0;
        for (int i : historyNeighbors) sum += i;
        float h = historyNeighbors.length != 0 ? sigmoidFunction(sum / historyNeighbors.length, 1f, 1f) : 0;
        mIntents.put("Coordinator", String.valueOf(d + delta + h + b));

        // Test
        //mIntents.put("Duration", String.valueOf(Math.min(durationUA, Math.min(durationDoor, durationGround))));
        //mIntents.put("Neighbors", String.valueOf(historyNeighbors.length));
        //mIntents.put("History", String.valueOf(historyNeighbors.length != 0 ? sum / historyNeighbors.length : 0));

        // Locator utility
        float locAcc = (float) mDeviceAttribute.getDeviceAttr().get("LocationAcc");
        float locPow = (float) mDeviceAttribute.getDeviceAttr().get("LocationPower");
        float l = -sigmoidFunction(locAcc, 0.1f, 10f) - sigmoidFunction(locPow, 0.1f, 30f);
        mIntents.put("Locator", String.valueOf(l + b));

        // Test
        //mIntents.put("LocAccuracy", String.valueOf(locAcc));

        // Proxy utility
        //float p = mDeviceAttribute.getDeviceAttr().get("Internet") == "Wifi" ? 1f : 0.6f;
        float netBw = (float) mDeviceAttribute.getDeviceAttr().get("UpBandwidth");
        float netPow = (float) mDeviceAttribute.getDeviceAttr().get("InternetPower");
        float n = sigmoidFunction(netBw, 0.00001f, 100000f) - sigmoidFunction(netPow, 0.01f, 100f);
        mIntents.put("Proxy", String.valueOf(n + b));

        // Test
        //mIntents.put("Bandwidth", String.valueOf(netBw));
        //mIntents.put("NetPower", String.valueOf(netPow));

        // Aggregator utility
        float cpu = (float) mDeviceAttribute.getDeviceAttr().get("CPU");
        float ram = (float) mDeviceAttribute.getDeviceAttr().get("Memory");
        float cpow = (float) mDeviceAttribute.getDeviceAttr().get("CPUPow");
        float cp = sigmoidFunction(cpu, 0.001f, 1000f) - sigmoidFunction(cpow, 0.01f, 100f);
        mIntents.put("Aggregator", String.valueOf(cp + sigmoidFunction(ram, 0.001f, 1000f)));

        // Test
        //mIntents.put("Memory", String.valueOf(ram));

        // Temperature utility
        float tacc = (float) mDeviceAttribute.getDeviceAttr().get("TemperatureAcc");
        float tpow = (float) mDeviceAttribute.getDeviceAttr().get("TemperaturePow");
        mIntents.put("Temperature", String.valueOf(sigmoidFunction(tacc, 0.1f, 10f) - sigmoidFunction(tpow, 1f, 0.1f)));

        // Light utility
        float lacc = (float) mDeviceAttribute.getDeviceAttr().get("LightAcc");
        float lpow = (float) mDeviceAttribute.getDeviceAttr().get("LightPow");
        mIntents.put("Light", String.valueOf(sigmoidFunction(lacc, 0.1f, 10f) - sigmoidFunction(lpow, 1f, 0.1f)));

        // Pressure utility
        float pacc = (float) mDeviceAttribute.getDeviceAttr().get("PressureAcc");
        float ppow = (float) mDeviceAttribute.getDeviceAttr().get("PressurePow");
        mIntents.put("Pressure", String.valueOf(sigmoidFunction(pacc, 0.1f, 10f) - sigmoidFunction(ppow, 1f, 0.1f)));

        // Humidity utility
        float hacc = (float) mDeviceAttribute.getDeviceAttr().get("HumidityAcc");
        float hpow = (float) mDeviceAttribute.getDeviceAttr().get("HumidityPow");
        mIntents.put("Humidity", String.valueOf(sigmoidFunction(hacc, 0.1f, 10f) - sigmoidFunction(hpow, 1f, 0.1f)));

        // Noise utility
        float nacc = (float) mDeviceAttribute.getDeviceAttr().get("NoiseAcc");
        float npow = (float) mDeviceAttribute.getDeviceAttr().get("NoisePow");
        mIntents.put("Noise", String.valueOf(sigmoidFunction(nacc, 0.1f, 10f) - sigmoidFunction(npow, 1f, 0.1f)));

        // Return the intents result
        return mIntents;
    }

    // Calculate and get the power consumption for a given role in one time scycle:
    // "Coordinator" "Locator", "Proxy", "Aggregator", "Temperature", "Light", "Pressure", "Humidity", "Noise"
    // Active time: how much time the components are active, in seconds
    // Individual: work by itself or using a neighboring collaboration
    @RequiresApi(api = Build.VERSION_CODES.M)
    private float getPowerOneRole(String service, int activeTime, boolean individual) {
        // Two working mode for each role
        float individualPow;
        float collaboratePow;
        switch (service) {
            case "Coordinator":
                individualPow = 0; // Self as coordinator, no additional power
                collaboratePow = WifiScanPow + WifiTxPow; // 1 second discovery + 1 second Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Locator":
                individualPow = GPSPow * activeTime; // Self as locator, need GPS power
                collaboratePow = individualPow + WifiTxPow; // + 1 second Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Proxy":
                individualPow = CellTxPow; // Self as proxy, 1 second cell transmission
                collaboratePow = individualPow + WifiTxPow; // + 1 second Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Aggregator":
                individualPow = CPUPow; // Self as aggregator, 1 second CPU power
                collaboratePow = individualPow + WifiTxPow; // + 1 second Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Temperature":
                individualPow = (float) mDeviceAttribute.getDeviceAttr().get("TemperaturePow") * activeTime; // Self as sensor
                collaboratePow = individualPow + WifiTxPow; // + 1 second Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Light":
                individualPow = (float) mDeviceAttribute.getDeviceAttr().get("LightPow") * activeTime; // Self as sensor
                collaboratePow = individualPow + WifiTxPow; // + 1 second Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Pressure":
                individualPow = (float) mDeviceAttribute.getDeviceAttr().get("PressurePow") * activeTime; // Self as sensor
                collaboratePow = individualPow + WifiTxPow; // + 1 second Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Humidity":
                individualPow = (float) mDeviceAttribute.getDeviceAttr().get("HumidityPow") * activeTime; // Self as sensor
                collaboratePow = individualPow + WifiTxPow; // + 1 second Wifi transmission
                return individual ? individualPow : collaboratePow;

            case "Noise":
                individualPow = (float) mDeviceAttribute.getDeviceAttr().get("NoisePow") * activeTime; // Self as sensor
                collaboratePow = individualPow + WifiTxPow; // + 1 second Wifi transmission
                return individual ? individualPow : collaboratePow;
        }
        //Log.e(TAG, "Wrong service is given!");
        return 0f;
    }

    // Calculate and get the total power consumption for several roles in one time cycle:
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
        return (float) (1 / (1 + Math.exp(k * (x0 - x))));
    }

    // Update the learning models for physical environments
    public void updatePEModels() {
        mPhysicalEnvironment.updateAllModels();
    }

    // Read prediction and update the learning models for duration prediction
    private void readUpdateDUModels() {
        // User Activity duration update automatically
        String currentUA = (String) mUserActivity.getUserActivity().get("UserActivity");
        assert currentUA != null;
        if (previousUA == null) {
            // First record
            startTimeUA = Calendar.getInstance();
            previousUA = currentUA;
            // UA has changed and it's not unknown
        } else if (!currentUA.equals("UNKNOWN") && !currentUA.equals(previousUA)) {
            //Log.d(TAG, "Activity is: " + currentUA + ", time is: " + startTimeUA.getTimeInMillis());
            mDurationPredict.updateActivityModel(startTimeUA, previousUA);
            mDurationPredict.saveModels();
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
            // Indoor state has changed and it's not null
            //Log.d(TAG, "Indoor is: " + currentDoor + ", time is: " + startTimeDoor.getTimeInMillis());
            mDurationPredict.updateDoorModel(startTimeDoor, previousIndoor);
            mDurationPredict.saveModels();
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
            // Underground state has changed and it's not null
            //Log.d(TAG, "Underground is: " + currentGround + ", time is: " + startTimeGround.getTimeInMillis());
            mDurationPredict.updateGroundModel(startTimeGround, previousUnderground);
            mDurationPredict.saveModels();
            startTimeGround = Calendar.getInstance();
            previousUnderground = currentGround;
        }
        durationGround = mDurationPredict.predictGroundDuration(currentGround);
        mContext.put("DurationGround", String.valueOf(durationGround));
    }

}
