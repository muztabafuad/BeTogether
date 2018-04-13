package fr.inria.yifan.mysensor.AdaBoost;

/**
 * This class implements the decision stump (weak learner) for AdaBoost.
 */

public class DecisionStump {

    private int index; // Index of attribute
    private char operation; // Operation '> <='
    private float threshold; // Threshold value

    public DecisionStump(float[] attributes) {

    }

    public void Train(float[][] samples, float[] weight) {
        float minError = Float.MAX_VALUE;
        // For each feature
        for (int i = 0; i < samples[0].length - 1; i++) {
            float maxF_i = Float.MIN_VALUE;
            float minF_i = Float.MAX_VALUE;
            // For each sample
            for (float[] sample : samples) {
                maxF_i = sample[i] > maxF_i ? sample[i] : maxF_i;
                minF_i = sample[i] < minF_i ? sample[i] : minF_i;
            }
            int stepSize = (int) ((maxF_i - minF_i) / 10);
            for (int j = 0; j < 10; j++) {
                float thresh = minF_i + j * stepSize;
                index = i;
                operation = '(';
                boolean resultLt = Predict(samples[0]);
                float weightErrorLt = 0;
                operation = '>';
                boolean resultGt = Predict(samples[0]);
                float weightErrorGt = 0;
                minError = weightErrorLt < minError ? weightErrorLt : minError;
                operation = '(';
            }
        }
    }

    private boolean Predict(float[] attributes) {
        switch (operation) {
            case '(':
                return attributes[index] <= threshold;
            case '>':
                return attributes[index] > threshold;
            default:
                throw new IllegalArgumentException("Illegal Operation.");
        }
    }
}
