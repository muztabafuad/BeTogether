package fr.inria.yifan.mysensor.Context;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;

import weka.classifiers.lazy.LWL;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * This class learn and predict the duration of an user activity / a physical environment.
 */

class DurationPredict {

    private static final String TAG = "Duration prediction";

    // Define the learning model files to load
    private static final String MODEL_ACTIVITY = "UA_prediction.model";
    private static final String MODEL_DOOR = "Door_prediction.model";
    private static final String MODEL_GROUND = "Ground_prediction.model";

    // Instances for data set declaration
    private Instances instancesUA;
    private Instances instancesDoor;
    private Instances instancesGround;

    // LWL classifiers (regression) declaration
    private LWL classifierActivity;
    private LWL classifierDoor;
    private LWL classifierGround;

    // Constructor
    DurationPredict(Context context) {
        // Load learning models
        File fileActivity = context.getFileStreamPath(MODEL_ACTIVITY);
        File fileDoor = context.getFileStreamPath(MODEL_DOOR);
        File fileGround = context.getFileStreamPath(MODEL_GROUND);

        FileInputStream fileInputStream;
        ObjectInputStream objectInputStream;
        FileOutputStream fileOutputStream;
        ObjectOutputStream objectOutputStream;

        // Check local model existence
        if (!fileActivity.exists() || !fileDoor.exists() || !fileGround.exists()) {
            try {
                fileInputStream = context.getAssets().openFd(MODEL_ACTIVITY).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierActivity = (LWL) objectInputStream.readObject();

                fileInputStream = context.getAssets().openFd(MODEL_DOOR).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierDoor = (LWL) objectInputStream.readObject();

                fileInputStream = context.getAssets().openFd(MODEL_GROUND).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierGround = (LWL) objectInputStream.readObject();

                objectInputStream.close();
                fileInputStream.close();

                fileOutputStream = context.openFileOutput(MODEL_ACTIVITY, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierActivity);

                fileOutputStream = context.openFileOutput(MODEL_DOOR, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierDoor);

                fileOutputStream = context.openFileOutput(MODEL_GROUND, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierGround);

                objectOutputStream.close();
                fileOutputStream.close();
            } catch (Exception e) {
                Log.d(TAG, "Error when loading from file: " + e);
            }
        } else {
            // Local models already exist.
            try {
                fileInputStream = context.openFileInput(MODEL_ACTIVITY);
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierActivity = (LWL) objectInputStream.readObject();

                fileInputStream = context.openFileInput(MODEL_DOOR);
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierDoor = (LWL) objectInputStream.readObject();

                fileInputStream = context.openFileInput(MODEL_GROUND);
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierGround = (LWL) objectInputStream.readObject();

                objectInputStream.close();
                fileInputStream.close();
            } catch (Exception e) {
                Log.d(TAG, "Error when loading model file: " + e);
            }
        }

        // Data set description initialization
        // Day 1-7; Hour 0-23; Minute 0-59; UA 1 "VEHICLE", 2 "BICYCLE", 3 "FOOT", 4 "STILL"; Duration in minutes
        ArrayList<Attribute> attInfo1 = new ArrayList<>();
        attInfo1.add(new Attribute("Day"));
        attInfo1.add(new Attribute("Hour"));
        attInfo1.add(new Attribute("Minute"));
        attInfo1.add(new Attribute("UA"));
        attInfo1.add(new Attribute("Duration"));
        instancesUA = new Instances("UserActivity", attInfo1, 0);
        instancesUA.setClassIndex(instancesUA.numAttributes() - 1);

        // Day 1-7; Hour 0-23; Minute 0-59; Indoor 0 or 1; Duration in minutes
        ArrayList<Attribute> attInfo2 = new ArrayList<>();
        attInfo2.add(new Attribute("Day"));
        attInfo2.add(new Attribute("Hour"));
        attInfo2.add(new Attribute("Minute"));
        attInfo2.add(new Attribute("Indoor"));
        attInfo2.add(new Attribute("Duration"));
        instancesDoor = new Instances("IndoorDetection", attInfo2, 0);
        instancesDoor.setClassIndex(instancesDoor.numAttributes() - 1);

        // Day 1-7; Hour 0-23; Minute 0-59; Underground 0 or 1; Duration in minutes
        ArrayList<Attribute> attInfo3 = new ArrayList<>();
        attInfo3.add(new Attribute("Day"));
        attInfo3.add(new Attribute("Hour"));
        attInfo3.add(new Attribute("Minute"));
        attInfo3.add(new Attribute("Underground"));
        attInfo3.add(new Attribute("Duration"));
        instancesGround = new Instances("UndergroundDetection", attInfo3, 0);
        instancesGround.setClassIndex(instancesGround.numAttributes() - 1);
    }

