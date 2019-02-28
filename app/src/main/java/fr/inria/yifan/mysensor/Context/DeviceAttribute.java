package fr.inria.yifan.mysensor.Context;

import android.content.Context;

import java.util.HashMap;

public class DeviceAttribute {


    private HashMap<String, Integer> mSensorAcc;
    private HashMap<String, Float> mSensorPow;

    public DeviceAttribute(Context context) {
        mSensorAcc = new HashMap<>();
        mSensorAcc.put("Temperature", 10);
        mSensorAcc.put("Light", 10);
        mSensorAcc.put("Pressure", 10);
        mSensorAcc.put("Humidity", 10);
        mSensorAcc.put("Noise", 10);
        mSensorPow= new HashMap<>();
        mSensorPow.put("Temperature", 0.1f);
        mSensorPow.put("Light", 0.1f);
        mSensorPow.put("Pressure", 0.1f);
        mSensorPow.put("Humidity", 0.1f);
        mSensorPow.put("Noise", 0.1f);
    }

}
