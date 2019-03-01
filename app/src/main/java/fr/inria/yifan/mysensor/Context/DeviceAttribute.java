package fr.inria.yifan.mysensor.Context;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.RandomAccessFile;
import java.util.HashMap;

/**
 * This class provides context information about the user activity.
 */

public class DeviceAttribute {

    private static final String TAG = "Device attributes";

    // Variables
    private Context mContext;

    // Constant
    private float mCpuFrequency;
    private float mMemorySize;

    private HashMap<String, Integer> mSensorAcc;
    private HashMap<String, Float> mSensorPow;
    private HashMap<String, Object> mDeviceAttr;

    public DeviceAttribute(Context context) {
        mContext = context;
        mDeviceAttr = new HashMap<>();
        mSensorAcc = new HashMap<>();
        mSensorPow = new HashMap<>();
    }


    public void startService() {
        try {
            // Get the maximum CPU frequency GHz
            RandomAccessFile reader = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r");
            mCpuFrequency = (Float.parseFloat(reader.readLine()) / 1e6f);
            reader.close();

            // Get the total memory size GB
            ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(memInfo);
            mMemorySize = memInfo.totalMem / 1e9f;
        } catch (Exception e) {
            e.printStackTrace();
        }

        SensorManager mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        // Get the sensors attributes
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) == null) {
            mSensorAcc.put("TemperatureAcc", 0);
            mSensorAcc.put("TemperaturePow", 999);
        } else {
            mSensorAcc.put("TemperatureAcc", 10);
            mSensorPow.put("TemperaturePow", mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE).getPower());
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) == null) {
            mSensorAcc.put("LightAcc", 0);
            mSensorAcc.put("LightPow", 999);
        } else {
            mSensorAcc.put("LightAcc", 10);
            mSensorPow.put("LightPow", mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT).getPower());
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) == null) {
            mSensorAcc.put("PressureAcc", 0);
            mSensorAcc.put("PressurePow", 999);
        } else {
            mSensorAcc.put("PressureAcc", 10);
            mSensorPow.put("PressurePow", mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE).getPower());
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) == null) {
            mSensorAcc.put("HumidityAcc", 0);
            mSensorAcc.put("HumidityPow", 999);
        } else {
            mSensorAcc.put("HumidityAcc", 10);
            mSensorPow.put("HumidityPow", mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY).getPower());
        }
        mSensorAcc.put("NoiseAcc", 10);
        mSensorPow.put("NoisePow", 0.5f);
    }

    public void stopService() {
        mDeviceAttr.clear();
    }

    // Get the most recent device attributes
    @RequiresApi(api = Build.VERSION_CODES.M)
    public HashMap getDeviceAttr() {
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
            Log.d(TAG, capability.toString());
            String mInternetType = getNetworkType(capability);

            // Get the location service type
            LocationManager lm = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mDeviceAttr.put("Location", "GPS");
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mDeviceAttr.put("Location", "Network");
            } else {
                mDeviceAttr.put("Location", null);
            }

            mDeviceAttr.put("Battery", mRemainBattery);
            mDeviceAttr.put("CPU", mCpuFrequency);
            mDeviceAttr.put("Memory", mMemorySize);
            mDeviceAttr.put("Internet", mInternetType);

            mDeviceAttr.putAll(mSensorAcc);
            mDeviceAttr.putAll(mSensorPow);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mDeviceAttr;
    }

    // Set the accuracy % of a specific sensor
    public void setSensorAcc(String sensor, int accuracy) {
        mSensorAcc.put(sensor, accuracy);
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