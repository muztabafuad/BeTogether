package fr.inria.yifan.mysensor;

import android.Manifest;
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

/*
* This activity provides functions including showing sensor and log sensing data.
*/

public class SoundActivity extends AppCompatActivity {

    private static final String TAG = "Sound measurement";

    // Declare microphone permissions
    private static final int PERMS_REQUEST_RECORD = 1000;
    private static final String[] RECORD_PERMS = {Manifest.permission.RECORD_AUDIO};

    // Audio recorder parameters for sampling
    private static final int SAMPLE_RATE_IN_HZ = 8000;
    private final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);

    // Thread locker and running flag
    private final Object mLock;
    private AudioRecord mAudioRecord;
    private boolean isGetVoiceRun;

    // File helper and string data
    private FileHelper fileHelper;
    private ArrayList<String> sensingData;

    // Declare all used views
    private ListView listView;
    private Button startButton;
    private Button stopButton;
    private ArrayAdapter<String> adapterSensing;

    // Constructor initializes locker
    public SoundActivity() {
        mLock = new Object();
    }

    // Initially bind all views
    private void bindViews() {
        listView = findViewById(R.id.list_view);
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
        stopButton.setVisibility(View.INVISIBLE);

        // Build an adapter to feed the list with the content of an array of strings
        sensingData = new ArrayList<>();
        adapterSensing = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sensingData);
        adapterSensing.add("Sound level:");

        // Attache the adapter to the list view
        listView.setAdapter(adapterSensing);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecord();
                startButton.setVisibility(View.INVISIBLE);
                stopButton.setVisibility(View.VISIBLE);
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecord();
                startButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.INVISIBLE);
                adapterSensing.clear();
                adapterSensing.add("Sound level:");
            }
        });
    }

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound);
        fileHelper = new FileHelper(this);
        bindViews();
        checkPermission();
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
                    //Log.d(TAG, sensingData.toString());
                    if (isGetVoiceRun) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                adapterSensing.add("Time: " + System.currentTimeMillis() + ", Volume: " + (int) volume + "dB");
                            }
                        });
                    }
                    // 10 times per second
                    synchronized (mLock) {
                        try {
                            mLock.wait(100);
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
        for (String line : sensingData.subList(1, sensingData.size())) {
            text.append(line).append("\n");
        }
        //Log.d(TAG, "Now is " + time);
        try {
            fileHelper.savaFile(time, text.toString());
            Toast.makeText(this, "Sensing data saved to file", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
