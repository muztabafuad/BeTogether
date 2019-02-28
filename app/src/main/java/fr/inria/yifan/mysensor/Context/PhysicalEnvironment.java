package fr.inria.yifan.mysensor.Context;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import java.util.Random;

import weka.classifiers.trees.HoeffdingTree;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * This class provides context information about the physical environments.
 */

public class PhysicalEnvironment extends BroadcastReceiver {

    private static final String TAG = "Physical environment";

    // The lambda parameter for learning model update
    public static final int LAMBDA = 10;

    // Define the learning model files to load
    private static final String MODEL_POCKET = "Classifier_pocket.model";
    private static final String MODEL_DOOR = "Classifier_door.model";
    private static final String MODEL_GROUND = "Classifier_ground.model";
    private static final String DATASET_POCKET = "Dataset_pocket.model";
    private static final String DATASET_DOOR = "Dataset_door.model";
    private static final String DATASET_GROUND = "Dataset_ground.model";

    // Instances for data set initialization
    private Instances instancesPocket;
    private Instances instancesDoor;
    private Instances instancesGround;
    // Hoeffding Tree for classifiers initialization
    private HoeffdingTree classifierPocket;
    private HoeffdingTree classifierDoor;
    private HoeffdingTree classifierGround;

    // Variables
    private Context mContext;
    private HashMap<String, Boolean> mPhysicalEnv;
    private SensorManager mSensorManager;
    private TelephonyManager mTelephonyManager;
    private LocationManager mLocationManager;
    private WifiManager mWifiManager;
    private int mHierarResult; // 1 in-pocket, 2 out-pocket out-door, 3 out-pocket in-door under-ground, 4 out-pocket in-door on-ground
    private float mLight;
    private float mRssiLevel;
    private float mRssiValue;
    private float mAccuracy;
    private float mWifiRssi;
    private float mProximity;
    private float mTemperature;
    private float mPressure;
    private float mHumidity;

