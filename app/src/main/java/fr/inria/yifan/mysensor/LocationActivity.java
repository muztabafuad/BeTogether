package fr.inria.yifan.mysensor;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class LocationActivity extends AppCompatActivity {

    // Declare views
    private TextView mTextTitle;
    private TextView mTextMessage;
    private TextView mTextMessage2;
    private TextView mTextMessage3;

    // Declare sensors and managers
    private Sensor mSensorLight;
    private Sensor mSensorProxy;
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;

    // Declare variables
    private float mLight;
    private float mProximity;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1001;
    private static final String[] LOCATION_PERMS = {
            permission.ACCESS_FINE_LOCATION
    };

    // Declare light sensor listener
    private SensorEventListener mListenerLight = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mLight = (float) sensorEvent.values[0];
            mTextMessage.setText("Light：" + mLight + " of " + mSensorLight.getMaximumRange() + " in lux units.");
            sensePocket();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    // Declare proximity sensor listener
    private SensorEventListener mListenerProxy = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mProximity = (float) sensorEvent.values[0];
            mTextMessage2.setText("Proxmity：" + mProximity + " of " + mSensorProxy.getMaximumRange() + " in binary near or far.");
            sensePocket();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    // Declare location service listener
    private LocationListener mListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateGPSShow(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onProviderEnabled(String provider) {
            updateGPSShow(mLocationManager.getLastKnownLocation(provider));
        }

        @Override
        public void onProviderDisabled(String provider) {
            updateGPSShow(null);
        }
    };

    // Initially bind views
    private void bindViews() {
        mTextTitle = (TextView) findViewById(R.id.title);
        mTextMessage = (TextView) findViewById(R.id.message);
        mTextMessage2 = (TextView) findViewById(R.id.message2);
        mTextMessage3 = (TextView) findViewById(R.id.message3);
    }

    // Clear all views content
    private void initialView() {
        mTextMessage.setText("...");
        mTextMessage2.setText("...");
        mTextMessage3.setText("...");
    }

    // Unregister all sensor listeners
    private void initialListeners() {
        mSensorManager.unregisterListener(mListenerLight);
        mSensorManager.unregisterListener(mListenerProxy);
        mLocationManager.removeUpdates(mListenerGPS);
    }

    // Start to sense the light
    private void senseLightProxy() {
        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorProxy = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorManager.registerListener(mListenerLight, mSensorLight, mSensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mListenerProxy, mSensorProxy, mSensorManager.SENSOR_DELAY_UI);
    }

    // In-pocket detection function
    private void sensePocket() {
        if (mProximity == 0 && mLight < 50) {
            mTextMessage3.setText("\nIn-pocket.");
        } else {
            mTextMessage3.setText("\nOut-pocket.");
        }
    }

    // Start GPS location service
    private void startGPS() {
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 0);
        }
        List<String> names = mLocationManager.getAllProviders();
        mTextMessage.setText(getListString(names));
        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Requesting permission", Toast.LENGTH_SHORT).show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(LOCATION_PERMS, MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return;
        } else {
            showGPSList();
        }
    }

    // Callback for user allowing permission
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showGPSList();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(LOCATION_PERMS, MY_PERMISSIONS_REQUEST_LOCATION);
                    }
                }
                return;
            }
        }
    }

    // Configure and show GPS service information
    @SuppressLint("MissingPermission")
    private void showGPSList() {
        mTextMessage2.setText("The current location provider is " + mLocationManager.getProvider(LocationManager.GPS_PROVIDER).getName() + ".\n");
        Location lc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        updateGPSShow(lc);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, mListenerGPS);
    }

    // Update the GPS information in views
    private void updateGPSShow(Location location) {
        if (location != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Current location information：\n");
            sb.append(" - Longitude：" + location.getLongitude() + "\n");
            sb.append(" - Latitude：" + location.getLatitude() + "\n");
            sb.append(" - Altitude：" + location.getAltitude() + "\n");
            sb.append(" - Speed：" + location.getSpeed() + "\n");
            sb.append(" - Bearing：" + location.getBearing() + "\n");
            sb.append(" - Accuracy：" + location.getAccuracy() + "\n");
            mTextMessage3.setText(sb.toString());
        } else mTextMessage3.setText("...");
    }

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    // Convert a list into a string
    private String getListString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (String str : list) {
            sb.append(i + " " + str + ".\n");
            i++;
        }
        return sb.toString();
    }

}
