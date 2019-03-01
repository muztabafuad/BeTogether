package fr.inria.yifan.mysensor.Context;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.io.RandomAccessFile;
import java.util.HashMap;

/**
 * This class provides context information about the user activity.
 */

public class DeviceAttribute {

    private static final String TAG = "Device attributes";

    // Variables
    private Context mContext;

    private float mRemainBattery;
    private float mCpuFrequency;
    private float mMemorySize;
    private float mRSSIInternet;
    private HashMap<String, Integer> mSensorAcc;
    private HashMap<String, Float> mSensorPow;
    private HashMap<String, Object> mDeviceAttr;

    public DeviceAttribute(Context context) {
        mContext = context;
        mDeviceAttr = new HashMap<>();

        mSensorAcc = new HashMap<>();
        mSensorAcc.put("TemperatureAcc", 10);
        mSensorAcc.put("LightAcc", 10);
        mSensorAcc.put("PressureAcc", 10);
        mSensorAcc.put("HumidityAcc", 10);
        mSensorAcc.put("NoiseAcc", 10);
        mSensorPow = new HashMap<>();
        mSensorPow.put("TemperaturePow", 10f);
        mSensorPow.put("LightPow", 10f);
        mSensorPow.put("PressurePow", 10f);
        mSensorPow.put("HumidityPow", 10f);
        mSensorPow.put("NoisePow", 10f);
    }

    // Get the most recent device attributes
    public HashMap getDeviceAttr() {
        try {
            // Get the remaining battery
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = mContext.registerReceiver(null, filter);
            assert batteryStatus != null;
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            mRemainBattery = level / (float) scale;


            // Get the maximum CPU frequency
            RandomAccessFile reader = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r");
            mCpuFrequency = (Float.parseFloat(reader.readLine()) / 1e6f);
            reader.close();

            // Get the total memory size
            ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            assert manager != null;
            manager.getMemoryInfo(memInfo);
            mMemorySize = memInfo.availMem / 1e9f;

            mDeviceAttr.put("Battery", mRemainBattery);
            mDeviceAttr.put("CPU", mCpuFrequency);
            mDeviceAttr.put("Memory", mMemorySize);

            mDeviceAttr.putAll(mSensorAcc);
            mDeviceAttr.putAll(mSensorPow);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mDeviceAttr;
    }

    public void setSensorAcc(String sensor, int accuracy) {
        mSensorAcc.put(sensor, accuracy);
    }


}