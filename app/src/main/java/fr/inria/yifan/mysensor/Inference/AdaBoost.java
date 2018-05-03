package fr.inria.yifan.mysensor.Inference;

import java.io.Serializable;

/*
 This class implements AdaBoost inference for 0-1 classes
 */

public class AdaBoost implements Serializable {

    private static final String TAG = "AdaBoost";
    private static final long serialVersionUID = 1000L;

    // Weak learners and respective weights
    private int numLearn; // Number of weak learners (classifiers)
    private int[] features; // The index of features to use for learn
    private int stepNumber; // Number of steps for threshold increasing
    private DecisionStump[] dStumps; // An array of decision stumps
    private double[] alphas; // Alpha coefficient for each decision stump

    // Initialization
    AdaBoost(int numLearner, int[] featureInd, int stepLearn) {
        numLearn = numLearner;
        features = featureInd;
        stepNumber = stepLearn;
        dStumps = new DecisionStump[numLearn];
        alphas = new double[numLearn];
    }

    // Train model from a large samples array
    public void BatchTrain(double[][] samples) {
        // Initial the weights for all samples
        double[] weight = new double[samples.length];
        for (int j = 0; j < weight.length; j++) {
            weight[j] = 1d / samples.length;
        }
        // Train all weak learners
        for (int i = 0; i < dStumps.length; i++) {
            // For each weak learner
            dStumps[i] = new DecisionStump();
            dStumps[i].BatchTrain(samples, weight, features, stepNumber);
            double epsilon = dStumps[i].getError();
            // The weight for classifier
            alphas[i] = Math.log((1d - epsilon) / epsilon);
            // Update the weight for all samples
            for (int j = 0; j < weight.length; j++) {
                weight[j] = weight[j] * (dStumps[i].Predict(samples[j]) == samples[j][samples[j].length - 1]
                        ? 1d / (2d - 2d * epsilon)
                        : 1d / (2d * epsilon));
            }
        }
    }

    // Greedy update from one new sample
    public void GreedyUpdate(double[] sample) {
        // Train all weak learners
        for (int i = 0; i < numLearn; i++) {
            // Found wrong predictor
            if (dStumps[i].Predict(sample) != sample[sample.length - 1]) {
                dStumps[i].UpdateThreshold(sample);
            }
        }
    }

    // Online AdaBoost from one new sample
    public void OnlineUpdate(double[] sample) {
        double lambda = 1d;
        // Train all weak learners
        for (int i = 0; i < numLearn; i++) {
            double lambda_right = 0d;
            double lambda_wrong = 0d;
            int k = Poisson(lambda);
            for (int j = 0; j < k; j++) {
                dStumps[i].PoissonUpdate(sample);
            }
            if (dStumps[i].Predict(sample) == sample[sample.length - 1]) {
                lambda_right = lambda_right + lambda;
                double epsilon = lambda_wrong / (lambda_right + lambda_wrong);
                lambda = lambda * (1 / (2 - 2 * epsilon));
                alphas[i] = Math.log((1d - epsilon) / epsilon);
            } else {
                lambda_wrong = lambda_wrong + lambda;
                double epsilon = lambda_wrong / (lambda_right + lambda_wrong);
                lambda = lambda * (1 / (2 * epsilon));
                alphas[i] = Math.log((1d - epsilon) / epsilon);
            }
        }
    }

    // Make inference for a new sample
    public int Predict(double[] feature) {
        // Predict new sample from all learners
        double result_1 = 0d;
        double result_0 = 0d;
        for (int i = 0; i < numLearn; i++) {
            if (dStumps[i].Predict(feature) == 1) {
                result_1 += alphas[i];
            } else if (dStumps[i].Predict(feature) == 0) {
                result_0 += alphas[i];
            } else {
                throw new IllegalArgumentException("Illegal prediction.");
            }
        }
        return result_1 > result_0 ? 1 : 0;
    }

    // Generate a Poisson number
    private int Poisson(double lambda) {
        double L = Math.exp(-lambda);
        double p = 1d;
        int k = 0;
        do {
            k++;
            p *= Math.random();
        } while (p > L);
        return k - 1;
    }
}
