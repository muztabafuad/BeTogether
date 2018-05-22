package fr.inria.yifan.mysensor.Inference;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Random;

// This class parse a csv file into double array
class CSVParser {

    private double samples[][]; // All samples stored in an array

    CSVParser(String filePath, int numSamples, int[] featureInd, int labelInd) {
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
                samples[i][samples[i].length - 1] = Double.parseDouble(item[labelInd]);
                i++;
            }
            reader.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    // Randomize the samples
    public void shuffleSamples() {
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

    public double[][] getSampleArray() {
        return samples;
    }

}
