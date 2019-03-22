package fr.inria.yifan.mysensor.Deprecated;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

import fr.inria.yifan.mysensor.ContextActivity;
import fr.inria.yifan.mysensor.Deprecated.Sensing.ContextHelper;
import fr.inria.yifan.mysensor.Deprecated.Sensing.SensorsHelper;
import fr.inria.yifan.mysensor.Deprecated.Support.FilesIOHelper;
import fr.inria.yifan.mysensor.R;
import fr.inria.yifan.mysensor.ServiceActivity;

import static fr.inria.yifan.mysensor.Deprecated.Support.Configuration.DST_MAIL_ADDRESS;
import static fr.inria.yifan.mysensor.Deprecated.Support.Configuration.ENABLE_REQUEST_LOCATION;
import static fr.inria.yifan.mysensor.Deprecated.Support.Configuration.SAMPLE_WINDOW_MS;
import static java.lang.System.currentTimeMillis;

/*
 * This activity provides functions including labeling contexts and storing/sending sensing data
 */

public class SensingActivity extends AppCompatActivity {

    private static final String TAG = "Sensing activity";

    private final Object mLock; // Thread locker
    private boolean isGetSenseRun; // Running flag
    private int mSenseRound; // Sensing round
    private PowerManager.WakeLock mWakeLock; // Awake locker

    // Declare all related views in UI
    private Button mStartButton;
    private Button mStopButton;
    private Switch mSwitchLog;
    private Switch mSwitchMail;
    private ArrayAdapter<String> mAdapterSensing;

    private FilesIOHelper mFilesIOHelper; // File helper
    private ArrayList<String> mSensingData; // Sensing data
    private int mPocketLabel; // In-pocket binary label
    private int mDoorLabel; // In-door binary label
    private int mGroundLabel; // Under-ground binary label

    // Helpers for sensors and context
    private SensorsHelper mSensorHelper;
    private ContextHelper mContextHelper;

    // Constructor initializes locker
    public SensingActivity() {
        mLock = new Object();
    }

    // Initially bind all views
    private void bindViews() {
        TextView mWelcomeView = findViewById(R.id.welcome_view);
        mWelcomeView.setText(R.string.hint_sensing);
        mStartButton = findViewById(R.id.start_button);
        mStopButton = findViewById(R.id.stop_button);
        mStopButton.setVisibility(View.INVISIBLE);
        mSwitchLog = findViewById(R.id.switch_log);
        mSwitchMail = findViewById(R.id.switch_mail);
        mSwitchMail.setVisibility(View.INVISIBLE);

        // Build an adapter to feed the list with the content of a string array
        mSensingData = new ArrayList<>();
        mAdapterSensing = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mSensingData);
        // Then attache the adapter to the list view
        ListView listView = findViewById(R.id.list_view);
        listView.setAdapter(mAdapterSensing);

