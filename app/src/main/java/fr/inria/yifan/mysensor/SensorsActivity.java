package fr.inria.yifan.mysensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.List;

/*
* This activity provides functions including showing sensor and log sensing data.
*/

public class SensorsActivity extends AppCompatActivity {

    // Declare all views used
    private TextView mTextTitle;
    private TextView mTextMessage;
    private TextView mTextMessage2;
    private TextView mTextMessage3;

    // Declare sensors and managers
    private SensorManager mSensorManager;

    // Initially bind views
    private void bindViews() {
        mTextTitle = findViewById(R.id.title);
        mTextMessage = findViewById(R.id.message);
        mTextMessage2 = findViewById(R.id.message2);
        mTextMessage3 = findViewById(R.id.message3);
    }

    // Clear all views content
    private void initialView() {
        mTextTitle.setText(R.string.title_sensing);
        mTextMessage.setText("...");
        mTextMessage2.setText("...");
        mTextMessage3.setText("...");
    }

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);
        bindViews();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        showSensorList();
    }

    // Show all available sensors in list
    private void showSensorList() {
        List<Sensor> allSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        StringBuilder sb = new StringBuilder();
        sb.append("This device has " + allSensors.size() + " sensors (include uncalibrated), listed as:\n\n");
        int i = 0;
        for (Sensor s : allSensors) {
            i += 1;
            switch (s.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    sb.append(i + ": Accelerometer sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    sb.append(i + ": Ambient temperature sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_GAME_ROTATION_VECTOR:
                    sb.append(i + ": Game rotation vector sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                    sb.append(i + ": Geomagnetic rotation vector sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_GRAVITY:
                    sb.append(i + ": Gravity sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    sb.append(i + ":Gyroscope sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_LIGHT:
                    sb.append(i + ":Light sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    sb.append(i + ": Linear acceleration sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    sb.append(i + ": Magnetic field sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_ORIENTATION:
                    sb.append(i + ": Orientation sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_PRESSURE:
                    sb.append(i + ": Pressure sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_PROXIMITY:
                    sb.append(i + ": Proximity sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_RELATIVE_HUMIDITY:
                    sb.append(i + ": Relative humidity sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    sb.append(i + ": Rotation vector sensor " + s.getType() + ".\n");
                    break;
                case Sensor.TYPE_TEMPERATURE:
                    sb.append(i + ": Temperature sensor " + s.getType() + ".\n");
                    break;
                default:
                    sb.append(i + ": Other sensor " + s.getType() + ".\n");
                    break;
            }
            sb.append(" - Device name: " + s.getName() + "\n - Device version: " + s.getVersion() + "\n - Manufacturer: "
                    + s.getVendor() + "\n - Power consumption: " + s.getPower() + "\n\n");
        }
        mTextMessage.setText(sb.toString());
    }

}
