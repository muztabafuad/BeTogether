package fr.inria.yifan.mysensor.Support;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_LOCATION;
import static fr.inria.yifan.mysensor.Support.Configuration.LOCATION_UPDATE_DISTANCE;
import static fr.inria.yifan.mysensor.Support.Configuration.LOCATION_UPDATE_TIME;
import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_RATE_IN_HZ;

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

    // Declare sensing variables
    private float mLight;
    private float mProximity;
    private float mTemperature;
    private float mPressure;
    private float mHumidity;
    private Location mLocation;

    private SensorManager mSensorManager;
    private LocationManager mLocationManager;

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

    // Declare proximity sensor listener
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

    // Declare proximity sensor listener
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

    // Declare proximity sensor listener
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
    private LocationListener mListenerGPS = new LocationListener() {
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
        Log.d(TAG, "Buffer size = " + BUFFER_SIZE);

        mSensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorProxy = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorTemp = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mSensorPress = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mSensorHumid = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);

        mLocationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
        initial();
    }

    // Check if location service on system is enabled
    @SuppressLint("MissingPermission")
    public void initial() {
        mAudioRecord.startRecording();
        // Register listeners for all environmental sensors
        mSensorManager.registerListener(mListenerLight, mSensorLight, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mListenerProxy, mSensorProxy, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mListenerTemp, mSensorTemp, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mListenerPress, mSensorPress, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mListenerHumid, mSensorHumid, SensorManager.SENSOR_DELAY_UI);
        // Check GPS enable switch
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Start GPS and location service
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_TIME, LOCATION_UPDATE_DISTANCE, mListenerGPS);
        } else {
            Toast.makeText(mActivity, "Please enable the GPS", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            mActivity.startActivityForResult(intent, ENABLE_REQUEST_LOCATION);
        }
    }

    // Unregister the broadcast receiver and listeners
    public void close() {
        mAudioRecord.stop();
        mSensorManager.unregisterListener(mListenerLight);
        mSensorManager.unregisterListener(mListenerProxy);
        mSensorManager.unregisterListener(mListenerTemp);
        mSensorManager.unregisterListener(mListenerPress);
        mSensorManager.unregisterListener(mListenerHumid);
        mLocationManager.removeUpdates(mListenerGPS);
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
        Log.d(TAG, "Sound dB value: " + volume);
        return (int) volume;
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
        // Detection on day or night
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 6 || hour > 18) {
            return mLight > 10;
        } else {
            return mLight < 1500;
        }
    }

    // Get location information from GPS
    public Location getLocation() {
        Log.d(TAG, "Location information: " + mLocation);
        return mLocation;
    }
}