        mStartButton.setOnClickListener(view -> {
            mAdapterSensing.clear();
            mAdapterSensing.add("0 timestamp, 1 daytime (b), 2 light density (lx), 3 magnetic strength (μT), " +
                    "4 GSM active (b), 5 RSSI level, 6 RSSI value (dBm), 7 GPS accuracy (m), 8 Wifi active (b), " +
                    "9 Wifi RSSI (dBm), 10 proximity (b), 11 sound level (dBA), 12 temperature (C), 13 pressure (hPa), " +
                    "14 humidity (%), 15 in-pocket label, 16 in-door label, 17 under-ground label");
            startRecord();
            mStartButton.setVisibility(View.INVISIBLE);
            mStopButton.setVisibility(View.VISIBLE);
        });
        mStopButton.setOnClickListener(view -> {
            stopRecord();
            mStartButton.setVisibility(View.VISIBLE);
            mStopButton.setVisibility(View.INVISIBLE);
        });
        mSwitchLog.setOnClickListener(view -> {
            if (mSwitchLog.isChecked()) {
                mSwitchMail.setVisibility(View.VISIBLE);
            } else {
                mSwitchMail.setVisibility(View.INVISIBLE);
            }
        });
    }

    // Main activity initialization
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensing);
        bindViews();

        mSensorHelper = new SensorsHelper(this);
        mContextHelper = new ContextHelper(this);
        mFilesIOHelper = new FilesIOHelper(this);

        //acquireWakeLock();
    }

    // Stop thread when exit!
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSwitchLog.isChecked() && isGetSenseRun) {
            try {
                mFilesIOHelper.autoSave(arrayToString(mSensingData));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //releaseWakeLock();
        isGetSenseRun = false;
        mSensorHelper.stopService();
        mContextHelper.stopService();
    }

    // Start the sensing thread
    private void startRecord() {
        if (isGetSenseRun) {
            Log.e(TAG, "Still in sensing and recording");
            return;
        }
        mSensorHelper.startService();
        mContextHelper.startService();
        isGetSenseRun = true;
        mSenseRound = 0;
        new Thread(() -> {
            while (isGetSenseRun) {
                runOnUiThread(() -> {
                    mAdapterSensing.add(currentTimeMillis() + ", " +
                            mContextHelper.isDaytime() + ", " +
                            mSensorHelper.getLightDensity() + ", " +
                            mSensorHelper.getMagnet() + ", " +
                            mContextHelper.isGSMLink() + ", " +
                            mContextHelper.getRssiLevel() + ", " +
                            mContextHelper.getRssiValue() + ", " +
                            mContextHelper.getGPSAccuracy() + ", " +
                            mContextHelper.isWifiLink() + ", " +
                            mContextHelper.getWifiRSSI() + ", " +
                            mSensorHelper.getProximity() + ", " +
                            mSensorHelper.getSoundLevel() + ", " +
                            mSensorHelper.getTemperature() + ", " +
                            mSensorHelper.getPressure() + ", " +
                            mSensorHelper.getHumidity() + ", " +
                            mPocketLabel + ", " +
                            mDoorLabel + ", " +
                            mGroundLabel);
                    mSenseRound += 1;
                    //Log.d(TAG, String.valueOf(mSenseRound));
                });
                // Sampling time delay
                synchronized (mLock) {
                    try {
                        mLock.wait(SAMPLE_WINDOW_MS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    // Stop the sensing
    @SuppressLint("SetTextI18n")
    private void stopRecord() {
        isGetSenseRun = false;
        mSensorHelper.stopService();
        mContextHelper.stopService();
        if (mSwitchLog.isChecked()) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            final EditText editName = new EditText(this);
            editName.setText(android.os.Build.MODEL + "_" + currentTimeMillis());
            dialog.setTitle("Enter file name: ");
            dialog.setView(editName);
            dialog.setPositiveButton("OK", (dialogInterface, i) -> {
                //Log.d(TAG, "Now is " + time);
                try {
                    String filename = editName.getText() + ".csv";
                    mFilesIOHelper.saveFile(filename, arrayToString(mSensingData));
                    if (mSwitchMail.isChecked()) {
                        Log.d(TAG, "File path is : " + mFilesIOHelper.getFileUri(filename));
                        mFilesIOHelper.sendFile(DST_MAIL_ADDRESS, getString(R.string.email_title), mFilesIOHelper.getFileUri(filename));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            dialog.setNegativeButton("Cancel", (dialogInterface, i) -> {
                //Pass
            });
            dialog.show();
        }
    }

    // Go to the context activity
    public void goContext(View view) {
        Intent goToContext = new Intent();
        goToContext.setClass(this, ContextActivity.class);
        startActivity(goToContext);
        finish();
    }

    // Go to the service activity
    public void goService(View view) {
        Intent goToService = new Intent();
        goToService.setClass(this, ServiceActivity.class);
        startActivity(goToService);
        finish();
    }

    // Callback for user enabling GPS switch
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ENABLE_REQUEST_LOCATION: {
                mContextHelper.startService();
            }
        }
    }

    // Turn on the awake locker
    private void acquireWakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            assert pm != null;
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getCanonicalName());
            mWakeLock.acquire(100 * 60 * 1000L /*100 minutes*/);
        }
    }

    // Turn off the awake locker
    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    // Convert string array to single string
    private String arrayToString(ArrayList<String> array) {
        StringBuilder content = new StringBuilder();
        for (String line : array) {
            content.append(line).append("\n");
        }
        return content.toString();
    }

}