package fr.inria.yifan.mysensor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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
import java.util.HashMap;

import fr.inria.yifan.mysensor.Context.ContextHelper;
import fr.inria.yifan.mysensor.Transmission.FilesIOHelper;

import static java.lang.System.currentTimeMillis;

/*
 * This activity provides functions storing/sending sensing data
 */

@SuppressLint("Registered")
public class ContextIntentActivity extends AppCompatActivity {

    // Email destination for the sensing data
    public static final String DST_MAIL_ADDRESS = "yifan.du@polytechnique.edu";
    private static final String TAG = "Sensing activity";
    private final Object mLock; // Thread locker
    private boolean isGetSenseRun; // Running flag

    // Declare all related views in UI
    private Button mStartButton;
    private Button mStopButton;
    private Switch mSwitchLog;
    private Switch mSwitchMail;
    private ArrayAdapter<String> mAdapterSensing;

    private FilesIOHelper mFilesIOHelper; // File helper
    private ArrayList<String> mSensingData; // Sensing data

    private ContextHelper mContextHelper;

    // Constructor initializes locker
    public ContextIntentActivity() {
        mLock = new Object();
    }

    // Initially bind all views
    @RequiresApi(api = Build.VERSION_CODES.M)
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
            //mAdapterSensing.add("0 Timestamp, 1 Latitude, Longitude, 2 Temperature (C), 3 Light density (lx), " +
            //"4 Pressure (hPa), 5 Humidity (%), 6 Sound level (dBA)");
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
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensing);
        bindViews();

        mFilesIOHelper = new FilesIOHelper(this);
        mContextHelper = new ContextHelper(this);
        mContextHelper.startService();
    }

    // Stop thread when exit!
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSwitchLog.isChecked()) {
            try {
                mFilesIOHelper.autoSave(arrayToString(mSensingData));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        isGetSenseRun = false;
        mContextHelper.stopService();
    }

    // Start the sensing thread
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startRecord() {

        isGetSenseRun = true;
        mAdapterSensing.add("0 Duration, 1 Neighbors, 2 History, 3 LocAccuracy, 4 Bandwidth, 5 NetPower, 6 Battery, 7 Memory, "
                + "8 Coordinator, 9 Locator, 10 Proxy, 11 Aggregator");

        new Thread(() -> {
            while (isGetSenseRun) {
                // Wait for the sensing thread to finish
                synchronized (mLock) {
                    try {
                        mLock.wait(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Context and intent test
                HashMap context = mContextHelper.getContext();

                // Random neighbors
                //Random rand = new Random();
                //int num = rand.nextInt(4);
                //int[] neighbors = new int[]{1};
                //int[] neighbors = new int[]{1, 2};
                int[] neighbors = new int[]{1, 2, 3};

                //for (int i = 0; i < neighbors.length; i++) {
                //    neighbors[i] = rand.nextInt(2);
                //}

                HashMap intent = mContextHelper.getIntentValues(neighbors);
                runOnUiThread(() -> mAdapterSensing.add(
                        intent.get("Duration") + ", " +
                                intent.get("Neighbors") + ", " +
                                intent.get("History") + ", " +
                                intent.get("LocAccuracy") + ", " +
                                intent.get("Bandwidth") + ", " +
                                intent.get("NetPower") + ", " +
                                intent.get("Battery") + ", " +
                                intent.get("Memory") + ", " +
                                intent.get("Coordinator") + ", " +
                                intent.get("Locator") + ", " +
                                intent.get("Proxy") + ", " +
                                intent.get("Aggregator")));
            }
        }).start();
    }

    // Stop the sensing
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("SetTextI18n")
    private void stopRecord() {

        isGetSenseRun = false;

        if (mSwitchLog.isChecked()) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            final EditText editName = new EditText(this);
            editName.setText(Build.MODEL + "_" + currentTimeMillis());
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

    // Convert string array to single string
    private String arrayToString(ArrayList<String> array) {
        StringBuilder content = new StringBuilder();
        for (String line : array) {
            content.append(line).append("\n");
        }
        return content.toString();
    }

}