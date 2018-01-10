package fr.inria.yifan.mysensor;

import android.Manifest;
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
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView mTextTitle;
    private TextView mTextMessage;
    private TextView mTextMessage2;
    private TextView mTextMessage3;

    private Sensor mSensorLight;
    private Sensor mSensorProxy;
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;

    private float mLight;
    private float mProximity;
    private static final String[] LOCATION_PERMS = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    initialListeners();
                    mTextTitle.setText(R.string.title_test);
                    mTextMessage2.setText("...");
                    senseGPS();
                    return true;
                case R.id.navigation_dashboard:
                    initialListeners();
                    mTextTitle.setText(R.string.title_sensors);
                    mTextMessage2.setText("...");
                    mTextMessage3.setText("...");
                    showSensorList();
                    return true;
                case R.id.navigation_notifications:
                    initialListeners();
                    mTextTitle.setText(R.string.title_sensing);
                    mTextMessage3.setText("...");
                    senseLightProxy();
                    return true;
            }
            return false;
        }
    };

    private void bindViews() {
        mTextTitle = (TextView) findViewById(R.id.title);
        mTextMessage = (TextView) findViewById(R.id.message);
        mTextMessage2 = (TextView) findViewById(R.id.message2);
        mTextMessage3 = (TextView) findViewById(R.id.message3);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    public void initialListeners() {
        mSensorManager.unregisterListener(mListnerLight);
        mSensorManager.unregisterListener(mListnerProxy);
    }

    public void senseGPS() {
        if (!mLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 0);
        }

        List<String> names = mLocationManager.getAllProviders();
        mTextMessage.setText(getListString(names));
        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(LOCATION_PERMS, 1);
            Toast.makeText(this, "Requesting permission", Toast.LENGTH_SHORT).show();
            return;
        } else {
            mTextMessage2.setText("The current location provider is " + mLocationManager.getProvider(LocationManager.GPS_PROVIDER).getName() + ".\n");
            Location lc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            updateGPSShow(lc);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, new LocationListener() {
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
            });
        }
    }

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

    public void showSensorList() {
        List<Sensor> allSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        StringBuilder sb = new StringBuilder();
        sb.append("This device has " + allSensors.size() + " sensors (include uncalibrated), listed as:\n\n");
        int i = 0;
        for (Sensor s : allSensors) {
            i += 1;
            switch (s.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    sb.append(i + ": Accelerometer sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    sb.append(i + ": Ambient temperature sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_GAME_ROTATION_VECTOR:
                    sb.append(i + ": Game rotation vector sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                    sb.append(i + ": Geomagnetic rotation vector sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_GRAVITY:
                    sb.append(i + ": Gravity sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    sb.append(i + ":Gyroscope sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_LIGHT:
                    sb.append(i + ":Light sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    sb.append(i + ": Linear acceleration sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    sb.append(i + ": Magnetic field sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_ORIENTATION:
                    sb.append(i + ": Orientation sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_PRESSURE:
                    sb.append(i + ": Pressure sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_PROXIMITY:
                    sb.append(i + ": Proximity sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_RELATIVE_HUMIDITY:
                    sb.append(i + ": Relative humidity sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    sb.append(i + ": Rotation vector sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_TEMPERATURE:
                    sb.append(i + ": Temperature sensor " + s.getType() + ".\n");
                    break;
                default:
                    sb.append(i + ": Other sensor " + s.getType() + ".\n");
                    break;
            }
            sb.append(" - Device name: " + s.getName() + "\n - Device version: " + s.getVersion() + "\n - Manufacturer: "
                    + s.getVendor() + "\n - Power consumption: " + s.getPower() + "\n\n");
        }
        mTextMessage.setText(sb.toString());
    }

    public void senseLightProxy() {
        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorProxy = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorManager.registerListener(mListnerLight, mSensorLight, mSensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mListnerProxy, mSensorProxy, mSensorManager.SENSOR_DELAY_UI);
    }

    SensorEventListener mListnerLight = new SensorEventListener() {
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

    SensorEventListener mListnerProxy = new SensorEventListener() {
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

    public void sensePocket() {
        if (mProximity == 0 && mLight < 50) {
            mTextMessage3.setText("\nIn-pocket.");
        } else {
            mTextMessage3.setText("\nOut-pocket.");
        }
    }

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