    // Day 1-7; Hour 0-23; Minute 0-59; UA 1 "VEHICLE", 2 "BICYCLE", 3 "FOOT", 4 "STILL"; Duration in minutes
    void updateActivityModel(Calendar starTime, String activity) {
        // Map activity to a numeric value in training set
        int activity_idx;
        switch (activity) {
            case "VEHICLE":
                activity_idx = 1;
                break;
            case "BICYCLE":
                activity_idx = 2;
                break;
            case "FOOT":
                activity_idx = 3;
                break;
            case "STILL":
                activity_idx = 4;
                break;
            default:
                activity_idx = 0;
                //Log.e(TAG, "Wrong user activity! It's " + activity);
                break;
        }
        // Create a new instance
        int day = starTime.get(Calendar.DAY_OF_WEEK);
        int hour = starTime.get(Calendar.HOUR_OF_DAY);
        int minute = starTime.get(Calendar.MINUTE);
        float minutes = (Calendar.getInstance().getTimeInMillis() - starTime.getTimeInMillis()) / (60f * 1000f);
        double[] entry = new double[]{day, hour, minute, activity_idx, minutes};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesUA);
        try {
            classifierActivity.updateClassifier(inst);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Day 1-7; Hour 0-23; Minute 0-59; Indoor 0 or 1; Duration in minute
    void updateDoorModel(Calendar starTime, String flag) {
        // Map binary to a numeric value in training set
        int indoor_idx;
        switch (flag) {
            case "True":
                indoor_idx = 1;
                break;
            case "False":
                indoor_idx = 0;
                break;
            default:
                indoor_idx = -1;
                //Log.e(TAG, "Wrong indoor detection! It's " + flag);
                break;
        }
        // Create a new instance
        int day = starTime.get(Calendar.DAY_OF_WEEK);
        int hour = starTime.get(Calendar.HOUR_OF_DAY);
        int minute = starTime.get(Calendar.MINUTE);
        float minutes = (Calendar.getInstance().getTimeInMillis() - starTime.getTimeInMillis()) / (60f * 1000f);
        double[] entry = new double[]{day, hour, minute, indoor_idx, minutes};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesDoor);
        try {
            classifierDoor.updateClassifier(inst);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Day 1-7; Hour 0-23; Minute 0-59; Underground 0 or 1; Duration in minute
    void updateGroundModel(Calendar starTime, String flag) {
        // Map binary to a numeric value in training set
        int underground_idx;
        switch (flag) {
            case "True":
                underground_idx = 1;
                break;
            case "False":
                underground_idx = 0;
                break;
            default:
                underground_idx = -1;
                //Log.e(TAG, "Wrong underground detection! It's " + flag);
                break;
        }
        // Create a new instance
        int day = starTime.get(Calendar.DAY_OF_WEEK);
        int hour = starTime.get(Calendar.HOUR_OF_DAY);
        int minute = starTime.get(Calendar.MINUTE);
        float minutes = (Calendar.getInstance().getTimeInMillis() - starTime.getTimeInMillis()) / (60f * 1000f);
        double[] entry = new double[]{day, hour, minute, underground_idx, minutes};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesGround);
        try {
            classifierGround.updateClassifier(inst);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Day 1-7; Hour 0-23; Minute 0-59; UA 1 "VEHICLE", 2 "BICYCLE", 3 "FOOT", 4 "STILL"; label 0
    float predictActivityDuration(String activity) {
        // Map activity to a numeric value in training set
        int activity_idx;
        switch (activity) {
            case "VEHICLE":
                activity_idx = 1;
                break;
            case "BICYCLE":
                activity_idx = 2;
                break;
            case "FOOT":
                activity_idx = 3;
                break;
            case "STILL":
                activity_idx = 4;
                break;
            default:
                activity_idx = 0;
                //Log.e(TAG, "Wrong user activity! It's " + activity);
                break;
        }
        // Prediction on new instance
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_WEEK);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        double[] entry = new double[]{day, hour, minute, activity_idx, 0};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesUA);
        try {
            return (float) classifierActivity.classifyInstance(inst);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Day 1-7; Hour 0-23; Minute 0-59; Indoor 0 or 1; label 0
    float predictDoorDuration(String flag) {
        // Map binary to a numeric value in training set
        int indoor_idx;
        switch (flag) {
            case "True":
                indoor_idx = 1;
                break;
            case "False":
                indoor_idx = 0;
                break;
            default:
                indoor_idx = -1;
                //Log.e(TAG, "Wrong indoor detection! It's " + flag);
                break;
        }
        // Prediction on new instance
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_WEEK);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        double[] entry = new double[]{day, hour, minute, indoor_idx, 0};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesDoor);
        try {
            return (float) classifierDoor.classifyInstance(inst);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Day 1-7; Hour 0-23; Minute 0-59; Underground 0 or 1; label 0
    float predictGroundDuration(String flag) {
        // Map binary to a numeric value in training set
        int underground_idx;
        assert flag != null;
        switch (flag) {
            case "True":
                underground_idx = 1;
                break;
            case "False":
                underground_idx = 0;
                break;
            default:
                underground_idx = -1;
                //Log.e(TAG, "Wrong underground detection! It's " + flag);
                break;
        }
        // Prediction on new instance
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_WEEK);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        double[] entry = new double[]{day, hour, minute, underground_idx, 0};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesGround);
        try {
            return (float) classifierGround.classifyInstance(inst);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
