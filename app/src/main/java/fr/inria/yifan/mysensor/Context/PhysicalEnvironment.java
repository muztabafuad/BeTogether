package fr.inria.yifan.mysensor.Context;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import weka.classifiers.trees.HoeffdingTree;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * This class provides context information about physical environments.
 */

public class PhysicalEnvironment {

    private static final String TAG = "Physical environment";

    // Load the learning model file from this path
    private static final String MODEL_POCKET = "Classifier_pocket.model";
    private static final String MODEL_DOOR = "Classifier_door.model";
    private static final String MODEL_GROUND = "Classifier_ground.model";
    private static final String DATASET_POCKET = "Dataset_pocket.model";
    private static final String DATASET_DOOR = "Dataset_door.model";
    private static final String DATASET_GROUND = "Dataset_ground.model";

    private HashMap<String, String> mPhysicalEnv;

    // Instances for data set initialization
    private Instances instancesPocket;
    private Instances instancesDoor;
    private Instances instancesGround;
    // H.Tree for classifiers initialization
    private HoeffdingTree classifierPocket;
    private HoeffdingTree classifierDoor;
    private HoeffdingTree classifierGround;

    private SensorManager mSensorManager;
    private TelephonyManager mTelephonyManager;
    private LocationManager mLocationManager;
    private WifiManager mWifiManager;

    private float mLight;
    private float mRssiLevel;
    private float mRssiValue;
    private float mAccuracy;
    private float mWifiRssi;
    private float mProximity;
    private float mTemperature;
    private float mPressure;
    private float mHumidity;

    private SensorEventListener mListenerLight = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mLight = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private PhoneStateListener mListenerPhone = new PhoneStateListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mRssiLevel = signalStrength.getLevel();
            mRssiValue = signalStrength.getGsmSignalStrength() * 2 - 113; // -> dBm
        }
    };

    private LocationListener mListenerLoc = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mAccuracy = location.getAccuracy();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private SensorEventListener mListenerProxy = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mProximity = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    private SensorEventListener mListenerTemp = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mTemperature = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    private SensorEventListener mListenerPress = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mPressure = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    private SensorEventListener mListenerHumid = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mHumidity = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    public PhysicalEnvironment(Context context) {
        loadModels(context);
        mLight = 0;
        mRssiLevel = 0;
        mRssiValue = 0;
        mAccuracy = 0;
        mWifiRssi = 0;
        mProximity = 0;
        mTemperature = 0;
        mPressure = 0;
        mHumidity = 0;

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mPhysicalEnv = new HashMap<>();
        mPhysicalEnv.put("InPocket", null);
        mPhysicalEnv.put("InDoor", null);
        mPhysicalEnv.put("UnderGround", null);
    }

    @SuppressLint("MissingPermission")
    public void startService() {
        // Register listeners for all environmental sensors
        mSensorManager.registerListener(mListenerLight, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_FASTEST);
        mTelephonyManager.listen(mListenerPhone, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, mListenerLoc);
        mSensorManager.registerListener(mListenerProxy, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mListenerTemp, mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE), SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mListenerPress, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mListenerHumid, mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY), SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stopService() {

    }

    public HashMap getPhysicalEnv() {
        // Wifi RSSI has no callback listener
        if (mWifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                mWifiRssi = wifiInfo.getRssi();
            }
        }
        return mPhysicalEnv;
    }
    //Log.d(TAG, Arrays.toString(mSample));


    // Inference on the new instance
    public boolean inferPocket(double[] sample) throws Exception {
        // 10 Proximity, 12 temperature, 2 light density
        double[] entry = new double[]{sample[10], sample[12], sample[2]};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesPocket);
        return classifierPocket.classifyInstance(inst) == 1;
    }

    // Inference on the new instance
    public boolean inferDoor(double[] sample) throws Exception {
        // 7 GPS accuracy, 5 RSSI level, 6 RSSI value, 9 Wifi RSSI, 2 light density, 12 temperature
        double[] entry = new double[]{sample[7], sample[5], sample[6], sample[9], sample[2], sample[12]};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesDoor);
        return classifierDoor.classifyInstance(inst) == 1;
    }

    // Inference on the new instance
    public boolean inferGround(double[] sample) throws Exception {
        // 5 RSSI level, 7 GPS accuracy (m), 12 temperature, 6 RSSI value, 13 pressure, 9 Wifi RSSI, 14 humidity
        double[] entry = new double[]{sample[5], sample[7], sample[12], sample[6], sample[13], sample[9], sample[14]};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesGround);
        return classifierGround.classifyInstance(inst) == 1;
    }

    private void loadModels(Context context) {
        File filePocket = context.getFileStreamPath(MODEL_POCKET);
        File fileDoor = context.getFileStreamPath(MODEL_DOOR);
        File fileGround = context.getFileStreamPath(MODEL_GROUND);

        FileInputStream fileInputStream;
        ObjectInputStream objectInputStream;
        FileOutputStream fileOutputStream;
        ObjectOutputStream objectOutputStream;

        // Check local model existence
        if (!filePocket.exists() || !fileDoor.exists() || !fileGround.exists()) {
            //Log.d(TAG, "Local models do not exist.");
            // Initialize trained models
            try {
                fileInputStream = context.getAssets().openFd(MODEL_POCKET).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierPocket = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = context.getAssets().openFd(DATASET_POCKET).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesPocket = (Instances) objectInputStream.readObject();

                fileInputStream = context.getAssets().openFd(MODEL_DOOR).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierDoor = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = context.getAssets().openFd(DATASET_DOOR).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesDoor = (Instances) objectInputStream.readObject();

                fileInputStream = context.getAssets().openFd(MODEL_GROUND).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierGround = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = context.getAssets().openFd(DATASET_GROUND).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesGround = (Instances) objectInputStream.readObject();

                objectInputStream.close();
                fileInputStream.close();
                //Log.d(TAG, "Success in loading from assets.");

                fileOutputStream = context.openFileOutput(MODEL_POCKET, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierPocket);
                fileOutputStream = context.openFileOutput(DATASET_POCKET, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(instancesPocket);

                fileOutputStream = context.openFileOutput(MODEL_DOOR, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierDoor);
                fileOutputStream = context.openFileOutput(DATASET_DOOR, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(instancesDoor);

                fileOutputStream = context.openFileOutput(MODEL_GROUND, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierGround);
                fileOutputStream = context.openFileOutput(DATASET_GROUND, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(instancesGround);

                objectOutputStream.close();
                fileOutputStream.close();
                //Log.d(TAG, "Success in saving into file.");
            } catch (Exception e) {
                Log.d(TAG, "Error when loading from file: " + e);
            }
        } else {
            //Log.d(TAG, "Local models already exist.");
            try {
                fileInputStream = context.openFileInput(MODEL_POCKET);
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierPocket = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = context.openFileInput(DATASET_POCKET);
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesPocket = (Instances) objectInputStream.readObject();

                fileInputStream = context.openFileInput(MODEL_DOOR);
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierDoor = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = context.openFileInput(DATASET_DOOR);
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesDoor = (Instances) objectInputStream.readObject();

                fileInputStream = context.openFileInput(MODEL_GROUND);
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierGround = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = context.openFileInput(DATASET_GROUND);
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesGround = (Instances) objectInputStream.readObject();

                objectInputStream.close();
                fileInputStream.close();
                //Log.d(TAG, "Success in loading from file.");
            } catch (Exception e) {
                Log.d(TAG, "Error when loading model file: " + e);
            }
        }

    }
}