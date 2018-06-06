package fr.inria.yifan.mysensor.Backup;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Random;

import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_INDOOR;

public class TrainModel {

    // Main method for generating original model
    public static void main(String[] args) {

        /*
        0 timestamp, 1 daytime (b), 2 light density (lx), 3 magnetic strength (μT), 4 GSM active (b),
        5 RSSI level, 6 GPS accuracy (m), 7 Wifi active (b), 8 Wifi RSSI (dBm), 9 proximity (b),
        10 sound level (dBA), 11 temperature (C), 12 pressure (hPa), 13 humidity (%),
        14 in-pocket label, 15 in-door label, 16 under-ground label
        */
        int featuresInit[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}; // The index of features to construct samples
        int labelInit = 15;
        int numSamples = 20000; // Use how many samples for learning

        String fileLoad = "C:/Users/Yifan/OneDrive/INRIA/Context Sense/Training Data/GT-I9505_1527175592592.csv";
        //String fileLoad = "C:/Users/Yifan/OneDrive/INRIA/Context Sense/Training Data/Redmi Note 4_1527233438107.csv";

        //String fileSave = "C:/Users/Yifan/Documents/MySensor/app/src/main/assets/" + MODEL_INPOCKET;
        //int featuresUsed[] = {2, 9}; // The index of features used for training

        String fileSave = "C:/Users/Yifan/Documents/MySensor/app/src/main/assets/" + MODEL_INDOOR;
        int featuresUsed[] = {6, 7, 8}; // The index of features used for training

        //String fileSave = "C:/Users/Yifan/Documents/MySensor/app/src/main/assets/" + MODEL_UNDERGROUND;
        //int featuresUsed[] = {2, 5, 6, 7}; // The index of features used for training

        CSVParser parserSample = new CSVParser(fileLoad, numSamples, featuresInit, labelInit);
        parserSample.shuffleSamples(); // Shuffle the samples
        double[][] samples = parserSample.getSampleArray();
        // Initially 80% samples will be used for train
        double[][] samples_train = Arrays.copyOfRange(samples, 0, (int) (numSamples * 0.8));
        // Initially 20% samples will be used for test
        double[][] samples_test = Arrays.copyOfRange(samples, (int) (numSamples * 0.8), numSamples);

        // n features * 10 threshold = 10n classifiers
        AdaBoost adaBoost = new AdaBoost(30, featuresUsed, 10);
        adaBoost.BatchTrain(samples_train);

        int right = 0; // True counter
        int wrong = 0; // False counter
        double ca; // Classification accuracy

        System.out.println("Trained from: " + samples_train.length);
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

        // Save trained model
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileSave);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(adaBoost);
            objectOutputStream.close();
        } catch (Exception e) {
            System.out.println("Error when saving to file: " + e);
        }

        // Load samples for feedback and test
        String fileTest = "C:/Users/Yifan/OneDrive/INRIA/Context Sense/Training Data/Redmi Note 4_1527233438107.csv";
        //String fileTest = "C:/Users/Yifan/OneDrive/INRIA/Context Sense/Training Data/GT-I9505_1527175592592.csv";

        CSVParser parserTest = new CSVParser(fileTest, numSamples, featuresInit, labelInit);
        StringBuilder logging = new StringBuilder();

        // Run multiple times
        int run = 100;
        for (int t = 0; t < run; t++) {
            // Load trained model
            try {
                FileInputStream fileInputStream = new FileInputStream(fileSave);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                adaBoost = (AdaBoost) objectInputStream.readObject();
                objectInputStream.close();
            } catch (Exception e) {
                System.out.println("Error when loading the file: " + e);
            }

            parserTest.shuffleSamples(); // Shuffle the samples
            double[][] new_samples = parserTest.getSampleArray();
            double[][] new_tests = Arrays.copyOfRange(new_samples, (int) (numSamples * 0.5), numSamples);

            // Online feedback and learning
            System.out.println("Online tested on: " + new_tests.length);

            right = 0;
            wrong = 0;
            for (double[] test : new_tests) {
                if (adaBoost.Predict(test) == test[test.length - 1]) {
                    right++;
                } else {
                    wrong++;
                }
            }
            double init_ca = (double) right / (right + wrong);
            //System.out.println("Initial classification accuracy: " + init_ca);

            double ca_max = Double.NEGATIVE_INFINITY;
            int i_min = Integer.MAX_VALUE;
            int i_feed = 0;

            for (double[] new_test : new_tests) {
                right = 0;
                wrong = 0;
                for (double[] test : new_tests) {
                    if (adaBoost.Predict(test) == test[test.length - 1]) {
                        right++;
                    } else {
                        wrong++;
                    }
                }
                ca = (double) right / (right + wrong);
                //System.out.println("Right inference: " + right + ", Wrong inference: " + wrong);
                //System.out.println("Classification accuracy: " + ca);
                if (ca > ca_max) {
                    ca_max = ca;
                    i_min = i_feed;
                }

                // Opportunistic feedback on wrong inference
                Random r = new Random();
                int num = r.nextInt(100);
                int threshold = (int) (ca * 100);
                //System.out.println("Feedback possibility: " + num + " " + threshold);

                if (adaBoost.Predict(new_test) != new_test[new_test.length - 1]) {
                    //System.out.println("Updated");
                    adaBoost.OnlineUpdate(new_test);
                    i_feed++;
                }
            }
            System.out.println("Iteration: " + (i_min) + ", Maximum CA: " + ca_max + ", Initial CA: " + init_ca);
            logging.append(i_min).append(", ").append(ca_max * 100).append(", ").append(init_ca * 100).append("\n");
        }

        // Save the log file
        String logfile = "C:/Users/Yifan/Documents/MySensor/app/src/main/assets/ClassificationAccuracy";
        try {
            FileOutputStream output = new FileOutputStream(logfile);
            output.write(logging.toString().getBytes());
            output.close();
        } catch (Exception e) {
            System.out.println("Error when saving to file: " + e);
        }
    }
}
