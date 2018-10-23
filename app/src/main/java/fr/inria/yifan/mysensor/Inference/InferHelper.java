package fr.inria.yifan.mysensor.Inference;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import fr.inria.yifan.mysensor.Deprecated.AdaBoost;
import weka.classifiers.trees.HoeffdingTree;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import static fr.inria.yifan.mysensor.Support.Configuration.DATASET_INDOOR;
import static fr.inria.yifan.mysensor.Support.Configuration.DATASET_INPOCKET;
import static fr.inria.yifan.mysensor.Support.Configuration.DATASET_UNDERGROUND;
import static fr.inria.yifan.mysensor.Support.Configuration.LAMBDA;
import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_INDOOR;
import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_INPOCKET;
import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_UNDERGROUND;

/**
 * This class represents the inference helper for environmental contexts.
 */

public class InferHelper extends BroadcastReceiver {

    private static final String TAG = "Inference helper";

    // Three empty instances for initialization
    private Instances instancesPocket;
    private Instances instancesDoor;
    private Instances instancesGround;
    private HoeffdingTree classifierPocket;
    private HoeffdingTree classifierDoor;
    private HoeffdingTree classifierGround;

    // Load the base model and instances
    public InferHelper(Context context) {
        // Check local model existence
        File filePocket = context.getFileStreamPath(MODEL_INPOCKET);
        File fileDoor = context.getFileStreamPath(MODEL_INDOOR);
        File fileGround = context.getFileStreamPath(MODEL_UNDERGROUND);

        FileInputStream fileInputStream;
        ObjectInputStream objectInputStream;
        FileOutputStream fileOutputStream;
        ObjectOutputStream objectOutputStream;

        if (!filePocket.exists() || !fileDoor.exists() || !fileGround.exists()) {
            Log.d(TAG, "Local model does not exist.");
            // Initialize trained model
            try {
                fileInputStream = context.getAssets().openFd(MODEL_INPOCKET).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierPocket = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = context.getAssets().openFd(DATASET_INPOCKET).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesPocket = (Instances) objectInputStream.readObject();

                fileInputStream = context.getAssets().openFd(MODEL_INDOOR).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierDoor = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = context.getAssets().openFd(DATASET_INDOOR).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesDoor = (Instances) objectInputStream.readObject();

                fileInputStream = context.getAssets().openFd(MODEL_UNDERGROUND).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierGround = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = context.getAssets().openFd(DATASET_UNDERGROUND).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesGround = (Instances) objectInputStream.readObject();

                objectInputStream.close();
                fileInputStream.close();
                Log.d(TAG, "Success in loading from assets.");

                fileOutputStream = context.openFileOutput(MODEL_INPOCKET, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierPocket);
                fileOutputStream = context.openFileOutput(DATASET_INPOCKET, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(instancesPocket);

                fileOutputStream = context.openFileOutput(MODEL_INDOOR, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierDoor);
                fileOutputStream = context.openFileOutput(DATASET_INDOOR, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(instancesDoor);

                fileOutputStream = context.openFileOutput(MODEL_UNDERGROUND, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierGround);
                fileOutputStream = context.openFileOutput(DATASET_UNDERGROUND, Context.MODE_PRIVATE);
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

    // Get a single result from inference
    public String inferOneResult(double[] sample) throws Exception {
        if (infer("Pocket", sample)) {
            return "In-Pocket (Do nothing)";
        } else if (!infer("Door", sample)) {
            return "Out-Door (Out-Pocket)";
        } else if (infer("Ground", sample)) {
            return "Under-ground (In-Door)";
        } else {
            return "On-ground (In-Door)";
        }
    }

    // Inference on the new instance
    public boolean infer(String model, double[] sample) throws Exception {
        double[] entry;
        Instance inst;
        switch (model) {
            case "Pocket":
                // 10 Proximity, 12 temperature, 2 light density
                entry = new double[]{sample[10], sample[12], sample[2]};
                inst = new DenseInstance(1, entry);
                inst.setDataset(instancesPocket);
                return classifierPocket.classifyInstance(inst) == 1;
            case "Door":
                // 7 GPS accuracy, 5 RSSI level, 6 RSSI value, 9 Wifi RSSI, 2 light density, 12 temperature
                entry = new double[]{sample[7], sample[5], sample[6], sample[9], sample[2], sample[12]};
                inst = new DenseInstance(1, entry);
                inst.setDataset(instancesDoor);
                return classifierDoor.classifyInstance(inst) == 1;
            case "Ground":
                // 5 RSSI level, 7 GPS accuracy (m), 12 temperature, 6 RSSI value, 13 pressure, 9 Wifi RSSI, 14 humidity
                entry = new double[]{sample[5], sample[7], sample[12], sample[6], sample[13], sample[9], sample[14]};
                inst = new DenseInstance(1, entry);
                inst.setDataset(instancesGround);
                return classifierGround.classifyInstance(inst) == 1;
            default:
                Log.e(TAG, "Wrong parameter of model type: " + model);
                return false;
        }
    }

    // Update the model by online learning
    public void updateByLabel(String model, double[] sample, boolean label) throws Exception {
        double[] entry;
        Instance inst;
        int p;
        switch (model) {
            case "Pocket":
                // 10 Proximity, 12 temperature, 2 light density
                entry = new double[]{sample[10], sample[12], sample[2], label ? 1 : 0};
                inst = new DenseInstance(1, entry);
                inst.setDataset(instancesPocket);
                p = AdaBoost.Poisson(LAMBDA);
                for (int k = 0; k < p; k++) {
                    classifierPocket.updateClassifier(inst);
                }
                break;
            case "Door":
                // 7 GPS accuracy, 5 RSSI level, 6 RSSI value, 9 Wifi RSSI, 2 light density, 12 temperature
                entry = new double[]{sample[7], sample[5], sample[6], sample[9], sample[2], sample[12], label ? 1 : 0};
                inst = new DenseInstance(1, entry);
                inst.setDataset(instancesDoor);
                p = AdaBoost.Poisson(LAMBDA);
                for (int k = 0; k < p; k++) {
                    classifierDoor.updateClassifier(inst);
                }
                break;
            case "Ground":
                // 5 RSSI level, 7 GPS accuracy (m), 12 temperature, 6 RSSI value, 13 pressure, 9 Wifi RSSI, 14 humidity
                entry = new double[]{sample[5], sample[7], sample[12], sample[6], sample[13], sample[9], sample[14], label ? 1 : 0};
                inst = new DenseInstance(1, entry);
                inst.setDataset(instancesGround);
                p = AdaBoost.Poisson(LAMBDA);
                for (int k = 0; k < p; k++) {
                    classifierGround.updateClassifier(inst);
                }
                break;
            default:
                Log.e(TAG, "Wrong parameter of model type: " + model);
                break;
        }
    }

    // Get the feedback and
    public void updateByFeedback(String feedback, double[] sample) throws Exception {
        double[] entry;
        Instance inst;
        int p;
        switch (feedback) {
            case "Out-pocket":
                // 10 Proximity, 12 temperature, 2 light density
                entry = new double[]{sample[10], sample[12], sample[2], 0};
                inst = new DenseInstance(1, entry);
                inst.setDataset(instancesPocket);
                p = AdaBoost.Poisson(LAMBDA);
                for (int k = 0; k < p; k++) {
                    classifierPocket.updateClassifier(inst);
                }
                break;
            case "In-door":
                // 7 GPS accuracy, 5 RSSI level, 6 RSSI value, 9 Wifi RSSI, 2 light density, 12 temperature
                entry = new double[]{sample[7], sample[5], sample[6], sample[9], sample[2], sample[12], 1};
                inst = new DenseInstance(1, entry);
                inst.setDataset(instancesDoor);
                p = AdaBoost.Poisson(LAMBDA);
                for (int k = 0; k < p; k++) {
                    classifierDoor.updateClassifier(inst);
                }
                break;
            case "Out-door":
                // 7 GPS accuracy, 5 RSSI level, 6 RSSI value, 9 Wifi RSSI, 2 light density, 12 temperature
                entry = new double[]{sample[7], sample[5], sample[6], sample[9], sample[2], sample[12], 0};
                inst = new DenseInstance(1, entry);
                inst.setDataset(instancesDoor);
                p = AdaBoost.Poisson(LAMBDA);
                for (int k = 0; k < p; k++) {
                    classifierDoor.updateClassifier(inst);
                }
                break;
            case "Under-ground":
                // 5 RSSI level, 7 GPS accuracy (m), 12 temperature, 6 RSSI value, 13 pressure, 9 Wifi RSSI, 14 humidity
                entry = new double[]{sample[5], sample[7], sample[12], sample[6], sample[13], sample[9], sample[14], 1};
                inst = new DenseInstance(1, entry);
                inst.setDataset(instancesGround);
                p = AdaBoost.Poisson(LAMBDA);
                for (int k = 0; k < p; k++) {
                    classifierGround.updateClassifier(inst);
                }
                break;
            case "On-ground":
                // 5 RSSI level, 7 GPS accuracy (m), 12 temperature, 6 RSSI value, 13 pressure, 9 Wifi RSSI, 14 humidity
                entry = new double[]{sample[5], sample[7], sample[12], sample[6], sample[13], sample[9], sample[14], 0};
                inst = new DenseInstance(1, entry);
                inst.setDataset(instancesGround);
                p = AdaBoost.Poisson(LAMBDA);
                for (int k = 0; k < p; k++) {
                    classifierGround.updateClassifier(inst);
                }
                break;
            default:
                Log.e(TAG, "Wrong parameter of feedback: " + feedback);
                break;
        }
    }

    // Receive user feedback from broadcast
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received: " + intent);
        String type = intent.getStringExtra("infer_type");
        Log.d(TAG, "Inference type is: " + type);
    }
}
