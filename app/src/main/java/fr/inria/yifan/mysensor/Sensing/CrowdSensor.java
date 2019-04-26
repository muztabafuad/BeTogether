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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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

    private final Object mLock; // Thread locker
    private Context mContext;
    private LocationManager mLocationManager;
    private SensorManager mSensorManager;
    private AudioRecord mAudioRecord;
    private AWeighting mAWeighting;

    // "Temperature", "Light", "Pressure", "Humidity", "Noise"
    private Location mLocation;
    private float mTemperature;
    private float mLight;
    private float mPressure;
    private float mHumidity;

    private boolean isGetSenseRun; // Running flag
    private JSONObject mSamples;

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
    protected CrowdSensor(Context context) {
        mContext = context;
        mLock = new Object();
        isGetSenseRun = false;
    }

    // Upload the content to the database on cloud
    public static void doProxyUpload(JSONObject instance) {
        // Access a Cloud FireStore instance from App
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        try {
            HashMap entry = new ObjectMapper().readValue(instance.toString(), HashMap.class);
            // Add a new document with a generated ID
            db.collection("SensingData").add(entry)
                    .addOnSuccessListener(documentReference ->
                            Log.d(TAG, "Document added with ID: " + documentReference.getId()))
                    .addOnFailureListener(e ->
                            Log.w(TAG, "Error adding document", e));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Get the aggregation result
    public static JSONObject doAggregation(List<JSONObject> jsonList) {
        JSONObject result = new JSONObject();
        for (JSONObject json : jsonList) {
            for (Iterator<String> it = json.keys(); it.hasNext(); ) {
                String key = it.next();
                try {
                    result.put(key, json.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    // Start the coordinator service
    public void startCoordinator() {

    }

    // Stop all registered service
    private void stopAllService() {
        isGetSenseRun = false;
        mLocationManager.removeUpdates(mLocationListener);
        // "Temperature", "Light", "Pressure", "Humidity", "Noise"
        mSensorManager.unregisterListener(mTempListener);
        mSensorManager.unregisterListener(mLightListener);
        mSensorManager.unregisterListener(mPressListener);
        mSensorManager.unregisterListener(mHumidListener);
        if (mAudioRecord != null) {
            mAudioRecord.stop();
        }
    }

    // Starting a sensing service
    // "Location", "Temperature", "Light", "Pressure", "Humidity", "Noise"
    @SuppressLint("MissingPermission")
    public void startService(String service) {
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        switch (service) {
            case "Location":
                // Check whether the GPS is enabled
                if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, mLocationListener);
                    mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                // Check whether the network is enabled
                else if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, mLocationListener);
                    mLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } else {
                    Toast.makeText(mContext, "Please enable the GPS/Network location!", Toast.LENGTH_SHORT).show();
                    mLocation = new Location("");
                }
                break;
            case "Temperature":
                mSensorManager.registerListener(mTempListener, mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE), SensorManager.SENSOR_DELAY_FASTEST);
                break;
            case "Light":
                mSensorManager.registerListener(mLightListener, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_FASTEST);
                break;
            case "Pressure":
                mSensorManager.registerListener(mPressListener, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_FASTEST);
                break;
            case "Humidity":
                mSensorManager.registerListener(mHumidListener, mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY), SensorManager.SENSOR_DELAY_FASTEST);
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

    // Start the main thread for all services allocated, total number of samples, delay of each sample
    public void startWorkingThread(List<String> services, int numSamples, float delay) {
        isGetSenseRun = true;
        //Log.e(TAG, "Work allocated: " + services);

        mSamples = new JSONObject();
        List<Integer> timestamp = new ArrayList<>();
        List<Float> latitude = new ArrayList<>();
        List<Float> longitude = new ArrayList<>();
        List<Float> temperature = new ArrayList<>();
        List<Float> light = new ArrayList<>();
        List<Float> pressure = new ArrayList<>();
        List<Float> humidity = new ArrayList<>();
        List<Float> noise = new ArrayList<>();

        for (String service : services) {
            startService(service);
        }
        new Thread(() -> {

            int count = 0;
            while (isGetSenseRun && count < numSamples) {
                // Sampling time delay
                synchronized (mLock) {
                    try {
                        mLock.wait((long) delay);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                timestamp.add((int) (currentTimeMillis() / 1000));
                for (String service : services) {
                    switch (service) {
                        case "Location":
                            latitude.add(getCurrentMeasurement("Latitude"));
                            longitude.add(getCurrentMeasurement("Longitude"));
                            break;
                        case "Temperature":
                            temperature.add(getCurrentMeasurement("Temperature"));
                            break;
                        case "Light":
                            light.add(getCurrentMeasurement("Light"));
                            break;
                        case "Pressure":
                            pressure.add(getCurrentMeasurement("Pressure"));
                            break;
                        case "Humidity":
                            humidity.add(getCurrentMeasurement("Humidity"));
                            break;
                        case "Noise":
                            noise.add(getCurrentMeasurement("Noise"));
                            break;
                        default:
                            Log.d(TAG, "Aggregator or proxy collaborator");
                            break;
                    }
                }
                count += 1;
            }
            try {
                if (!timestamp.isEmpty()) {
                    mSamples.put("Timestamp", timestamp);
                }
                if (!latitude.isEmpty()) {
                    mSamples.put("Latitude", latitude);
                }
                if (!longitude.isEmpty()) {
                    mSamples.put("Longitude", longitude);
                }
                if (!temperature.isEmpty()) {
                    mSamples.put("Temperature", temperature);
                }
                if (!light.isEmpty()) {
                    mSamples.put("Light", light);
                }
                if (!pressure.isEmpty()) {
                    mSamples.put("Pressure", pressure);
                }
                if (!humidity.isEmpty()) {
                    mSamples.put("Humidity", humidity);
                }
                if (!noise.isEmpty()) {
                    mSamples.put("Noise", noise);
                }
                onWorkFinished(mSamples);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ).start();
    }

    // Get the main result for all services allocated
    public JSONObject getWorkingResult() {
        stopAllService();
        return mSamples;
    }

    // Callback when the sensing work is finished
    public abstract void onWorkFinished(JSONObject result);

    /*
    db.collection("SensingData").get()
                .addOnCompleteListener(task -> {
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