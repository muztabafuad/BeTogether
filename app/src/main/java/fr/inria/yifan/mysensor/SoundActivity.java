package fr.inria.yifan.mysensor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

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

    // Constructor initializes locker
    public SoundActivity() {
        mLock = new Object();
    }

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound);
        senseSound();
    }

    // Start to sense the sound
    private void senseSound() {
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
        } else {
            getNoiseLevel();
        }
    }

    // Callback for user allowing permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMS_REQUEST_RECORD: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getNoiseLevel();
                } else {
                    Toast.makeText(this, "Please give Microphone permission", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    private void startRecord() {
        //这里我们使用私有模式,创建出来的文件只能被本应用访问,还会覆盖原文件哦
        //FileOutputStream output = this.openFileOutput(filename, this.MODE_PRIVATE);
        //output.write(filecontent.getBytes());
        //output.close();
    }

    // Get the sound level
    private void getNoiseLevel() {
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
                    // get content from buffer and calculate square sum
                    for (short aBuffer : buffer) {
                        v += aBuffer * aBuffer;
                    }
                    // square sum divide by data length to get volume
                    double mean = v / (double) r;
                    double volume = 10 * Math.log10(mean);
                    Log.d(TAG, "Sound dB value: " + volume);
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
}
