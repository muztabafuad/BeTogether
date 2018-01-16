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

/*
* This activity provides functions including in-pocket detection and GPS location service.
*/

public class InPocketActivity extends AppCompatActivity {

    // Declare GPS permissions
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1000;
    private static final int MY_ENABLE_REQUEST_LOCATION = 1001;
    private static final String[] LOCATION_PERMS = {permission.ACCESS_FINE_LOCATION};

    // Declare all used views
    private TextView mTextTitle;
    private TextView mTextMessage;
    private TextView mTextMessage2;
    private TextView mTextMessage3;
    private TextView mTextMessage4;
    private TextView mTextMessage5;
    private TextView mTextMessage6;

    // Declare sensors and managers
    private Sensor mSensorLight;
    private Sensor mSensorProxy;
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;

    // Declare sensing variables
    private float mLight;
    private float mProximity;

    // Declare light sensor listener
    private SensorEventListener mListenerLight = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mLight = sensorEvent.values[0];
            mTextMessage.setText("Light：" + mLight + " of " + mSensorLight.getMaximumRange() + " in lux units.");
            sensePocket();
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
            mTextMessage2.setText("Proximity：" + mProximity + " of " + mSensorProxy.getMaximumRange() + " in binary (near or far).");
            sensePocket();
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
            updateGPSShow(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //PASS
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

    // Initially bind all views
    private void bindViews() {
        mTextTitle = findViewById(R.id.title);
        mTextMessage = findViewById(R.id.message);
        mTextMessage2 = findViewById(R.id.message2);
        mTextMessage3 = findViewById(R.id.message3);
        mTextMessage4 = findViewById(R.id.message4);
        mTextMessage5 = findViewById(R.id.message5);
        mTextMessage6 = findViewById(R.id.message6);
    }

    // Initialize all views contents
    private void initialView() {
        mTextTitle.setText(R.string.title_detect);
        mTextMessage.setText("...");
        mTextMessage2.setText("...");
        mTextMessage3.setText("...");
        mTextMessage4.setText("...");
        mTextMessage5.setText("...");
        mTextMessage6.setText("...");
    }

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);
        bindViews();
        initialView();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        senseLightProxy();
        startGPSLocation();
    }

    // Register the broadcast receiver with the intent values to be matched
    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mListenerLight, mSensorLight, mSensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mListenerProxy, mSensorProxy, mSensorManager.SENSOR_DELAY_UI);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, mListenerGPS);
    }

    // Unregister the broadcast receiver and listeners
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mListenerLight);
        mSensorManager.unregisterListener(mListenerProxy);
        mLocationManager.removeUpdates(mListenerGPS);
    }

    // Start to sense the light and proximity
    private void senseLightProxy() {
        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorProxy = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorManager.registerListener(mListenerLight, mSensorLight, mSensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mListenerProxy, mSensorProxy, mSensorManager.SENSOR_DELAY_UI);
    }

    // Simple In-pocket detection function
    private void sensePocket() {
        if (mProximity == 0 && mLight < 30) {
            mTextMessage3.setText("\nIn-pocket.");
        } else {
            mTextMessage3.setText("\nOut-pocket.");
        }
    }

    // Start GPS and location service
    private void startGPSLocation() {
        // Check GPS switch
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable the GPS", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, MY_ENABLE_REQUEST_LOCATION);
        } else {
            List<String> names = mLocationManager.getAllProviders();
            mTextMessage4.setText(getListString(names));
            // Check user permission for GPS
            if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Requesting GPS permission", Toast.LENGTH_SHORT).show();
                // Request permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(LOCATION_PERMS, MY_PERMISSIONS_REQUEST_LOCATION);
                } else {
                    Toast.makeText(this, "Please give GPS permission", Toast.LENGTH_SHORT).show();
                }
            } else {
                showGPSInfo();
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, mListenerGPS);
            }
        }
    }

    // Callback for user enabling GPS switch
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MY_ENABLE_REQUEST_LOCATION: {
                if (resultCode == RESULT_OK) {
                    startGPSLocation();
                }
            }
        }
    }

    // Callback for user allowing permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startGPSLocation();
                }
            }
        }
    }

    // Get location information from GPS ans show it
    @SuppressLint("MissingPermission")
    private void showGPSInfo() {
        mTextMessage5.setText("The current location provider is " + mLocationManager.getProvider(LocationManager.GPS_PROVIDER).getName() + ".\n");
        Location loc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        updateGPSShow(loc);
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
            mTextMessage6.setText(sb.toString());
        } else mTextMessage6.setText("...");
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
