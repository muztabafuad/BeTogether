package fr.inria.yifan.mysensor.Sensing;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import static fr.inria.yifan.mysensor.Support.Configuration.INTERCEPT;
import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_NUM_WINDOW;
import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_RATE_IN_HZ;
import static fr.inria.yifan.mysensor.Support.Configuration.SLOPE;

/**
 * This class provides functions including initialize and reading data from sensors.
 */

public class SensorsHelper {

    private static final String TAG = "Sensors helper";

    // Audio recorder parameters for sampling
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);

    // Declare sensors and recorder
    private AudioRecord mAudioRecord;
    private AWeighting mAWeighting;
    private Sensor mSensorLight;
    private Sensor mSensorProxy;
    private Sensor mSensorTemp;
    private Sensor mSensorPress;
    private Sensor mSensorHumid;
    private Sensor mSensorMagnet;
    private SensorManager mSensorManager;

    // Declare sensing variables
    private SlideWindow mLight;
    private SlideWindow mProximity;
    private SlideWindow mTemperature;
    private SlideWindow mPressure;
    private SlideWindow mHumidity;
    private SlideWindow mMagnet;

    // Declare light sensor listener
    private SensorEventListener mListenerLight = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mLight.putValue(sensorEvent.values[0]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            //PASS
        }
    };

    // Declare magnetic sensor listener
    private SensorEventListener mListenerMagnet = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mMagnet.putValue((float) Math.sqrt(Math.pow(sensorEvent.values[0], 2) + Math.pow(sensorEvent.values[1], 2) + Math.pow(sensorEvent.values[2], 2)));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            //PASS
        }
    };

    // Declare proximity sensor listener
    private SensorEventListener mListenerProxy = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mProximity.putValue(sensorEvent.values[0]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            //PASS
        }
    };

    // Declare temperature sensor listener
    private SensorEventListener mListenerTemp = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mTemperature.putValue(sensorEvent.values[0]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            //PASS
        }
    };

    // Declare pressure sensor listener
    private SensorEventListener mListenerPress = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mPressure.putValue(sensorEvent.values[0]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            //PASS
        }
    };

    // Declare humidity sensor listener
    private SensorEventListener mListenerHumid = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mHumidity.putValue(sensorEvent.values[0]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            //PASS
        }
    };

    // Register the broadcast receiver with the intent values to be matched
    public SensorsHelper(Activity activity) {

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        mAWeighting = new AWeighting(SAMPLE_RATE_IN_HZ);
        //Log.d(TAG, "Buffer size = " + BUFFER_SIZE);

        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorMagnet = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorProxy = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorTemp = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mSensorPress = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mSensorHumid = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
    }

    // Check if location service on system is enabled
    public void startService() {
        mAudioRecord.startRecording();

        mLight = new SlideWindow(SAMPLE_NUM_WINDOW, 0);
        mMagnet = new SlideWindow(SAMPLE_NUM_WINDOW, 0);
        mProximity = new SlideWindow(SAMPLE_NUM_WINDOW, mSensorProxy.getMaximumRange());
        mTemperature = new SlideWindow(SAMPLE_NUM_WINDOW, 0);
        mPressure = new SlideWindow(SAMPLE_NUM_WINDOW, 0);
        mHumidity = new SlideWindow(SAMPLE_NUM_WINDOW, 0);

        // Register listeners for all environmental sensors
        mSensorManager.registerListener(mListenerLight, mSensorLight, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mListenerMagnet, mSensorMagnet, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mListenerProxy, mSensorProxy, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mListenerTemp, mSensorTemp, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mListenerPress, mSensorPress, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mListenerHumid, mSensorHumid, SensorManager.SENSOR_DELAY_FASTEST);
    }

    // Unregister the broadcast receiver and listeners
    public void stopService() {
        mAudioRecord.stop();
        mSensorManager.unregisterListener(mListenerLight);
        mSensorManager.unregisterListener(mListenerMagnet);
        mSensorManager.unregisterListener(mListenerProxy);
        mSensorManager.unregisterListener(mListenerTemp);
        mSensorManager.unregisterListener(mListenerPress);
        mSensorManager.unregisterListener(mListenerHumid);
    }

    // Get the most recent sound level
    public int getSoundLevel() {
        short[] buffer = new short[BUFFER_SIZE];
        // r is the real measurement data length, normally r is less than buffersize
        int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
        // Apply A-weighting filtering
        buffer = mAWeighting.apply(buffer);
        long v = 0;
        // Get content from buffer and calculate square sum
        for (short aBuffer : buffer) {
            v += aBuffer * aBuffer;
        }
        // Square sum divide by data length to get volume
        double mean = v / (double) r;
        //Log.d(TAG, "Sound value: " + mean);
        final double volume = 10 * Math.log10(mean);
        //Log.d(TAG, "Sound dB value: " + volume);
        return (int) (volume * SLOPE + INTERCEPT);
    }

    // Get the most recent light density
    public float getLightDensity() {
        return mLight.getMean();
    }

    // Get thr most recent proximity value
    public float getProximity() {
        return mProximity.getMean();
    }

    // Get the most recent temperature
    public float getTemperature() {
        return mTemperature.getMean();
    }

    // Get the most recent pressure
    public float getPressure() {
        return mPressure.getMean();
    }

    // Get the most recent humidity
    public float getHumidity() {
        return mHumidity.getMean();
    }

    // Get the most recent magnet field
    public float getMagnet() {
        return mMagnet.getMean();
    }

    // Manually update sliding window
    public void updateWindow() {
        mLight.putValue(mLight.getLast());
        mMagnet.putValue(mMagnet.getLast());
        mProximity.putValue(mProximity.getLast());
        mTemperature.putValue(mTemperature.getLast());
        mPressure.putValue(mPressure.getLast());
        mHumidity.putValue(mHumidity.getLast());
    }

    // Simple In/Out-pocket detection function
    /*public boolean isInPocket() {
        //Toast.makeText(this, "In-pocket", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this, "Out-pocket", Toast.LENGTH_SHORT).show();
        return mProximity == 0 && mLight < 10;
    }*/

    // Simple Indoor/Outdoor detection function
    /*public boolean isInDoor() {
        //Toast.makeText(this, "Indoor", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this, "Outdoor", Toast.LENGTH_SHORT).show();
        if (isDaytime() == 1) {
            return mLight < 1500;
        } else {
            return mLight > 10;
        }
    }*/
}
