package fr.inria.yifan.mysensor.Inference;

/*
 This class implements the decision stump (weak learner) for AdaBoost.
 */

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_FILE;

public class InferHelper {

    private static final String TAG = "Inference helper";

    public InferHelper(Context context) {

        // 1 daytime, 2 light, 3 magnetic, 4 GSM, 5 GPS accuracy, 6 GPS speed, 7
        // proximity
        // Load trained model
        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor fileDescriptor = assetManager.openFd(MODEL_FILE);
            FileInputStream fileInputStream = fileDescriptor.createInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            AdaBoost mAdaBoost = (AdaBoost) objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
            Log.d(TAG, "Success in loading from file.");
        } catch (Exception e) {
            Log.d(TAG, "Error when loading from file: " + e);
        }

        // Log.d(TAG, adaBoost.toString());

        double[] sample = new double[]{15248, 1, 0.0, 78.48013, -93, 0.0, 0.0, 1.0, 28, 0.0, 1001.85876, 0.0, 1};
        // Feedback on wrong inference
        // Log.d(TAG, String.valueOf(adaBoost.Predict(sample)));
        // if (adaBoost.Predict(sample) != sample[sample.length - 1]) {
        // adaBoost.PoissonUpdate(sample);
        // }

        // Save trained model
        /*
         * try { FileOutputStream fileOutputStream = new FileOutputStream(MODEL_FILE);
         * ObjectOutputStream objectOutputStream = new
         * ObjectOutputStream(fileOutputStream);
         * objectOutputStream.writeObject(adaBoost); objectOutputStream.close(); } catch
         * (Exception e) { Log.d(TAG, "Error when saving to file: " + e); }
         */
    }

    public static void main(String[] args) {

        // 1 daytime, 2 light, 3 magnetic, 4 GSM, 5 GPS accuracy, 6 GPS speed, 7
        // proximity
        int featureInd[] = {1, 2, 3, 4, 5, 6, 7}; // The index of features to construct samples

        // Load samples for training
        String filePath1 = "C:/Users/Yifan/OneDrive/INRIA/Context Sense/Training Data/InpocketOutpocket_binary.csv";
        int numSamples = 40000; // Use how many samples for learning
        CSVParser parser1 = new CSVParser(filePath1, numSamples, featureInd);
        parser1.shuffleSamples(); // Shuffle the samples
        double samples[][] = parser1.getSampleArray();

        // 3 features * 20 threshold = 60 classifiers
        // 0 daytime, 1 light, 2 magnetic, 3 GSM, 4 GPS accuracy, 5 GPS speed 6
        // proximity
        AdaBoost adaBoost = new AdaBoost(60, new int[]{1, 2, 6}, 20);
        adaBoost.BatchTrain(samples);

        int right = 0; // True counter
        int wrong = 0; // False counter
        double ca; // Classification accuracy

        // Initially 20% samples will be used for test
        double[][] samples_test = Arrays.copyOfRange(samples, (int) (numSamples * 0.8), numSamples);
        System.out.println("Trained from: " + samples.length);
        System.out.println("Tested on: " + samples_test.length);
        for (double[] test : samples_test) {
            if (adaBoost.Predict(test) == test[test.length - 1]) {
                right++;
            } else {
                wrong++;
            }
        }
        System.out.println("Right inference: " + right + ", Wrong inference: " + wrong);
        System.out.println("Classification accuracy: " + (double) right / (right + wrong));
        System.out.println("----------------------------------------------------------------");

        String filePath = "C:/Users/Yifan/Documents/MySensor/app/src/main/java/fr/inria/yifan/mysensor/Inference/AdaBoost.model";
        // Save trained model
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(adaBoost);
            objectOutputStream.close();
        } catch (Exception e) {
            System.out.println("Error when saving to file: " + e);
        }

        // Load trained model
        try {
            FileInputStream fileInputStream = new FileInputStream(filePath);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            adaBoost = (AdaBoost) objectInputStream.readObject();
            objectInputStream.close();
        } catch (Exception e) {
            System.out.println("Error when loading from file: " + e);
        }

        // Load samples for feedback and test
        String filePath2 = "C:/Users/Yifan/OneDrive/INRIA/Context Sense/20180427/Pocket_Crosscall.csv";
        int numTests = 1000; // Use how many samples for testing
        CSVParser parser2 = new CSVParser(filePath2, numTests, featureInd);
        parser2.shuffleSamples(); // Shuffle the samples
        double tests[][] = parser2.getSampleArray();

        // Online feedback and learning
        System.out.println("Onine tested on: " + tests.length);
        StringBuilder log = new StringBuilder();
        for (double[] sample : tests) {
            right = 0;
            wrong = 0;
            for (double[] test : tests) {
                if (adaBoost.Predict(test) == test[test.length - 1]) {
                    right++;
                } else {
                    wrong++;
                }
            }
            ca = (double) right / (right + wrong);
            System.out.println("Right inference: " + right + ", Wrong inference: " + wrong);
            System.out.println("Classification accuracy: " + ca);
            log.append("Right inference, ").append(right).append(", Wrong inference, ").append(wrong).append(", Classification accuracy, ").append(ca).append("\n");
            // Feedback on wrong inference
            if (adaBoost.Predict(sample) != sample[sample.length - 1]) {
                adaBoost.OnlineUpdate(sample);
            }
        }

        // Save the log file
        String logfile = "ClassificationAccuracy.log";
        try {
            FileOutputStream output = new FileOutputStream(logfile);
            output.write(log.toString().getBytes());
            output.close();
        } catch (Exception e) {
            System.out.println("Error when saving to file: " + e);
        }
    }
}
