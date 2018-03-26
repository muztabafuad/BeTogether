package fr.inria.yifan.mysensor.Support;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_LOCATION;
import static fr.inria.yifan.mysensor.Support.Configuration.INTERCEPT;
import static fr.inria.yifan.mysensor.Support.Configuration.LOCATION_UPDATE_DISTANCE;
import static fr.inria.yifan.mysensor.Support.Configuration.LOCATION_UPDATE_TIME;
import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_RATE_IN_HZ;
import static fr.inria.yifan.mysensor.Support.Configuration.SLOPE;

/**
 * This class provides functions including initialize and reading data from sensors.
 */

public class SensorsHelper {

    private static final String TAG = "Sensors helper";

    // Audio recorder parameters for sampling
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    private Activity mActivity;

    // Declare sensors and recorder
    private AudioRecord mAudioRecord;
    private AWeighting mAWeighting;
    private Sensor mSensorLight;
    private Sensor mSensorProxy;
    private Sensor mSensorTemp;
    private Sensor mSensorPress;
    private Sensor mSensorHumid;
    private Sensor mSensorMagnet;
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;

    // Declare sensing variables
    private float mLight;
    private float mProximity;
    private float mTemperature;
    private float mPressure;
    private float mHumidity;
    private float mMagnet;
    private Location mLocation;

    // Declare light sensor listener
    private SensorEventListener mListenerLight = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mLight = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            //PASS
        }
    };

    // Declare magnetic sensor listener
    private SensorEventListener mListenerMagnet = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mMagnet = (float) Math.sqrt(Math.pow(sensorEvent.values[0], 2) + Math.pow(sensorEvent.values[1], 2) + Math.pow(sensorEvent.values[2], 2));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            //PASS
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
            //PASS
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
            //PASS
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
            //PASS
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
            //PASS
        }
    };

    // Declare location service listener
    private LocationListener mListenerLoc = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //PASS
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onProviderEnabled(String provider) {
            mLocation = mLocationManager.getLastKnownLocation(provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            //PASS
        }
    };

    // Register the broadcast receiver with the intent values to be matched
    public SensorsHelper(Activity activity) {
        mActivity = activity;

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        mAWeighting = new AWeighting(SAMPLE_RATE_IN_HZ);
        //Log.d(TAG, "Buffer size = " + BUFFER_SIZE);

        mSensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorMagnet = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorProxy = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorTemp = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mSensorPress = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mSensorHumid = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);

        mLocationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
    }

    // Check if location service on system is enabled
    @SuppressLint("MissingPermission")
    public void start() {
        mLight = 0;
        mProximity = 0;
        mTemperature = 0;
        mPressure = 0;
        mHumidity = 0;
        mMagnet = 0;
        mLocation = new Location("null");

        mAudioRecord.startRecording();
        // Register listeners for all environmental sensors
        mSensorManager.registerListener(mListenerLight, mSensorLight, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mListenerMagnet, mSensorMagnet, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mListenerProxy, mSensorProxy, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mListenerTemp, mSensorTemp, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mListenerPress, mSensorPress, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mListenerHumid, mSensorHumid, SensorManager.SENSOR_DELAY_UI);

        // Check GPS enable switch
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Start GPS and location service
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_TIME, LOCATION_UPDATE_DISTANCE, mListenerLoc);
        } else {
            Toast.makeText(mActivity, "Please enable the GPS", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            mActivity.startActivityForResult(intent, ENABLE_REQUEST_LOCATION);
        }
    }

    // Unregister the broadcast receiver and listeners
    public void stop() {
        mAudioRecord.stop();
        mSensorManager.unregisterListener(mListenerLight);
        mSensorManager.unregisterListener(mListenerMagnet);
        mSensorManager.unregisterListener(mListenerProxy);
        mSensorManager.unregisterListener(mListenerTemp);
        mSensorManager.unregisterListener(mListenerPress);
        mSensorManager.unregisterListener(mListenerHumid);
        mLocationManager.removeUpdates(mListenerLoc);
    }

    // Get the most recent sound level
    public int getSoundLevel() {
        short[] buffer = new short[BUFFER_SIZE];
        // r is the real measurement data length, normally r is less than buffersize
        int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
        // Apply A-weighting filtering
        buffer = mAWeighting.apply(buffer);
        long v = 0;
        // Get content from buffer and calculate square sum
        for (short aBuffer : buffer) {
            v += aBuffer * aBuffer;
        }
        // Square sum divide by data length to get volume
        double mean = v / (double) r;
        final double volume = 10 * Math.log10(mean);
        //Log.d(TAG, "Sound dB value: " + volume);
        return (int) (volume * SLOPE + INTERCEPT);
    }

    // Get the most recent light density
    public float getLightDensity() {
        return mLight;
    }

    // Get thr most recent proximity value
    public float getProximity() {
        return mProximity;
    }

    // Get the most recent temperature
    public float getTemperature() {
        return mTemperature;
    }

    // Get the most recent pressure
    public float getPressure() {
        return mPressure;
    }

    // Get the most recent humidity
    public float getHumidity() {
        return mHumidity;
    }

    // Get the most recent magnet field
    public float getMagnet() {
        return mMagnet;
    }

    // Detection in daytime or night
    public boolean isDaytime() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour > 6 && hour < 18;
    }

    // Simple In/Out-pocket detection function
    public boolean isInPocket() {
        //Toast.makeText(this, "In-pocket", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this, "Out-pocket", Toast.LENGTH_SHORT).show();
        return mProximity == 0 && mLight < 10;
    }

    // Simple Indoor/Outdoor detection function
    public boolean isInDoor() {
        //Toast.makeText(this, "Indoor", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this, "Outdoor", Toast.LENGTH_SHORT).show();
        if (isDaytime()) {
            return mLight < 1500;
        } else {
            return mLight > 10;
        }
    }

    // Get location information from GPS
    @SuppressLint("MissingPermission")
    public Location getLocation() {
        for (GpsSatellite satellite : mLocationManager.getGpsStatus(null).getSatellites()) {
            Log.d(TAG, satellite.toString());
        }
        //Log.d(TAG, "Location information: " + mLocation);
        return mLocation;
    }
}
