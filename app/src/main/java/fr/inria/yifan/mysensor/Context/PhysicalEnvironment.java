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

import static fr.inria.yifan.mysensor.Context.FeatureHelper.LAMBDA;
import static fr.inria.yifan.mysensor.Context.FeatureHelper.MIN_UPDATE_TIME;

/**
 * This class provides context information about the physical environments.
 * The keys to retrieve the values are "InPocket", "InDoor" and "UnderGround".
 */

public class PhysicalEnvironment extends BroadcastReceiver {

    private static final String TAG = "Physical environment";

    // Define the learning model files to load
    private static final String MODEL_POCKET = "Classifier_pocket.model";
    private static final String MODEL_DOOR = "Classifier_door.model";
    private static final String MODEL_GROUND = "Classifier_ground.model";
    private static final String DATASET_POCKET = "Dataset_pocket.model";
    private static final String DATASET_DOOR = "Dataset_door.model";
    private static final String DATASET_GROUND = "Dataset_ground.model";

    // Instances for data set description
    private Instances instancesPocket;
    private Instances instancesDoor;
    private Instances instancesGround;
    // Hoeffding Tree classifiers declaration
    private HoeffdingTree classifierPocket;
    private HoeffdingTree classifierDoor;
    private HoeffdingTree classifierGround;

    // Variables
    private Context mContext;
    private HashMap<String, String> mPhysicalEnv;
    private int mHierarResult; // 1 in-pocket, 2 out-pocket out-door, 3 out-pocket in-door under-ground, 4 out-pocket in-door on-ground

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
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
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

    // Constructor
    public PhysicalEnvironment(Context context) {
        mContext = context;
        loadModels(mContext);

        // Initial values are set to out-pocket in-door on-ground state
        mLight = 1000f;
        mRssiLevel = 3f;
        mRssiValue = -100f;
        mAccuracy = 1000f;
        mWifiRssi = -100f;
        mProximity = 8f;
        mTemperature = 25f;
        mPressure = 1007f;
        mHumidity = 65f;

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mWifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mPhysicalEnv = new HashMap<>();
        mPhysicalEnv.put("InPocket", null);
        mPhysicalEnv.put("InDoor", null);
        mPhysicalEnv.put("UnderGround", null);
    }

    // Method to generate a Poisson number
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
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_UPDATE_TIME, 1, mListenerLoc);
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
    // 1 in-pocket, 2 out-pocket out-door, 3 out-pocket in-door under-ground, 4 out-pocket in-door on-ground
    public HashMap getPhysicalEnv() {
        try {
            if (inferInPocket()) {
                mHierarResult = 1;
                mPhysicalEnv.put("InPocket", "True");
                mPhysicalEnv.put("InDoor", "Null");
                mPhysicalEnv.put("UnderGround", "Null");
            } else if (!inferInDoor()) {
                mHierarResult = 2;
                mPhysicalEnv.put("InPocket", "False");
                mPhysicalEnv.put("InDoor", "False");
                mPhysicalEnv.put("UnderGround", "Null");
            } else if (inferUnderGround()) {
                mHierarResult = 3;
                mPhysicalEnv.put("InPocket", "False");
                mPhysicalEnv.put("InDoor", "True");
                mPhysicalEnv.put("UnderGround", "True");
            } else {
                mHierarResult = 4;
                mPhysicalEnv.put("InPocket", "False");
                mPhysicalEnv.put("InDoor", "True");
                mPhysicalEnv.put("UnderGround", "False");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mPhysicalEnv;
    }

    // Inference on the new instance
    private boolean inferInPocket() throws Exception {
        // Proximity, temperature, light density
        double[] entry = new double[]{mProximity, mTemperature, mLight};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesPocket);
        return classifierPocket.classifyInstance(inst) == 1;
    }

    // Inference on the new instance
    private boolean inferInDoor() throws Exception {
        // GPS accuracy, RSSI level, RSSI value, Wifi RSSI, light density, temperature
        double[] entry = new double[]{mAccuracy, mRssiLevel, mRssiValue, mWifiRssi, mLight, mTemperature};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesDoor);
        return classifierDoor.classifyInstance(inst) == 1;
    }

    // Inference on the new instance
    private boolean inferUnderGround() throws Exception {
        // RSSI level, GPS accuracy, temperature, RSSI value, pressure, Wifi RSSI, humidity
        double[] entry = new double[]{mRssiLevel, mAccuracy, mTemperature, mRssiValue, mPressure, mWifiRssi, mHumidity};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesGround);
        return classifierGround.classifyInstance(inst) == 1;
    }

    // Update the model as online learning
    private void updateInPocket() throws Exception {
        // Proximity, temperature, light density
        double[] entry = new double[]{mProximity, mTemperature, mLight, inferInPocket() ? 0 : 1};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesPocket);
        for (int k = 0; k < Poisson(); k++) {
            classifierPocket.updateClassifier(inst);
        }
    }

    // Update the model as online learning
    private void updateInDoor() throws Exception {
        // GPS accuracy, RSSI level, RSSI value, Wifi RSSI, light density, temperature
        double[] entry = new double[]{mAccuracy, mRssiLevel, mRssiValue, mWifiRssi, mLight, mTemperature, inferInDoor() ? 0 : 1};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesDoor);
        for (int k = 0; k < Poisson(); k++) {
            classifierDoor.updateClassifier(inst);
        }
    }

    // Update the model as online learning
    private void updateUnderGround() throws Exception {
        // RSSI level, GPS accuracy (m), temperature, RSSI value, pressure, Wifi RSSI, humidity
        double[] entry = new double[]{mRssiLevel, mAccuracy, mTemperature, mRssiValue, mPressure, mWifiRssi, mHumidity, inferUnderGround() ? 0 : 1};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesGround);
        for (int k = 0; k < Poisson(); k++) {
            classifierGround.updateClassifier(inst);
        }
    }

    // Update the classifiers hierarchically by one click
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
                        updateInDoor();
                    } else {
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
            } catch (Exception e) {
                Log.d(TAG, "Error when loading from file: " + e);
            }
        } else {
            // Local models already exist
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
            } catch (Exception e) {
                Log.d(TAG, "Error when loading model file: " + e);
            }
        }
    }

    // Wifi signal strength detected callback
    @Override
    public void onReceive(Context context, Intent intent) {
        mWifiRssi = mWifiManager.getConnectionInfo().getRssi();
    }
}
