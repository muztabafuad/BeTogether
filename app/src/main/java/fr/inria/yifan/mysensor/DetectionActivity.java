package fr.inria.yifan.mysensor;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import fr.inria.yifan.mysensor.Inference.InferHelper;
import fr.inria.yifan.mysensor.Sensing.ContextHelper;
import fr.inria.yifan.mysensor.Sensing.SensorsHelper;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_LOCATION;
import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_WINDOW_MS;
import static java.lang.System.currentTimeMillis;

/*
 * This activity provides functions including in-pocket detection and GPS location service.
 */

public class DetectionActivity extends AppCompatActivity {

    private static final String TAG = "Detection activity";

    // Thread locker and running flag
    private final Object mLock;
    private boolean isSensingRun;

    // Declare all used views
    private TextView mPocketView;
    private TextView mDoorView;
    private TextView mGroundView;
    private TextView mLocationView;
    private TextView mActivityView;
    private Button mPocketButton;
    private Button mDoorButton;
    private Button mGroundButton;
    private Button mStartButton;
    private Button mStopButton;
    private NotificationCompat.Builder mNotifyBuilder;
    private NotificationManagerCompat notificationManager;

    // Sensors helper for sensor and context
    private SensorsHelper mSensorHelper;
    private ContextHelper mContextHelper;
    private InferHelper mInferHelper;
    private double[] mSample;
    private boolean mInPocket;
    private boolean mInDoor;
    private boolean mUnderGround;

    // Constructor initializes locker
    public DetectionActivity() {
        mLock = new Object();
    }

    // Initially bind all views
    private void bindViews() {
        TextView mWelcomeView = findViewById(R.id.welcome_view);
        mWelcomeView.setText(R.string.hint_detect);

        mPocketView = findViewById(R.id.pocket_text);
        mDoorView = findViewById(R.id.door_text);
        mGroundView = findViewById(R.id.ground_text);
        mActivityView = findViewById(R.id.activity_view);
        mLocationView = findViewById(R.id.location_view);
        mPocketButton = findViewById(R.id.pocket_button);
        mDoorButton = findViewById(R.id.door_button);
        mGroundButton = findViewById(R.id.ground_button);
        mStartButton = findViewById(R.id.start_button);
        mStopButton = findViewById(R.id.stop_button);
        mStopButton.setVisibility(View.INVISIBLE);

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSensing();
                mPocketButton.setVisibility(View.VISIBLE);
                mDoorButton.setVisibility(View.VISIBLE);
                mGroundButton.setVisibility(View.VISIBLE);
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

        mPocketButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mInferHelper.updateModel("Pocket", mSample, 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mDoorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mInferHelper.updateModel("Door", mSample, 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mGroundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mInferHelper.updateModel("Ground", mSample, 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Clean all text views
    @SuppressLint("SetTextI18n")
    private void cleanView() {
        mPocketView.setText(null);
        mDoorView.setText(null);
        mGroundView.setText(null);
        mActivityView.setText(null);
        mLocationView.setText(null);
        mPocketButton.setVisibility(View.INVISIBLE);
        mDoorButton.setVisibility(View.INVISIBLE);
        mGroundButton.setVisibility(View.INVISIBLE);
    }

    // Notification bar initialization
    private void notifyView() {
        // TODO intent when users click the feedback button
        Intent feedbackIntent = new Intent(this, BroadcastReceiver.class);
        mNotifyBuilder = new NotificationCompat.Builder(this, "Inference")
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(getString(R.string.notify_title))
                .setContentText(getString(R.string.notify_content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_delete, "On-ground!", PendingIntent.getBroadcast(this, 0, feedbackIntent, 0))
                .addAction(android.R.drawable.ic_delete, "Out-door!", PendingIntent.getBroadcast(this, 0, feedbackIntent, 0));
        notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(1, mNotifyBuilder.build());
    }

    // Main activity initialization
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);
        bindViews();
        cleanView();
        notifyView();
        mSensorHelper = new SensorsHelper(this);
        mContextHelper = new ContextHelper(this);
        mInferHelper = new InferHelper(this);
        if (mSensorHelper != null) {
            mSensorHelper.startService();
        }
        if (mContextHelper != null) {
            mContextHelper.startService();
        }
    }

    // Stop thread when exit!
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isSensingRun = false;
        if (mSensorHelper != null) {
            mSensorHelper.stopService();
        }
        if (mContextHelper != null) {
            mContextHelper.stopService();
        }
    }

    // Start the sensing detection
    @SuppressLint("SetTextI18n")
    private void startSensing() {
        if (isSensingRun) {
            Log.e(TAG, "Still in sensing state");
            return;
        }
        //mSensorHelper.startService();
        isSensingRun = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isSensingRun) {
                    // Sampling time delay
                    synchronized (mLock) {
                        try {
                            mLock.wait(SAMPLE_WINDOW_MS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.N)
                        @Override
                        public void run() {
                            Location location = mContextHelper.getLocation();

                            /*
                            0 timestamp, 1 daytime (b), 2 light density (lx), 3 magnetic strength (μT), 4 GSM active (b),
                            5 RSSI level, 6 RSSI value (dBm), 7 GPS accuracy (m), 8 Wifi active (b), 9 Wifi RSSI (dBm),
                            10 proximity (b), 11 sound level (dBA), 12 temperature (C), 13 pressure (hPa), 14 humidity (%),
                            */
                            mSample = new double[]{currentTimeMillis(),
                                    mContextHelper.isDaytime(),
                                    mSensorHelper.getLightDensity(),
                                    mSensorHelper.getMagnet(),
                                    mContextHelper.isGSMLink(),
                                    mContextHelper.getRssiLevel(),
                                    mContextHelper.getRssiValue(),
                                    mContextHelper.getGPSAccuracy(),
                                    mContextHelper.isWifiLink(),
                                    mContextHelper.getWifiRSSI(),
                                    mSensorHelper.getProximity(),
                                    mSensorHelper.getSoundLevel(),
                                    mSensorHelper.getTemperature(),
                                    mSensorHelper.getPressure(),
                                    mSensorHelper.getHumidity()};
                            //Log.d(TAG, Arrays.toString(mSample));

                            try {
                                if (mInPocket = mInferHelper.inferInPocket(mSample)) {
                                    mPocketView.setText("In-pocket");
                                } else {
                                    mPocketView.setText("Out-pocket");
                                }
                                //Log.d(TAG, String.valueOf(mInferHelper.InferIndoor(sample)));
                                if (mInDoor = mInferHelper.inferInDoor(mSample)) {
                                    mDoorView.setText("In-door");
                                } else {
                                    mDoorView.setText("Out-door");
                                }
                                //Log.d(TAG, String.valueOf(mInferHelper.InferUnderground(sample)));
                                if (mUnderGround = mInferHelper.inferUnderGround(mSample)) {
                                    mGroundView.setText("Under-ground");
                                } else {
                                    mGroundView.setText("On-ground");
                                }
                                mNotifyBuilder.setContentText(mInferHelper.inferOneResult(mSample));
                                notificationManager.notify(1, mNotifyBuilder.build());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            mActivityView.setText(/*"Signal strength level: " + mContextHelper.getRssiLevel() + "\n\n" +*/
                                    mContextHelper.getUserActivity());

                            //Log.d(TAG, "Wifi state: " + mContextHelper.isWifiLink());

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
                    });
                }
            }
        }).start();
    }

    // Stop the sensing detection
    private void stopSensing() {
        //mSensorHelper.stopService();
        isSensingRun = false;
        //cleanView();
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
                mContextHelper.startService();
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
