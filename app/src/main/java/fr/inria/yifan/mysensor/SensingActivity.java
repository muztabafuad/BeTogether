package fr.inria.yifan.mysensor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import fr.inria.yifan.mysensor.Support.FilesIOHelper;
import fr.inria.yifan.mysensor.Support.SensorsHelper;

import static fr.inria.yifan.mysensor.Support.Configuration.PERMS_REQUEST_RECORD;
import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_DELAY_IN_MS;
import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_RATE_IN_HZ;

/*
* This activity provides functions including showing sensor and log sensing data.
*/

public class SensingActivity extends AppCompatActivity {

    private static final String TAG = "Sound activity";

    // Declare microphone permissions
    private static final String[] RECORD_PERMS = {Manifest.permission.RECORD_AUDIO};

    // Audio recorder parameters for sampling
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);

    // Thread locker and running flag
    private final Object mLock;
    private AudioRecord mAudioRecord;
    private boolean isGetVoiceRun;

    // Declare all related views
    private Button mStartButton;
    private Button mStopButton;
    private ArrayAdapter<String> mAdapterSensing;

    // File helper and string data
    private FilesIOHelper mFilesIOHelper;
    private ArrayList<String> mSensingData;

    // Sensors helper for sensor and GPS
    private SensorsHelper mSensorHelper;

    // Constructor initializes locker
    public SensingActivity() {
        mLock = new Object();
    }

    // Initially bind all views
    private void bindViews() {
        mStartButton = findViewById(R.id.start_button);
        mStopButton = findViewById(R.id.stop_button);
        mStopButton.setVisibility(View.INVISIBLE);

        // Build an adapter to feed the list with the content of an array of strings
        mSensingData = new ArrayList<>();
        mAdapterSensing = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mSensingData);

        // Attache the adapter to the list view
        ListView listView = findViewById(R.id.list_view);
        listView.setAdapter(mAdapterSensing);

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAdapterSensing.clear();
                mAdapterSensing.add("Timestamp, sound level (dB) and in-pocket flag:");
                startRecord();
                mStartButton.setVisibility(View.INVISIBLE);
                mStopButton.setVisibility(View.VISIBLE);
            }
        });
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecord();
                mStartButton.setVisibility(View.VISIBLE);
                mStopButton.setVisibility(View.INVISIBLE);
            }
        });
    }

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensing);
        mFilesIOHelper = new FilesIOHelper(this);
        bindViews();
        checkPermission();
    }

    // Stop thread when exit!
    @Override
    protected void onPause() {
        isGetVoiceRun = false;
        super.onPause();
    }

    // Check related user permissions
    private void checkPermission() {
        // Check user permission for microphone
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Requesting Microphone permission", Toast.LENGTH_SHORT).show();
            // Request permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(RECORD_PERMS, PERMS_REQUEST_RECORD);
            } else {
                Toast.makeText(this, "Please give Microphone permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // Callback for user allowing permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMS_REQUEST_RECORD: {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, String.valueOf(grantResults[0]));
                    Toast.makeText(this, "Please give Microphone permission", Toast.LENGTH_SHORT).show();
                    checkPermission();
                }
            }
        }
    }

    // Start the sound sensing
    private void startRecord() {
        if (isGetVoiceRun) {
            Log.e(TAG, "Still in recording");
            return;
        }
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        mSensorHelper = new SensorsHelper(this);
        isGetVoiceRun = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();
                short[] buffer = new short[BUFFER_SIZE];
                while (isGetVoiceRun) {
                    // r is the real measurement data, normally r is less than buffersize
                    int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                    long v = 0;
                    // Get content from buffer and calculate square sum
                    for (short aBuffer : buffer) {
                        v += aBuffer * aBuffer;
                    }
                    // Square sum divide by data length to get volume
                    double mean = v / (double) r;
                    final double volume = 10 * Math.log10(mean);
                    Log.d(TAG, "Sound dB value: " + volume);
                    //Log.d(TAG, mSensingData.toString());
                    if (isGetVoiceRun) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mAdapterSensing.add(System.currentTimeMillis() + ", " + (int) volume + ", " + mSensorHelper.isInPocket());
                            }
                        });
                    }
                    // 10 times per second
                    synchronized (mLock) {
                        try {
                            mLock.wait(SAMPLE_DELAY_IN_MS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }).start();
    }

    // Stop the sound sensing
    private void stopRecord() {
        isGetVoiceRun = false;
        String time = String.valueOf(System.currentTimeMillis());
        StringBuilder text = new StringBuilder();
        for (String line : mSensingData.subList(1, mSensingData.size())) {
            text.append(line).append("\n");
        }
        //Log.d(TAG, "Now is " + time);
        try {
            mFilesIOHelper.saveFile(time, text.toString());
            Toast.makeText(this, "Sensing data saved to file", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Go to the sensing activity
    public void goDetection(View view) {
        Intent goToDetection = new Intent();
        goToDetection.setClass(this, DetectionActivity.class);
        startActivity(goToDetection);
        finish();
    }

}
