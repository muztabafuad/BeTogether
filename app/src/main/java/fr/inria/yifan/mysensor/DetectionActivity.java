package fr.inria.yifan.mysensor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import fr.inria.yifan.mysensor.Support.SensorsHelper;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_LOCATION;
import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_DELAY_IN_MS;

/*
* This activity provides functions including in-pocket detection and GPS location service.
*/

public class DetectionActivity extends AppCompatActivity {

    private static final String TAG = "Detection activity";

    // Thread locker and running flag
    private final Object mLock;
    private boolean isSensingRun;

    // Declare all used views
    private TextView mProximityView;
    private TextView mLightView;
    private TextView mPocketView;
    private TextView mLocationView;
    private TextView mIndoorView;
    private Button mStartButton;
    private Button mStopButton;

    // Sensors helper for sensor and GPS
    private SensorsHelper mSensorHelper;

    // Constructor initializes locker
    public DetectionActivity() {
        mLock = new Object();
    }

    // Initially bind all views
    private void bindViews() {
        TextView mWelcomeView = findViewById(R.id.welcome_view);
        mWelcomeView.setText(R.string.hint_detect);

        mProximityView = findViewById(R.id.proximity_view);
        mLightView = findViewById(R.id.light_view);
        mPocketView = findViewById(R.id.pocket_view);
        mIndoorView = findViewById(R.id.indoor_view);
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

    // Clean all text views
    @SuppressLint("SetTextI18n")
    private void cleanView() {
        mProximityView.setText(null);
        mLightView.setText(null);
        mPocketView.setText(null);
        mIndoorView.setText(null);
        mLocationView.setText(null);
    }

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);
        bindViews();
        cleanView();
        mSensorHelper = new SensorsHelper(this);
    }

    // Resume the sensing service
    @Override
    protected void onResume() {
        super.onResume();
        if (mSensorHelper != null) {
            mSensorHelper.initial();
        }
    }

    // Stop thread when exit!
    @Override
    protected void onPause() {
        isSensingRun = false;
        super.onPause();
        if (mSensorHelper != null) {
            mSensorHelper.close();
        }
    }

    // Start the sensing detection
    @SuppressLint("SetTextI18n")
    private void startSensing() {
        if (isSensingRun) {
            Log.e(TAG, "Still in sensing state");
            return;
        }
        isSensingRun = true;
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

                            if (mSensorHelper.isInPocket()) {
                                mPocketView.setText("Detection result: In-pocket");
                            } else {
                                mPocketView.setText("Detection result: Out-pocket");
                            }

                            if (mSensorHelper.isInDoor()) {
                                mIndoorView.setText("Detection result: In-door");
                            } else {
                                mIndoorView.setText("Detection result: Out-door");
                            }

                            Location location = mSensorHelper.getLocation();
                            if (location != null) {
                                String loc = "Current location information：\n" +
                                        " - Longitude：" + location.getLongitude() + "\n" +
                                        " - Latitude：" + location.getLatitude() + "\n" +
                                        " - Altitude：" + location.getAltitude() + "\n" +
                                        " - Speed：" + location.getSpeed() + "\n" +
                                        " - Bearing：" + location.getBearing() + "\n" +
                                        " - Accuracy：" + location.getAccuracy() + "\n" +
                                        " - Time：" + location.getTime();
                                mLocationView.setText(loc);
                            }
                        }
                    });
                    // sample times per second
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
        cleanView();
    }

    // Go to the sensing activity
    public void goSensing(View view) {
        Intent goToSensing = new Intent();
        goToSensing.setClass(this, SensingActivity.class);
        startActivity(goToSensing);
        finish();
    }

    // Go to the network activity
    public void goNetwork(View view) {
        Intent goToNetwork = new Intent();
        goToNetwork.setClass(this, NetworkActivity.class);
        startActivity(goToNetwork);
        finish();
    }

    // Callback for user enabling GPS switch
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ENABLE_REQUEST_LOCATION: {
                mSensorHelper.initial();
            }
        }
    }

    // Convert a list into a string
    private String getListString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (String str : list) {
            sb.append(i).append(" ").append(str).append(".\n");
            i++;
        }
        return sb.toString();
    }
}