    // Declare light sensor listener
    private SensorEventListener mListenerLight = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mLight = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    // Declare GSM RSSI state listener
    private PhoneStateListener mListenerPhone = new PhoneStateListener() {
        @Override
        @RequiresApi(api = Build.VERSION_CODES.M)
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mRssiLevel = signalStrength.getLevel();
            mRssiValue = signalStrength.getGsmSignalStrength() * 2 - 113; // -> dBm
        }
    };

    // Declare location service listener
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

    // Declare proximity sensor listener
    private SensorEventListener mListenerProxy = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mProximity = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    // Declare temperature sensor listener
    private SensorEventListener mListenerTemp = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mTemperature = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    // Declare pressure sensor listener
    private SensorEventListener mListenerPress = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mPressure = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    // Declare humidity sensor listener
    private SensorEventListener mListenerHumid = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mHumidity = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    // Constructor initialization
    public PhysicalEnvironment(Context context) {
        mContext = context;
        loadModels(mContext);

        // Initial values are set to indoor
        mLight = 1000;
        mRssiLevel = 3;
        mRssiValue = -100;
        mAccuracy = 1000;
        mWifiRssi = -100;
        mProximity = 8;
        mTemperature = 25;
        mPressure = 1007;
        mHumidity = 65;

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mWifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mPhysicalEnv = new HashMap<>();
        mPhysicalEnv.put("InPocket", null);
        mPhysicalEnv.put("InDoor", null);
        mPhysicalEnv.put("UnderGround", null);
    }

    // Public method to generate a Poisson number
    private static int Poisson() {
        double L = Math.exp(-LAMBDA);
        double p = 1d;
        int k = 0;
        do {
            k++;
            p *= Math.random();
        } while (p > L);
        return k - 1;
    }

    @SuppressLint("MissingPermission")
    public void startService() {
        // Register listeners for all sensors and components
        try {
            mSensorManager.registerListener(mListenerLight, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL);
            mTelephonyManager.listen(mListenerPhone, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, mListenerLoc);
            mContext.registerReceiver(this, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
            mSensorManager.registerListener(mListenerProxy, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mListenerTemp, mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE), SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mListenerPress, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mListenerHumid, mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY), SensorManager.SENSOR_DELAY_NORMAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopService() {
        try {
            mSensorManager.unregisterListener(mListenerLight);
            mTelephonyManager.listen(mListenerPhone, PhoneStateListener.LISTEN_NONE);
            mLocationManager.removeUpdates(mListenerLoc);
            mContext.unregisterReceiver(this);
            mSensorManager.unregisterListener(mListenerProxy);
            mSensorManager.unregisterListener(mListenerTemp);
            mSensorManager.unregisterListener(mListenerPress);
            mSensorManager.unregisterListener(mListenerHumid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Get the most recent physical environment
    public HashMap getPhysicalEnv() {
        try {
            if (inferInPocket()) {
                mHierarResult = 1;
                mPhysicalEnv.put("InPocket", true);
                mPhysicalEnv.put("InDoor", null);
                mPhysicalEnv.put("UnderGround", null);
            } else if (!inferInDoor()) {
                mHierarResult = 2;
                mPhysicalEnv.put("InPocket", false);
                mPhysicalEnv.put("InDoor", false);
                mPhysicalEnv.put("UnderGround", null);
            } else if (inferUnderGround()) {
                mHierarResult = 3;
                mPhysicalEnv.put("InPocket", false);
                mPhysicalEnv.put("InDoor", true);
                mPhysicalEnv.put("UnderGround", true);
            } else {
                mHierarResult = 4;
                mPhysicalEnv.put("InPocket", false);
                mPhysicalEnv.put("InDoor", true);
                mPhysicalEnv.put("UnderGround", false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mPhysicalEnv;
    }

    // Inference on the new instance
    private boolean inferInPocket() throws Exception {
        // 10 Proximity, 12 temperature, 2 light density
        double[] entry = new double[]{mProximity, mTemperature, mLight};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesPocket);
        return classifierPocket.classifyInstance(inst) == 1;
    }

    // Inference on the new instance
    private boolean inferInDoor() throws Exception {
        // 7 GPS accuracy, 5 RSSI level, 6 RSSI value, 9 Wifi RSSI, 2 light density, 12 temperature
        double[] entry = new double[]{mAccuracy, mRssiLevel, mRssiValue, mWifiRssi, mLight, mTemperature};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesDoor);
        return classifierDoor.classifyInstance(inst) == 1;
    }

    // Inference on the new instance
    private boolean inferUnderGround() throws Exception {
        // 5 RSSI level, 7 GPS accuracy (m), 12 temperature, 6 RSSI value, 13 pressure, 9 Wifi RSSI, 14 humidity
        double[] entry = new double[]{mRssiLevel, mAccuracy, mTemperature, mRssiValue, mPressure, mWifiRssi, mHumidity};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesGround);
        return classifierGround.classifyInstance(inst) == 1;
    }

    // Update the model by online learning
    private void updateInPocket() throws Exception {
        // 10 Proximity, 12 temperature, 2 light density
        double[] entry = new double[]{mProximity, mTemperature, mLight, inferInPocket() ? 0 : 1};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesPocket);
        for (int k = 0; k < Poisson(); k++) {
            classifierPocket.updateClassifier(inst);
        }
    }

    // Update the model by online learning
    private void updateInDoor() throws Exception {
        // 7 GPS accuracy, 5 RSSI level, 6 RSSI value, 9 Wifi RSSI, 2 light density, 12 temperature
        double[] entry = new double[]{mAccuracy, mRssiLevel, mRssiValue, mWifiRssi, mLight, mTemperature, inferInDoor() ? 0 : 1};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesDoor);
        for (int k = 0; k < Poisson(); k++) {
            classifierDoor.updateClassifier(inst);
        }
    }

    // Update the model by online learning
    private void updateUnderGround() throws Exception {
        // 5 RSSI level, 7 GPS accuracy (m), 12 temperature, 6 RSSI value, 13 pressure, 9 Wifi RSSI, 14 humidity
        double[] entry = new double[]{mRssiLevel, mAccuracy, mTemperature, mRssiValue, mPressure, mWifiRssi, mHumidity, inferUnderGround() ? 0 : 1};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesGround);
        for (int k = 0; k < Poisson(); k++) {
            classifierGround.updateClassifier(inst);
        }
    }

    // Load models and data set format from files
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

    // Update the classifiers hierarchically
    public void updateModels() {
        try {
            switch (mHierarResult) {
                case 1:
                    updateInPocket();
                    break;
                case 2:
                    updateInDoor();
                    break;
                case 3:
                    updateUnderGround();
                    break;
                case 4:
                    Random ran = new Random();
                    if (ran.nextInt(2) == 1) {
                        //Log.d(TAG, "Door update");
                        updateInDoor();
                    } else {
                        //Log.d(TAG, "Ground update");
                        updateUnderGround();
                    }
                    break;
                default:
                    Log.e(TAG, "Wrong inference result code");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Wifi signal strength detected callback
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            mWifiRssi = mWifiManager.getConnectionInfo().getRssi();
        } catch (Exception e) {
            //Pass
        }
    }
}