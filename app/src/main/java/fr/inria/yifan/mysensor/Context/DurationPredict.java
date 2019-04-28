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
 * This class learns and predicts the duration of an user activity / a physical environment.
 */

class DurationPredict {

    private static final String TAG = "Duration prediction";

    // Naming the learning model files to load
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

    private Context mContext;

    // Constructor
    DurationPredict(Context context) {

        mContext = context;

        // Load learning models
        File fileActivity = mContext.getFileStreamPath(MODEL_ACTIVITY);
        File fileDoor = mContext.getFileStreamPath(MODEL_DOOR);
        File fileGround = mContext.getFileStreamPath(MODEL_GROUND);

        FileInputStream fileInputStream;
        ObjectInputStream objectInputStream;
        FileOutputStream fileOutputStream;
        ObjectOutputStream objectOutputStream;

        // Check local model existence
        if (!fileActivity.exists() || !fileDoor.exists() || !fileGround.exists()) {
            try {
                // Load from assets
                fileInputStream = mContext.getAssets().openFd(MODEL_ACTIVITY).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierActivity = (LWL) objectInputStream.readObject();

                fileInputStream = mContext.getAssets().openFd(MODEL_DOOR).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierDoor = (LWL) objectInputStream.readObject();

                fileInputStream = mContext.getAssets().openFd(MODEL_GROUND).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierGround = (LWL) objectInputStream.readObject();

                objectInputStream.close();
                fileInputStream.close();

                // Save into app data
                fileOutputStream = mContext.openFileOutput(MODEL_ACTIVITY, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierActivity);

                fileOutputStream = mContext.openFileOutput(MODEL_DOOR, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierDoor);

                fileOutputStream = mContext.openFileOutput(MODEL_GROUND, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierGround);

                objectOutputStream.close();
                fileOutputStream.close();
            } catch (Exception e) {
                Log.d(TAG, "Error when loading from file: " + e);
            }
        } else {
            // Local models already exist
            try {
                // Load from app data
                fileInputStream = mContext.openFileInput(MODEL_ACTIVITY);
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierActivity = (LWL) objectInputStream.readObject();

                fileInputStream = mContext.openFileInput(MODEL_DOOR);
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierDoor = (LWL) objectInputStream.readObject();

                fileInputStream = mContext.openFileInput(MODEL_GROUND);
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
        // Map the activity string to a numeric value in training set
        int activity_idx = convertActivity(activity);

        // Create a new instance
        int day = starTime.get(Calendar.DAY_OF_WEEK);
        int hour = starTime.get(Calendar.HOUR_OF_DAY);
        int minute = starTime.get(Calendar.MINUTE);
        float duration = (Calendar.getInstance().getTimeInMillis() - starTime.getTimeInMillis()) / (60f * 1000f);
        double[] entry = new double[]{day, hour, minute, activity_idx, duration};
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
        // Map indoor binary to a numeric value in training set
        int indoor_idx = convertBinary(flag);

        // Create a new instance
        int day = starTime.get(Calendar.DAY_OF_WEEK);
        int hour = starTime.get(Calendar.HOUR_OF_DAY);
        int minute = starTime.get(Calendar.MINUTE);
        float duration = (Calendar.getInstance().getTimeInMillis() - starTime.getTimeInMillis()) / (60f * 1000f);
        double[] entry = new double[]{day, hour, minute, indoor_idx, duration};
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
        // Map underground binary to a numeric value in training set
        int underground_idx = convertBinary(flag);

        // Create a new instance
        int day = starTime.get(Calendar.DAY_OF_WEEK);
        int hour = starTime.get(Calendar.HOUR_OF_DAY);
        int minute = starTime.get(Calendar.MINUTE);
        float duration = (Calendar.getInstance().getTimeInMillis() - starTime.getTimeInMillis()) / (60f * 1000f);
        double[] entry = new double[]{day, hour, minute, underground_idx, duration};
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
        // Map activity string to a numeric value in training set
        int activity_idx = convertActivity(activity);

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
        // Map indoor binary to a numeric value in training set
        int indoor_idx = convertBinary(flag);

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
        // Map underground binary to a numeric value in training set
        int underground_idx = convertBinary(flag);

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

    // Save the updated models
    void saveModels() {
        FileOutputStream fileOutputStream;
        ObjectOutputStream objectOutputStream;
        try {
            // Save into app data
            fileOutputStream = mContext.openFileOutput(MODEL_ACTIVITY, Context.MODE_PRIVATE);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(classifierActivity);

            fileOutputStream = mContext.openFileOutput(MODEL_DOOR, Context.MODE_PRIVATE);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(classifierDoor);

            fileOutputStream = mContext.openFileOutput(MODEL_GROUND, Context.MODE_PRIVATE);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(classifierGround);

            objectOutputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            Log.d(TAG, "Error when saving model file: " + e);
        }
    }


    // Convert to numeric value for the model
    private int convertActivity(String activity) {
        switch (activity) {
            case "VEHICLE":
                return 1;
            case "BICYCLE":
                return 2;
            case "FOOT":
                return 3;
            case "STILL":
                return 4;
            default:
                return 0;
            //Log.e(TAG, "Wrong user activity! It's " + activity);
        }
    }

    // Convert to numeric value for the model
    private int convertBinary(String flag) {
        switch (flag) {
            case "True":
                return 1;
            case "False":
                return 0;
            default:
                return -1;
            //Log.e(TAG, "Wrong indoor detection! It's " + flag);
        }
    }
}
