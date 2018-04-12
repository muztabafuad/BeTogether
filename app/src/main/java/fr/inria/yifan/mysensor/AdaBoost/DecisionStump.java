package fr.inria.yifan.mysensor.AdaBoost;

/**
 * This class implements the decision stump (weak learner) for AdaBoost.
 */

public class DecisionStump {

    private int index; // Index of attribute
    private char operation;
    private float threshold;

    public DecisionStump(float[] attributes) {

    }

    public boolean Predict(float[] attributes) {
        switch (operation) {
            case '(':
                return attributes[index] <= threshold;
            case '<':
                return attributes[index] < threshold;
            case ')':
                return attributes[index] >= threshold;
            case '>':
                return attributes[index] > threshold;
            default:
                throw new IllegalArgumentException("Illegal Operation.");
        }
    }
}
