package fr.inria.yifan.mysensor.Context;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.RandomAccessFile;
import java.util.HashMap;

/**
 * This class provides context information about the device attributes.
 */

public class DeviceAttribute {

    private static final String TAG = "Device attributes";

    /* Power consumption constants in mA, real-world example values are attained from
     * https://android.googlesource.com/platform/frameworks/base/+/master/core/res/res/xml/power_profile.xml
     */
    //public static final float BluetoothTxPow = 10f; // Bluetooth data transfer
    //public static final float BluetoothScanPow = 0.1f; // Bluetooth scanning
    static final float WifiTxPow = 200f; // WIFI data transfer
    static final float WifiScanPow = 100f; // WIFI scanning
    //public static final float WifiIdlePow = 3f; // WIFI network on
    //public static final float AudioPow = 10f; // Audio DSP encoding
    static final float CPUPow = 100f; // CPU computing power
    static final float GPSPow = 50f; // GPS is acquiring a signal
    static final float CellTxPow = 200f; // Cellular radio is transmitting
    private static final float CellScanPow = 10f; // Cellular radio is scanning

    // Variables
    private Context mContext;
    private HashMap<String, Object> mDeviceAttr;

    // Constructor
    DeviceAttribute(Context context) {
        mContext = context;
        mDeviceAttr = new HashMap<>();
    }

    // Read device profile and put attributes into the hashmap
    public void startService() {

        SensorManager mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        // Get the sensors attributes
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) == null) {
            mDeviceAttr.put("TemperatureAcc", 0f);
            mDeviceAttr.put("TemperaturePow", 99f);
        } else {
            mDeviceAttr.put("TemperatureAcc", 10f);
            mDeviceAttr.put("TemperaturePow", mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE).getPower());
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) == null) {
            mDeviceAttr.put("LightAcc", 0f);
            mDeviceAttr.put("LightPow", 99f);
        } else {
            mDeviceAttr.put("LightAcc", 10f);
            mDeviceAttr.put("LightPow", mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT).getPower());
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) == null) {
            mDeviceAttr.put("PressureAcc", 0f);
            mDeviceAttr.put("PressurePow", 99f);
        } else {
            mDeviceAttr.put("PressureAcc", 10f);
            mDeviceAttr.put("PressurePow", mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE).getPower());
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) == null) {
            mDeviceAttr.put("HumidityAcc", 0f);
            mDeviceAttr.put("HumidityPow", 99f);
        } else {
            mDeviceAttr.put("HumidityAcc", 10f);
            mDeviceAttr.put("HumidityPow", mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY).getPower());
        }
        mDeviceAttr.put("NoiseAcc", 10f);
        mDeviceAttr.put("NoisePow", 0.1f);
    }

    // Nothing to do when stop the service
    public void stopService() {
        //Pass
    }

    // Get the most recent device attributes
    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("MissingPermission")
    HashMap getDeviceAttr() {

        // Get the current CPU frequency in MHz
        try {
            // Read from the system
            RandomAccessFile reader = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies", "r");
            float mCpuFrequency = Float.parseFloat(reader.readLine()) / 1e3f;
            //Log.e(TAG, String.valueOf(mCpuFrequency));
            mDeviceAttr.put("CPU", mCpuFrequency);
            mDeviceAttr.put("CPUPow", CPUPow);
            reader.close();
        } catch (Exception e) {
            // In case it's not accessible
            e.printStackTrace();
            mDeviceAttr.put("CPU", 1000f);
            mDeviceAttr.put("CPUPow", CPUPow);
        }

        // Get the remaining battery in mAh
        BatteryManager batteryManager = (BatteryManager) mContext.getSystemService(Context.BATTERY_SERVICE);
        //float mRemainBattery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000f;
        float mRemainBattery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER) / 1000f;
        mDeviceAttr.put("Battery", mRemainBattery);

        // Get the remaining memory size in MB
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memInfo);
        float mMemorySize = memInfo.availMem / 1e6f;
        mDeviceAttr.put("Memory", mMemorySize);

        // Get the internet connection type
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities capability = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        String mInternetType = readNetworkType(capability);
        mDeviceAttr.put("Internet", mInternetType);

        // Get yhe internet connection attributes
        switch (mInternetType) {
            case "WIFI":
                mDeviceAttr.put("InternetPower", WifiTxPow);
                break;
            case "CELLULAR":
                mDeviceAttr.put("InternetPower", CellTxPow);
                break;
            default:
                mDeviceAttr.put("Internet", "Null");
                mDeviceAttr.put("InternetPower", 999f);
                break;
        }
        mDeviceAttr.put("UpBandwidth", capability != null ? (float) capability.getLinkUpstreamBandwidthKbps() : 0f);

        // Get the location service type
        LocationManager lm = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mDeviceAttr.put("Location", "GPS");
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            mDeviceAttr.put("LocationAcc", loc != null ? loc.getAccuracy() : 1000f);
            mDeviceAttr.put("LocationPower", GPSPow);
        } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mDeviceAttr.put("Location", "NETWORK");
            Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            mDeviceAttr.put("LocationAcc", loc != null ? loc.getAccuracy() : 1000f);
            mDeviceAttr.put("LocationPower", CellScanPow);
        } else {
            mDeviceAttr.put("Location", "Null");
            mDeviceAttr.put("LocationAcc", 1000f);
            mDeviceAttr.put("LocationPower", 999f);
        }

        return mDeviceAttr;
    }

    // Set the accuracy % of a specific sensor
    public void setSensorAcc(String sensor, float accuracy) {
        mDeviceAttr.put(sensor, accuracy);
    }

    // Read the Internet type from network capability
    private String readNetworkType(NetworkCapabilities capability) {
        if (capability != null) {
            String cap = capability.toString();
            String[] pairs = cap.split(" ");
            //Log.e(TAG, pairs[2]);
            return pairs[2];
        } else {
            return "Null";
        }
    }

    // Read the Internet RSSI from network capability (ONLY works for WiFi)
    private int readNetworkRSSI(NetworkCapabilities capability) {
        String cap = capability.toString();
        return Integer.parseInt(cap.substring(cap.lastIndexOf(" ") + 1, cap.length() - 1));
    }
}
