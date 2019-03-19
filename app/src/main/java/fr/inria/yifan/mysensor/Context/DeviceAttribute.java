package fr.inria.yifan.mysensor.Context;

import android.annotation.SuppressLint;
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

    /* Power consumption constants in mA, real-world values are attained from
     * https://android.googlesource.com/platform/frameworks/base/+/master/core/res/res/xml/power_profile.xml
     */
    public static final float BluetoothTxPow = 10f; // Bluetooth data transfer
    public static final float BluetoothScanPow = 0.1f; // Bluetooth scanning
    public static final float WifiTxPow = 200f; // WIFI data transfer
    public static final float WifiScanPow = 100f; // WIFI network scanning
    public static final float AudioPow = 10f; // Audio DSP encoding
    public static final float GPSPow = 50f; // GPS is acquiring a signal
    public static final float CellTxPow = 200f; // Cellular radio is transmitting
    public static final float CellScanPow = 10f; // Cellular radio is scanning

    // Variables
    private Context mContext;
    private HashMap<String, Object> mDeviceAttr;

    // Constructor
    DeviceAttribute(Context context) {
        mContext = context;
        mDeviceAttr = new HashMap<>();
    }

    public void startService() {
        try {
            // Get the maximum CPU frequency in MHz
            RandomAccessFile reader = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r");
            float mCpuFrequency = Float.parseFloat(reader.readLine()) / 1e3f;
            reader.close();
            mDeviceAttr.put("CPU", mCpuFrequency);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get the total memory size in MB
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memInfo);
        float mMemorySize = memInfo.totalMem / 1e6f;
        mDeviceAttr.put("Memory", mMemorySize);

        SensorManager mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        // Get the sensors attributes
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) == null) {
            mDeviceAttr.put("TemperatureAcc", 0f);
            mDeviceAttr.put("TemperaturePow", 99f);
        } else {
            mDeviceAttr.put("TemperatureAcc", 50f);
            mDeviceAttr.put("TemperaturePow", mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE).getPower());
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) == null) {
            mDeviceAttr.put("LightAcc", 0f);
            mDeviceAttr.put("LightPow", 99f);
        } else {
            mDeviceAttr.put("LightAcc", 50f);
            mDeviceAttr.put("LightPow", mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT).getPower());
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) == null) {
            mDeviceAttr.put("PressureAcc", 0f);
            mDeviceAttr.put("PressurePow", 99f);
        } else {
            mDeviceAttr.put("PressureAcc", 50f);
            mDeviceAttr.put("PressurePow", mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE).getPower());
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) == null) {
            mDeviceAttr.put("HumidityAcc", 0f);
            mDeviceAttr.put("HumidityPow", 99f);
        } else {
            mDeviceAttr.put("HumidityAcc", 0f);
            mDeviceAttr.put("HumidityPow", mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY).getPower());
        }
        mDeviceAttr.put("NoiseAcc", 50f);
        mDeviceAttr.put("NoisePow", 0.1f);
    }

    public void stopService() {
        //mDeviceAttr.clear();
    }

    // Get the most recent device attributes
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    HashMap getDeviceAttr() {
        // Get the remaining battery in mAh
        BatteryManager batteryManager = (BatteryManager) mContext.getSystemService(Context.BATTERY_SERVICE);
        float mRemainBattery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000f;
        mDeviceAttr.put("Battery", mRemainBattery);

        // Get the Internet connection type
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities capability = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        String mInternetType = readNetworkType(capability);
        mDeviceAttr.put("Internet", mInternetType);

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
            mDeviceAttr.put("Location", null);
            mDeviceAttr.put("LocationAcc", 1000f);
            mDeviceAttr.put("LocationPower", 999f);
        }

        // Get yhe Internet connection attributes
        switch (mInternetType) {
            case "Wifi":
                mDeviceAttr.put("InternetPower", WifiTxPow);
                break;
            case "Cellular":
                mDeviceAttr.put("InternetPower", CellTxPow);
                break;
            default:
                mDeviceAttr.put("InternetPower", 999f);
                break;
        }
        mDeviceAttr.put("UpBandwidth", (float) capability.getLinkUpstreamBandwidthKbps());

        return mDeviceAttr;
    }

    // Set the accuracy % of a specific sensor
    public void setSensorAcc(String sensor, float accuracy) {
        mDeviceAttr.put(sensor, accuracy);
    }

    // Get the Internet type from network capability
    private String readNetworkType(NetworkCapabilities capability) {
        String cap = capability.toString();
        String[] pairs = cap.split(" ");
        return pairs[2];
    }

    // Get the Internet RSSI from network capability ONLY works for WiFi
    private int getNetworkRSSI(NetworkCapabilities capability) {
        String cap = capability.toString();
        return Integer.parseInt(cap.substring(cap.lastIndexOf(" ") + 1, cap.length() - 1));
    }
}