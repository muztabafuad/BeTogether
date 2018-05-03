package fr.inria.yifan.mysensor.Inference;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Random;

import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_POCKET;

// This class parse a csv file into double array
class CSVParser {

    private double samples[][]; // All samples stored in an array

    private CSVParser(String filePath, int numSamples, int[] featureInd) {
        // A sample has several features and only 1 label
        samples = new double[numSamples][featureInd.length + 1];
        // Read the CSV file into array of samples
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            reader.readLine(); // Sample title is skipped here
            String line;
            int i = 0; // Samples reader counter
            while ((line = reader.readLine()) != null & i < numSamples) {
                // Split the a csv line by ','
                String item[] = line.split(",");
                int j = 0; // Feature index for a sample
                // Extract features according to given index set
                for (int feature : featureInd) {
                    samples[i][j] = Double.parseDouble(item[feature]);
                    j++;
                }
                // And extract the label into each sample
                samples[i][samples[i].length - 1] = Double.parseDouble(item[item.length - 1]);
                i++;
            }
            reader.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    // Randomize the samples
    private void shuffleSamples() {
        // Implementing Fisher–Yates shuffle
        Random random = new Random();
        for (int i = samples.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            // Simple swap
            double[] tmp = samples[index];
            samples[index] = samples[i];
            samples[i] = tmp;
        }
    }

    private double[][] getSampleArray() {
        return samples;
    }

    // Main method for generating original model
    public static void main(String[] args) {

        // 1 daytime, 2 light, 3 magnetic, 4 GSM, 5 GPS accuracy, 6 GPS speed, 7 proximity
        int featureInd[] = {1, 2, 3, 4, 5, 6, 7}; // The index of features to construct samples

        // Load samples for training
        String filePath1 = "C:/Users/Yifan/OneDrive/INRIA/Context Sense/Training Data/InpocketOutpocket_binary.csv";
        int numSamples = 40000; // Use how many samples for learning
        CSVParser parser1 = new CSVParser(filePath1, numSamples, featureInd);
        parser1.shuffleSamples(); // Shuffle the samples
        double samples[][] = parser1.getSampleArray();

        // 3 features * 20 threshold = 60 classifiers
        // 0 daytime, 1 light, 2 magnetic, 3 GSM, 4 GPS accuracy, 5 GPS speed, 6 proximity
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

        String filePath = "C:/Users/Yifan/Documents/MySensor/app/src/main/java/fr/inria/yifan/mysensor/Inference/" + MODEL_POCKET;
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
        System.out.println("Online tested on: " + tests.length);
        StringBuilder logging = new StringBuilder();
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
            logging.append("Right inference, ").append(right).append(", Wrong inference, ").append(wrong).append(", Classification accuracy, ").append(ca).append("\n");
            // Feedback on wrong inference
            if (adaBoost.Predict(sample) != sample[sample.length - 1]) {
                adaBoost.OnlineUpdate(sample);
            }
        }

        // Save the log file
        String logfile = "ClassificationAccuracy.log";
        try {
            FileOutputStream output = new FileOutputStream(logfile);
            output.write(logging.toString().getBytes());
            output.close();
        } catch (Exception e) {
            System.out.println("Error when saving to file: " + e);
        }
    }

}
