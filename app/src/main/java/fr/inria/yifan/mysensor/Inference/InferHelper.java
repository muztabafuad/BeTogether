package fr.inria.yifan.mysensor.Inference;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import weka.classifiers.trees.HoeffdingTree;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import static fr.inria.yifan.mysensor.Support.Configuration.DATASET_INDOOR;
import static fr.inria.yifan.mysensor.Support.Configuration.DATASET_INPOCKET;
import static fr.inria.yifan.mysensor.Support.Configuration.DATASET_UNDERGROUND;
import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_INDOOR;
import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_INPOCKET;
import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_UNDERGROUND;

/**
 * This class represents the inference helper for environmental contexts.
 */

public class InferHelper {

    private static final String TAG = "Inference helper";

    private Context mContext;
    // Three empty instances for initialization
    private Instances instancesPocket;
    private Instances instancesDoor;
    private Instances instancesGround;
    private HoeffdingTree classifierPocket;
    private HoeffdingTree classifierDoor;
    private HoeffdingTree classifierGround;

    // Load the base model and instances
    public InferHelper(Context context) {
        mContext = context;
        // Check local model existence
        File filePocket = mContext.getFileStreamPath(MODEL_INPOCKET);
        File fileDoor = mContext.getFileStreamPath(MODEL_INDOOR);
        File fileGround = mContext.getFileStreamPath(MODEL_UNDERGROUND);

        FileInputStream fileInputStream;
        ObjectInputStream objectInputStream;
        FileOutputStream fileOutputStream;
        ObjectOutputStream objectOutputStream;

        if (!filePocket.exists() || !fileDoor.exists() || !fileGround.exists()) {
            Log.d(TAG, "Local model does not exist.");
            // Initialize trained model
            try {
                fileInputStream = mContext.getAssets().openFd(MODEL_INPOCKET).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierPocket = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = mContext.getAssets().openFd(DATASET_INPOCKET).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesPocket = (Instances) objectInputStream.readObject();

                fileInputStream = mContext.getAssets().openFd(MODEL_INDOOR).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierDoor = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = mContext.getAssets().openFd(DATASET_INDOOR).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesDoor = (Instances) objectInputStream.readObject();

                fileInputStream = mContext.getAssets().openFd(MODEL_UNDERGROUND).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierGround = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = mContext.getAssets().openFd(DATASET_UNDERGROUND).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesGround = (Instances) objectInputStream.readObject();

                objectInputStream.close();
                fileInputStream.close();
                Log.d(TAG, "Success in loading from assets.");

                fileOutputStream = mContext.openFileOutput(MODEL_INPOCKET, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierPocket);
                fileOutputStream = mContext.openFileOutput(DATASET_INPOCKET, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(instancesPocket);

                fileOutputStream = mContext.openFileOutput(MODEL_INDOOR, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierDoor);
                fileOutputStream = mContext.openFileOutput(DATASET_INDOOR, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(instancesDoor);

                fileOutputStream = mContext.openFileOutput(MODEL_UNDERGROUND, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierGround);
                fileOutputStream = mContext.openFileOutput(DATASET_UNDERGROUND, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(instancesGround);

                objectOutputStream.close();
                fileOutputStream.close();
                Log.d(TAG, "Success in saving into file.");
            } catch (Exception e) {
                Log.d(TAG, "Error when loading from file: " + e);
            }
        } else {
            Log.d(TAG, "Local model already exist.");
            try {
                fileInputStream = context.openFileInput(MODEL_INPOCKET);
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierPocket = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = context.openFileInput(DATASET_INPOCKET);
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesPocket = (Instances) objectInputStream.readObject();

                fileInputStream = context.openFileInput(MODEL_INDOOR);
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierDoor = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = context.openFileInput(DATASET_INDOOR);
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesDoor = (Instances) objectInputStream.readObject();

                fileInputStream = context.openFileInput(MODEL_UNDERGROUND);
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierGround = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = context.openFileInput(DATASET_UNDERGROUND);
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesGround = (Instances) objectInputStream.readObject();

                objectInputStream.close();
                fileInputStream.close();
                Log.d(TAG, "Success in loading from file.");
            } catch (Exception e) {
                Log.d(TAG, "Error when loading model file: " + e);
            }
        }
    }

    public boolean inferTrigger() {
        return false;
    }

    public boolean inferInPocket(double[] sample) throws Exception {
        // 10 Proximity, 12 temperature, 2 light density
        double[] entry = new double[]{sample[10], sample[12], sample[2]};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesPocket);
        return classifierPocket.classifyInstance(inst) == 1;
    }

    public boolean inferInDoor(double[] sample) throws Exception {
        // 7 GPS accuracy, 5 RSSI level, 6 RSSI value, 2 light density, 12 temperature
        double[] entry = new double[]{sample[7], sample[5], sample[6], sample[2], sample[12]};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesDoor);
        return classifierDoor.classifyInstance(inst) == 1;
    }

    public boolean inferUnderGround(double[] sample) throws Exception {
        // 5 RSSI level, 7 GPS accuracy (m), 12 temperature, 6 RSSI value, 13 pressure, 9 Wifi RSSI, 14 humidity
        double[] entry = new double[]{sample[5], sample[7], sample[12], sample[6], sample[13], sample[9], sample[14]};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesGround);
        return classifierGround.classifyInstance(inst) == 1;
    }

    public void updateModel(String model, double[] sample) {
        switch (model) {
            case "Pocket":
                break;
            case "Door":
                break;
            case "Ground":
            default:
                Log.e(TAG, "Wrong parameter of model type: " + model);
        }
    }

}
