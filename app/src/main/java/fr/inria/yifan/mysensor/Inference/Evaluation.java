package fr.inria.yifan.mysensor.Inference;

import weka.core.Instances;
import weka.core.converters.ConverterUtils;

/**
 * Hierarchical models evaluation test
 */

public class Evaluation {

    // Main method for evaluating models
    public static void main(String[] args) throws Exception {

        // Load data from csv file
        ConverterUtils.DataSource source_train = new ConverterUtils.DataSource("/Users/yifan/OneDrive/INRIA/Context Sense/Training Data/GT-I9505.csv");
        ConverterUtils.DataSource source_test = new ConverterUtils.DataSource("/Users/yifan/OneDrive/INRIA/Context Sense/Training Data/Redmi-Note4_2.csv");
        Instances train = source_train.getDataSet();
        Instances test = source_test.getDataSet();



    }

}
