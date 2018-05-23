package fr.inria.yifan.mysensor.Sensing;

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import java.util.Calendar;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_LOCATION;
import static fr.inria.yifan.mysensor.Support.Configuration.LOCATION_UPDATE_DISTANCE;
import static fr.inria.yifan.mysensor.Support.Configuration.LOCATION_UPDATE_TIME;
import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_NUM_WINDOW;

/**
 * This class represents the context map set of a sensing device.
 */

public class ContextHelper extends BroadcastReceiver {

    private static final String TAG = "Device context";

    // Declare references and managers
    private Activity mActivity;
    private TelephonyManager mTelephonyManager;
    private LocationManager mLocationManager;
    private ConnectivityManager mConnectManager;

    // Declare all contexts
    private SlideWindow mGSMFlag;
    private SlideWindow mRssiLevel;
    private SlideWindow mAccuracy;
    private SlideWindow mSpeed;
    //private float hasBattery;
    //private long localTime;
    //private boolean inPocket;
    //private boolean inDoor;
    //private boolean underGround;
    private ArrayMap<Sensor, Boolean> sensorArray;
    private Location mLocation;
    private String userActivity;

    //private TensorFlowInferenceInterface inferenceInterface;

    // Declare GSM RSSI state listener
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            //mRssiDbm.putValue(signalStrength.getGsmSignalStrength() * 2 - 113); // -> dBm
            mGSMFlag.putValue(signalStrength.isGsm() ? 1 : 0);
            //Log.d(TAG, "CDMA: " + String.valueOf(signalStrength.getCdmaDbm()));
            //Log.d(TAG, "Evdo: " + String.valueOf(signalStrength.getEvdoDbm()));
            //Log.d(TAG, "GSM: " + String.valueOf(signalStrength.getGsmSignalStrength() * 2 - 113));
            //Log.d(TAG, "Is GSM: " + String.valueOf(signalStrength.isGsm()));
            //Log.d(TAG, "Level: " + signalStrength.getLevel());
            mRssiLevel.putValue(signalStrength.getLevel());
        }
    };

    // Declare location service listener
    private LocationListener mListenerLoc = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;
            mAccuracy.putValue(location.getAccuracy());
            mSpeed.putValue(location.getSpeed());
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

        mTelephonyManager = (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);
        mLocationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
        mConnectManager = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

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

        mGSMFlag = new SlideWindow(SAMPLE_NUM_WINDOW, 0);
        mRssiLevel = new SlideWindow(SAMPLE_NUM_WINDOW, 0);
        mAccuracy = new SlideWindow(SAMPLE_NUM_WINDOW, 0);
        mSpeed = new SlideWindow(SAMPLE_NUM_WINDOW, 0);
        //hasBattery = 0;
        //localTime = 0;
        //inPocket = false;
        //inDoor = false;
        //underGround =false;
        userActivity = null;
        mLocation = new Location("null");

        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
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

    // Get the most recent GSM RSSI
    public float getGSMFlag() {
        return mGSMFlag.getMean();
    }

    // Get the most recent signal strength level
    public float getRssiLevel() {
        return mRssiLevel.getMean();
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

    // Get the most recent GPS accuracy
    public float getGPSAccuracy() {
        return mAccuracy.getMean();
    }

    // Get the most recent GPS speed
    public float getGPSSpeed() {
        return mSpeed.getMean();
    }

    // Get the most recent user activity
    public String getUserActivity() {
        return userActivity;
    }

    // Detection in daytime or night
    public int isDaytime() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return (hour > 6 && hour < 18) ? 1 : 0;
    }

    // Detection on Wifi access
    public int isWifiLink() {
        NetworkInfo info = mConnectManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return info.isConnected() ? 1 : 0;
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

    // Manually update sliding window
    public void updateWindow() {
        mGSMFlag.putValue(mGSMFlag.getLast());
        mRssiLevel.putValue(mRssiLevel.getLast());
        mAccuracy.putValue(mAccuracy.getLast());
        mSpeed.putValue(mSpeed.getLast());
    }

}
