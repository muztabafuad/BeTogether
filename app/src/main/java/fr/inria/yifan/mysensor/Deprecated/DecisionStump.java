package fr.inria.yifan.mysensor.Deprecated;

import java.io.Serializable;

/*
 This class creates a basic decision stump
 */

public class DecisionStump implements Serializable {

    private static final String TAG = "Decision stump";
    private static final long serialVersionUID = 1001L;

    private double error; // Minimal error of prediction
    private int index; // Index of feature attribute
    private char operation; // Operation > or <=
    private double threshold; // Threshold value
    private double stepValue; // Threshold steps

    // Something for constructor
    DecisionStump() {
        error = Double.MAX_VALUE;
        index = 0;
        operation = ' ';
        threshold = 0d;
        stepValue = 0d;
    }

    // Search best decision stump from large samples
    public void BatchTrain(double[][] samples, double[] weight, int[] featureInd, int stepNum) {
        // Iteration for each possible feature
        for (int i : featureInd) {
            // For each sample find Max and Min
            double maxF_i = Double.NEGATIVE_INFINITY;
            double minF_i = Double.POSITIVE_INFINITY;
            for (double[] sample : samples) {
                maxF_i = sample[i] > maxF_i ? sample[i] : maxF_i;
                minF_i = sample[i] < minF_i ? sample[i] : minF_i;
            }
            // Increasing step of threshold for feature
            double stepValue_i = (maxF_i - minF_i) / stepNum;
            // Iteration on all possible thresholds
            for (int j = 0; j < stepNum; j++) {
                double threshold_i = minF_i + j * stepValue_i;
                double weightErrorLt = 0d; // Less than operation error
                double weightErrorGt = 0d; // Greater than operation error
                // Iteration on all samples
                for (int k = 0; k < samples.length; k++) {
                    // Weighted sum of prediction error, right prediction will give value 0
                    weightErrorLt += Math.abs(
                            (samples[k][i] <= threshold_i ? 1d : 0d) - samples[k][samples[k].length - 1]) * weight[k];
                    weightErrorGt += Math.abs(
                            (samples[k][i] > threshold_i ? 1d : 0d) - samples[k][samples[k].length - 1]) * weight[k];
                }
                // Update the minimal sum of weighted error
                if (weightErrorLt < error) {
                    error = weightErrorLt;
                    index = i;
                    operation = '(';
                    threshold = threshold_i;
                    stepValue = stepValue_i;
                }
                // Update the minimal sum of weighted error
                if (weightErrorGt < error) {
                    error = weightErrorGt;
                    index = i;
                    operation = '>';
                    threshold = threshold_i;
                    stepValue = stepValue_i;
                }
            }
        }
        // Here the decision stump is found
    }

    // The simple adjusting for threshold
    public void UpdateThreshold(double[] sample) {
        if (Predict(sample) != sample[sample.length - 1]) {
            //System.out.println("Current FE: " + index + ", OP: " + operation + ", TH: " + threshold +
            //        ", VA: " + sample[index] + ", HY: " + Predict(sample) + ", TR: " + sample[sample.length - 1]);
            switch (operation) {
                case '(':
                    threshold += stepValue;
                    break;
                case '>':
                    threshold -= stepValue;
                    break;
                default:
                    throw new IllegalArgumentException("Illegal operation: " + operation);
            }
            //System.out.println("New FE: " + index + ", OP: " + operation + ", TH: " + threshold +
            //        ", VA: " + sample[index] + ", H: " + Predict(sample) + ", TR: " + sample[sample.length - 1]);
        }
    }

    // The incremental method for threshold
    public void PoissonUpdate(double[] sample) {
        System.out.println("Current FE: " + index + ", OP: " + operation + ", TH: " + threshold +
                ", VA: " + sample[index] + ", HY: " + Predict(sample) + ", TR: " + sample[sample.length - 1]);
        switch (operation) {
            case '(':
                threshold += stepValue;
                break;
            case '>':
                threshold -= stepValue;
                break;
            default:
                throw new IllegalArgumentException("Illegal operation: " + operation);
        }
        System.out.println("New FE: " + index + ", OP: " + operation + ", TH: " + threshold +
                ", VA: " + sample[index] + ", H: " + Predict(sample) + ", TR: " + sample[sample.length - 1]);
    }

    // Predict for one new sample
    public int Predict(double[] features) {
        switch (operation) {
            case '(':
                return features[index] <= threshold ? 1 : 0;
            case '>':
                return features[index] > threshold ? 1 : 0;
            default:
                throw new IllegalArgumentException("Illegal operation: " + operation);
        }
    }

    // Get the error for this decision stump
    public double getError() {
        return error;
    }

    // Update the error for this decision stump
    public void setError(double err) {
        error = err;
    }

}