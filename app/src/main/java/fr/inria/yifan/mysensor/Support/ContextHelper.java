package fr.inria.yifan.mysensor.Support;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_LOCATION;
import static fr.inria.yifan.mysensor.Support.Configuration.LOCATION_UPDATE_DISTANCE;
import static fr.inria.yifan.mysensor.Support.Configuration.LOCATION_UPDATE_TIME;

/**
 * This class represents the context map set of a sensing device.
 */

public class ContextHelper extends BroadcastReceiver {

    private static final String TAG = "Device context";

    // Declare references and managers
    private Activity mActivity;
    private TelephonyManager mTelephonyManager;
    private LocationManager mLocationManager;

    // Declare all contexts
    private int rssiDbm;
    private float hasBattery;
    private long localTime;
    private boolean inPocket;
    private boolean inDoor;
    private boolean underGround;
    private ArrayMap<Sensor, Boolean> sensorArray;
    private Location mLocation;
    private String userActivity;

    //private TensorFlowInferenceInterface inferenceInterface;

    // Declare GSM RSSI state listener
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            rssiDbm = signalStrength.getGsmSignalStrength() * 2 - 113; // -> dBm
            //Log.d(TAG, String.valueOf(rssiDbm));
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

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public ContextHelper(Activity activity) {
        mActivity = activity;
        mLocation = new Location("null");

        mTelephonyManager = (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);
        mLocationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);

        //List<CellInfo> cellList = mTelephonyManager.getAllCellInfo();
        /*
        for(CellInfo cellInfo: cellList){
            Log.d(TAG, String.valueOf(cellInfo));
        }
        */

        //IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //Intent batteryStatus = mActivity.registerReceiver(null, filter);

        // Are we charging / charged?
        //assert batteryStatus != null;
        //int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        //boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
        //int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        //boolean acCharge = (chargePlug == BatteryManager.BATTERY_PLUGGED_AC);

        // Get the current battery capacity
        //int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        //int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        //float batteryPct = level / (float) scale;

        //inferenceInterface = new TensorFlowInferenceInterface(mActivity.getAssets(), MODEL_FILE);
    }

    // Start the context service
    @SuppressLint("MissingPermission")
    public void startService() {
        rssiDbm = 0;
        hasBattery = 0;
        localTime = 0;
        inPocket = false;
        inDoor = false;
        underGround =false;
        mLocation = null;
        userActivity = null;

        assert mTelephonyManager != null;
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        assert mLocationManager != null;
        // Check GPS enable switch
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Start GPS and location service
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_TIME, LOCATION_UPDATE_DISTANCE, mListenerLoc);
        } else {
            Toast.makeText(mActivity, "Please enable the GPS", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            mActivity.startActivityForResult(intent, ENABLE_REQUEST_LOCATION);
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(mActivity, 1,
                new Intent("ActivityRecognitionResult"), PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognitionClient activityRecognitionClient = ActivityRecognition.getClient(mActivity);
        activityRecognitionClient.requestActivityUpdates(LOCATION_UPDATE_TIME, pendingIntent);
        mActivity.registerReceiver(this, new IntentFilter("ActivityRecognitionResult"));
    }

    // Unregister the listeners
    public void stopService() {
        mLocationManager.removeUpdates(mListenerLoc);
        mActivity.unregisterReceiver(this);
    }

    // Get the most recent RSSI
    public int getRssiDbm() {
        return rssiDbm;
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

    // Get the most recent user activity
    public String getUserActivity(){
        return userActivity;
    }

    // Intent receiver for activity recognition result callback
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            userActivity = result.getMostProbableActivity().toString();
            //Log.e(TAG, "Received intent: " + result.getMostProbableActivity().toString());
        }
    }
}
