// V1

package fr.inria.yifan.mysensor.Sensing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static fr.inria.yifan.mysensor.Context.ContextHelper.MIN_UPDATE_TIME;
import static fr.inria.yifan.mysensor.SensingActivity.SAMPLE_DELAY;
import static fr.inria.yifan.mysensor.SensingActivity.SAMPLE_NUMBER;
import static java.lang.System.currentTimeMillis;

/**
 * This class provides a crowdsensor and its sensing methods.
 */

public abstract class CrowdSensor {

    private static final String TAG = "CrowdSensor";

    // Parameters for audio sound signal sampling
    private static final int SAMPLE_RATE_IN_HZ = 44100;
    // Audio recorder parameters for sampling
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);

    private Context mContext;
    private LocationManager mLocationManager;
    private SensorManager mSensorManager;
    private final Object mLock; // Thread locker
    private List<String> mServices;

    // "Location", "Temperature", "Light", "Pressure", "Humidity", "Noise"
    private Location mLocation;
    private float mTemperature;
    private float mLight;
    private float mPressure;
    private float mHumidity;
    private AudioRecord mAudioRecord;
    private AWeighting mAWeighting;

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            //PASS
        }

        @Override
        public void onProviderEnabled(String s) {
            //PASS
        }

        @Override
        public void onProviderDisabled(String s) {
            //PASS
        }
    };

    // Declare temperature sensor listener
    private SensorEventListener mTempListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mTemperature = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            //PASS
        }
    };

    // Declare light sensor listener
    private SensorEventListener mLightListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mLight = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            //PASS
        }
    };

    // Declare pressure sensor listener
    private SensorEventListener mPressListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mPressure = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            //PASS
        }
    };

    // Declare humidity sensor listener
    private SensorEventListener mHumidListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mHumidity = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            //PASS
        }
    };

    // Constructor
    @SuppressLint("MissingPermission")
    protected CrowdSensor(Context context) {
        mContext = context;
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

        mLock = new Object();

        mLight = 1000f;
        mTemperature = 25f;
        mPressure = 1007f;
        mHumidity = 65f;
    }

    // Upload the content to the database on cloud
    public static void doProxyUpload(JSONObject instance) {
        // Access a Cloud FireStore instance from App
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        try {
            HashMap entry = new ObjectMapper().readValue(instance.toString(), HashMap.class);
            // Add a new document with a generated ID
            db.collection("SensingData").add(entry)
                    .addOnSuccessListener(documentReference -> Log.d(TAG, "Document added with ID: " + documentReference.getId()))
                    .addOnFailureListener(e -> Log.w(TAG, "Error adding document", e));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Get the aggregation result
    static JSONObject doAggregation(JSONObject json1, JSONObject json2) {
        for (Iterator<String> it = json2.keys(); it.hasNext(); ) {
            String key = it.next();
            try {
                json1.put(key, json2.get(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return json1;
    }

    // Start the coordinator service
    public void startCoordinator() {
        // Pass
    }

    // Starting a sensing service
    // "Location", "Temperature", "Light", "Pressure", "Humidity", "Noise"
    @SuppressLint("MissingPermission")
    private void startAService(String service) {
        switch (service) {
            case "Location":
                // Check whether the GPS is enabled
                if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_UPDATE_TIME, 1, mLocationListener);
                    mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                // Check whether the network is enabled
                else if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_UPDATE_TIME, 1, mLocationListener);
                    mLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } else {
                    Toast.makeText(mContext, "Please enable the GPS/Network location!", Toast.LENGTH_SHORT).show();
                    mLocation = new Location("");
                }
                if (mLocation == null) {
                    mLocation = new Location("");
                }
                break;
            case "Temperature":
                mSensorManager.registerListener(mTempListener, mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE), SensorManager.SENSOR_DELAY_NORMAL);
                break;
            case "Light":
                mSensorManager.registerListener(mLightListener, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL);
                break;
            case "Pressure":
                mSensorManager.registerListener(mPressListener, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
                break;
            case "Humidity":
                mSensorManager.registerListener(mHumidListener, mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY), SensorManager.SENSOR_DELAY_NORMAL);
                break;
            case "Noise":
                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
                mAWeighting = new AWeighting(SAMPLE_RATE_IN_HZ);
                mAudioRecord.startRecording();
                break;
            default:
                Log.d(TAG, "Aggregator or proxy collaborator");
                break;
        }
    }

    // Start all registered service
    public void startServices(List<String> services) {
        mServices = services;
        for (String service : services) {
            startAService(service);
        }
    }

    // Read the most current value from a service
    private float getCurrentMeasurement(String service) {
        switch (service) {
            case "Latitude":
                return (float) mLocation.getLatitude();
            //return mLocation != null ? (float) mLocation.getLatitude() : 0;
            case "Longitude":
                return (float) mLocation.getLongitude();
            //return mLocation != null ? (float) mLocation.getLongitude() : 0;
            case "Temperature":
                return mTemperature;
            case "Light":
                return mLight;
            case "Pressure":
                return mPressure;
            case "Humidity":
                return mHumidity;
            case "Noise":
                short[] buffer = new short[BUFFER_SIZE];
                int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                mAWeighting = new AWeighting(SAMPLE_RATE_IN_HZ);
                buffer = mAWeighting.apply(buffer);
                long v = 0;
                for (short aBuffer : buffer) {
                    v += aBuffer * aBuffer;
                }
                return (float) (10 * Math.log10(v / (double) r));
            default:
                return 0f;
        }
    }

    // Get the current result for all services allocated
    public JSONObject getCurrentResult() {
        // "Timestamp", "Latitude", "Longitude", "Temperature", "Light", "Pressure", "Humidity", "Noise"
        JSONObject mSamples = new JSONObject();
        try {
            mSamples.put("Timestamp", (int) (currentTimeMillis() / 1000));
            for (String service : mServices) {
                if (service.equals("Location")) {
                    mSamples.put("Latitude", getCurrentMeasurement("Latitude"));
                    mSamples.put("Longitude", getCurrentMeasurement("Longitude"));
                } else {
                    mSamples.put(service, getCurrentMeasurement(service));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mSamples;
    }

    // Stop all registered service
    public void stopServices() {
        mServices = null;
        try {
            // "Location", "Temperature", "Light", "Pressure", "Humidity", "Noise"
            mLocationManager.removeUpdates(mLocationListener);
            mSensorManager.unregisterListener(mTempListener);
            mSensorManager.unregisterListener(mLightListener);
            mSensorManager.unregisterListener(mPressListener);
            mSensorManager.unregisterListener(mHumidListener);
            //if (mAudioRecord != null) {
            mAudioRecord.stop();
            //}
        } catch (Exception e) {
            Log.e(TAG, String.valueOf(e));
        }
    }

    // Autonomous sensing thread
    void startWorkingThread(List<String> services) {
        new Thread(() -> {
            // Sensing thread loop
            int count = 0;
            while (count < SAMPLE_NUMBER) {
                // Sampling time delay
                synchronized (mLock) {
                    try {
                        mLock.wait((long) SAMPLE_DELAY);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //TODO
                    count++;
                }
            }
        }).start();
    }

    // Callback when the sensing work is finished
    public abstract void onWorkFinished(JSONObject result);

    /*
    db.collection("SensingData").get().addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
            for (QueryDocumentSnapshot document : task.getResult()) {
                Log.d(TAG, document.getId() + " => " + document.getData());
            }
        } else {
            Log.w(TAG, "Error getting documents.", task.getException());
        }
    });
    */
}
