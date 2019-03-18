package fr.inria.yifan.mysensor.Context;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
 * This class provides context information about the user activity.
 */

public class DeviceAttribute {

    private static final String TAG = "Device attributes";

    private Context mContext;

    // Variables
    private HashMap<String, Object> mDeviceAttr;

    DeviceAttribute(Context context) {
        mContext = context;
        mDeviceAttr = new HashMap<>();
    }

    public void startService() {
        try {
            // Get the maximum CPU frequency MHz
            RandomAccessFile reader = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r");
            // Constant
            float mCpuFrequency = (Float.parseFloat(reader.readLine()) / 1e3f);
            reader.close();

            // Get the total memory size MB
            ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(memInfo);
            float mMemorySize = memInfo.totalMem / 1e6f;

            mDeviceAttr.put("CPU", mCpuFrequency);
            mDeviceAttr.put("Memory", mMemorySize);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SensorManager mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        // Get the sensors attributes
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) == null) {
            mDeviceAttr.put("TemperatureAcc", 0f);
            mDeviceAttr.put("TemperaturePow", 999f);
        } else {
            mDeviceAttr.put("TemperatureAcc", 50f);
            mDeviceAttr.put("TemperaturePow", mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE).getPower());
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) == null) {
            mDeviceAttr.put("LightAcc", 0f);
            mDeviceAttr.put("LightPow", 999f);
        } else {
            mDeviceAttr.put("LightAcc", 50f);
            mDeviceAttr.put("LightPow", mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT).getPower());
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) == null) {
            mDeviceAttr.put("PressureAcc", 0f);
            mDeviceAttr.put("PressurePow", 999f);
        } else {
            mDeviceAttr.put("PressureAcc", 50f);
            mDeviceAttr.put("PressurePow", mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE).getPower());
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) == null) {
            mDeviceAttr.put("HumidityAcc", 0f);
            mDeviceAttr.put("HumidityPow", 999f);
        } else {
            mDeviceAttr.put("HumidityAcc", 0f);
            mDeviceAttr.put("HumidityPow", mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY).getPower());
        }
        mDeviceAttr.put("NoiseAcc", 50f);
        mDeviceAttr.put("NoisePow", 0.5f);
    }

    public void stopService() {
        mDeviceAttr.clear();
    }

    // Get the most recent device attributes
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    HashMap getDeviceAttr() {
        try {
            // Get the remaining battery %
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = mContext.registerReceiver(null, filter);
            assert batteryStatus != null;
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float mRemainBattery = level / (float) scale * 100;

            // Get the Internet connection type
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities capability = cm.getNetworkCapabilities(cm.getActiveNetwork());
            //Log.d(TAG, capability.toString());
            String mInternetType = getNetworkType(capability);
            mDeviceAttr.put("Internet", mInternetType);
            switch (mInternetType) {
                case "Wifi":
                    mDeviceAttr.put("InternetPower", 20f);
                    break;
                case "Cellular":
                    mDeviceAttr.put("InternetPower", 40f);
                    break;
                default:
                    mDeviceAttr.put("InternetPower", 999f);
                    break;
            }
            mDeviceAttr.put("UpBandwidth", (float) capability.getLinkUpstreamBandwidthKbps());

            // Get the location service type
            LocationManager lm = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mDeviceAttr.put("Location", "GPS");
                Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                mDeviceAttr.put("LocationAcc", loc != null ? loc.getAccuracy() : 0f);
                mDeviceAttr.put("LocationPower", 50f);
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mDeviceAttr.put("Location", "NETWORK");
                Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                mDeviceAttr.put("LocationAcc", loc != null ? loc.getAccuracy() : 0f);
                mDeviceAttr.put("LocationPower", 20f);
            } else {
                mDeviceAttr.put("Location", null);
                mDeviceAttr.put("LocationAcc", 0f);
                mDeviceAttr.put("LocationPower", 999f);
            }
            mDeviceAttr.put("Battery", mRemainBattery);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return mDeviceAttr;
    }

    // Set the accuracy % of a specific sensor
    public void setSensorAcc(String sensor, float accuracy) {
        mDeviceAttr.put(sensor, accuracy);
    }

    // Get the Internet type from network capability
    private String getNetworkType(NetworkCapabilities capability) {
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