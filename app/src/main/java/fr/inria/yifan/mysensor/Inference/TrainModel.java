package fr.inria.yifan.mysensor.Inference;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Random;

import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_INDOOR;
import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_INPOCKET;
import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_UNDERGROUND;

public class TrainModel {

    // Main method for generating original model
    public static void main(String[] args) {

        // 1 daytime, 2 light, 3 magnetic, 4 GSM, 5 GPS accuracy, 6 GPS speed, 7 proximity
        int featuresInit[] = {1, 2, 3, 4, 5, 6, 7}; // The index of features to construct samples
        int numSamples = 40000; // Use how many samples for learning

        // 0 daytime, 1 light, 2 magnetic, 3 GSM, 4 GPS accuracy, 5 GPS speed, 6 proximity
        //String fileLoad = "C:/Users/Yifan/OneDrive/INRIA/Context Sense/Training Data/InpocketOutpocket_binary.csv";
        //String fileSave = "C:/Users/Yifan/Documents/MySensor/app/src/main/assets/" + MODEL_INPOCKET;
        //int featuresUsed[] = {1, 2, 6}; // The index of features used for training
        //String fileLoad = "C:/Users/Yifan/OneDrive/INRIA/Context Sense/Training Data/IndoorOutdoor_binary.csv";
        //String fileSave = "C:/Users/Yifan/Documents/MySensor/app/src/main/assets/" + MODEL_INDOOR;
        //int featuresUsed[] = {1, 2, 4}; // The index of features used for training
        String fileLoad = "C:/Users/Yifan/OneDrive/INRIA/Context Sense/Training Data/OngroundUnderground_binary.csv";
        String fileSave = "C:/Users/Yifan/Documents/MySensor/app/src/main/assets/" + MODEL_UNDERGROUND;
        int featuresUsed[] = {2, 3, 4}; // The index of features used for training

        CSVParser parserSample = new CSVParser(fileLoad, numSamples, featuresInit);
        parserSample.shuffleSamples(); // Shuffle the samples
        double[][] samples = parserSample.getSampleArray();
        // Initially 20% samples will be used for test
        double[][] samples_test = Arrays.copyOfRange(samples, (int) (numSamples * 0.8), numSamples);

        // 3 features * 20 threshold = 60 classifiers
        AdaBoost adaBoost = new AdaBoost(60, featuresUsed, 20);
        adaBoost.BatchTrain(samples);

        int right = 0; // True counter
        int wrong = 0; // False counter
        double ca; // Classification accuracy

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
        int numTests = 1000; // Use how many samples for testing
        //String fileTest = "C:/Users/Yifan/OneDrive/INRIA/Context Sense/20180427/Pocket_Crosscall.csv";
        //String fileTest = "C:/Users/Yifan/OneDrive/INRIA/Context Sense/20180427/Door_Crosscall.csv";
        String fileTest = "C:/Users/Yifan/OneDrive/INRIA/Context Sense/20180427/Ground_Crosscall.csv";
        CSVParser parserTest = new CSVParser(fileTest, numTests, featuresInit);
        StringBuilder logging = new StringBuilder();

        // Run multiple times
        int run = 100;
        for (int count = 0; count < run; count++) {
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
            double[][] tests = parserTest.getSampleArray();

            // Online feedback and learning
            System.out.println("Online tested on: " + tests.length);
            double ca_max = Double.NEGATIVE_INFINITY;
            int in_min = Integer.MAX_VALUE;

            for (int i = 0; i < tests.length; i++) {
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
                //System.out.println("Right inference: " + right + ", Wrong inference: " + wrong);
                //System.out.println("Classification accuracy: " + ca);
                if (ca > ca_max) {
                    ca_max = ca;
                    in_min = i;
                }

                // Opportunistic feedback on wrong inference
                Random r = new Random();
                int num = r.nextInt(100);
                int threshold = (int) (ca * 100);
                //System.out.println("Feedback possibility: " + num + " " + threshold);

                if (adaBoost.Predict(tests[i]) != tests[i][tests[i].length - 1] & num > threshold) {
                    //System.out.println("Updated");
                    //adaBoost.GreedyUpdate(sample);
                    adaBoost.OnlineUpdate(tests[i]);
                }
            }
            System.out.println("Iteration: " + (in_min) + ", Maximum CA: " + ca_max * 100);
            logging.append(in_min).append(", ").append(ca_max * 100).append("\n");
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
