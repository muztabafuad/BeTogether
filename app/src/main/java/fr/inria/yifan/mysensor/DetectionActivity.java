package fr.inria.yifan.mysensor;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import fr.inria.yifan.mysensor.Support.SensorsHelper;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_LOCATION;
import static fr.inria.yifan.mysensor.Support.Configuration.PERMS_REQUEST_LOCATION;
import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_DELAY_IN_MS;

/*
* This activity provides functions including in-pocket detection and GPS location service.
*/

public class DetectionActivity extends AppCompatActivity {

    private static final String TAG = "Detection activity";
    // Thread locker and running flag
    private final Object mLock;
    // Declare all used views
    private TextView mProximityView;
    private TextView mLightView;
    private TextView mPocketView;
    private TextView mLocationView;
    private Button mStartButton;
    private Button mStopButton;
    private boolean isSensingRun;

    // Sensors helper for sensor and GPS
    private SensorsHelper mSensorHelper;

    // Constructor initializes locker
    public DetectionActivity() {
        mLock = new Object();
    }

    // Initially bind all views
    private void bindViews() {
        mProximityView = findViewById(R.id.proximity_view);
        mLightView = findViewById(R.id.light_view);
        mPocketView = findViewById(R.id.pocket_view);
        mLocationView = findViewById(R.id.location_view);
        mStartButton = findViewById(R.id.start_button);
        mStopButton = findViewById(R.id.stop_button);
        mStopButton.setVisibility(View.INVISIBLE);

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSensing();
                mStartButton.setVisibility(View.INVISIBLE);
                mStopButton.setVisibility(View.VISIBLE);
            }
        });
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSensing();
                mStartButton.setVisibility(View.VISIBLE);
                mStopButton.setVisibility(View.INVISIBLE);
            }
        });
    }

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);
        bindViews();
    }

    // Simple In-pocket detection function
    private boolean isInPocket(float proximity, float light) {
        //Toast.makeText(this, "In-pocket", Toast.LENGTH_SHORT).show();
//Toast.makeText(this, "Out-pocket", Toast.LENGTH_SHORT).show();
        return proximity == 0 && light < 30;
    }

    // Callback for user enabling GPS switch
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ENABLE_REQUEST_LOCATION: {
                mSensorHelper.initialization();
            }
        }
    }

    // Callback for user allowing permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    mSensorHelper.initialization();
                }
            }
        }
    }

    // Start the sensing detection
    private void startSensing() {
        if (isSensingRun) {
            Log.e(TAG, "Still in sensing");
            return;
        }
        isSensingRun = true;
        mSensorHelper = new SensorsHelper(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isSensingRun) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            float proximity = mSensorHelper.getProximity();
                            float light = mSensorHelper.getLightDensity();
                            mProximityView.setText("Proximity：" + proximity + " in binary (near or far).");
                            mLightView.setText("Light：" + light + " of " + " in lux units.");
                            if (isInPocket(proximity, light)) {
                                mPocketView.setText("Detection result: In-pocket");
                            } else {
                                mPocketView.setText("Detection result: Out-pocket");
                            }
                            Location location = mSensorHelper.getLocation();
                            if (location != null) {
                                String loc = "Current location information：\n" +
                                        " - Longitude：" + location.getLongitude() + "\n" +
                                        " - Latitude：" + location.getLatitude() + "\n" +
                                        " - Altitude：" + location.getAltitude() + "\n" +
                                        " - Speed：" + location.getSpeed() + "\n" +
                                        " - Bearing：" + location.getBearing() + "\n" +
                                        " - Accuracy：" + location.getAccuracy() + "\n";
                                mLocationView.setText(loc);
                            }
                        }
                    });
                    // 10 times per second
                    synchronized (mLock) {
                        try {
                            mLock.wait(SAMPLE_DELAY_IN_MS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    // Stop the sensing detection
    private void stopSensing() {
        isSensingRun = false;
        mSensorHelper.close();
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
