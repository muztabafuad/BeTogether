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
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_LOCATION;
import static fr.inria.yifan.mysensor.Support.Configuration.LOCATION_UPDATE_DISTANCE;
import static fr.inria.yifan.mysensor.Support.Configuration.LOCATION_UPDATE_TIME;

/**
 * This class provides functions including initialize and reading data from sensors.
 */

public class SensorsHelper {

    private static final String TAG = "Sensors helper";

    private Activity mActivity;

    // Declare sensing variables
    private float mLight;
    private float mProximity;
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
        mSensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        Sensor mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        Sensor mSensorProxy = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        // Start to sense the light and proximity
        mSensorManager.registerListener(mListenerLight, mSensorLight, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mListenerProxy, mSensorProxy, SensorManager.SENSOR_DELAY_UI);
        mLocationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
        initialization();
    }

    // Check if location service on system is enabled
    @SuppressLint("MissingPermission")
    private void initialization() {
        // Check GPS enable switch
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(mActivity, "Please enable the GPS", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            mActivity.startActivityForResult(intent, ENABLE_REQUEST_LOCATION);
        } else {
            // Start GPS and location service
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_TIME, LOCATION_UPDATE_DISTANCE, mListenerGPS);
        }
    }

    // Unregister the broadcast receiver and listeners
    public void close() {
        mSensorManager.unregisterListener(mListenerLight);
        mSensorManager.unregisterListener(mListenerProxy);
        mLocationManager.removeUpdates(mListenerGPS);
    }

    // Get the most recent light density
    public float getLightDensity() {
        return mLight;
    }

    // Get thr most recent proximity value
    public float getProximity() {
        return mProximity;
    }

    // Simple In-pocket detection function
    public boolean isInPocket() {
        //Toast.makeText(this, "In-pocket", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this, "Out-pocket", Toast.LENGTH_SHORT).show();
        return mProximity == 0 && mLight < 30;
    }

    // Get location information from GPS
    public Location getLocation() {
        Log.d(TAG, "Location information: " + mLocation);
        return mLocation;
    }
}
