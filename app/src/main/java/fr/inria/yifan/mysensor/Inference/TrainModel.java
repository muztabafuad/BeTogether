package fr.inria.yifan.mysensor.Inference;

import java.util.Random;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.attribute.Remove;

public class TrainModel {

    // Main method for generating original model
    public static void main(String[] args) {
        try {
            //DataSource source = new DataSource("C:/Users/Yifan/OneDrive/INRIA/Context Sense/Training Data/GT-I9505_1527175592592.csv");
            DataSource source = new DataSource("/Users/yifan/OneDrive/INRIA/Context Sense/Training Data/GT-I9505_1527175592592.csv");
            Instances data = source.getDataSet();

            for (int i = 0; i < data.numAttributes(); i++) {
                System.out.println(data.attribute(i).name());
            }

            // Remove attributes
            Remove remove = new Remove();
            remove.setAttributeIndices("1, 2, 5, 11, 12, 13, 14, 15, 17");
            remove.setInputFormat(data);
            Instances revData = Filter.useFilter(data, remove);

            NumericToNominal nominal = new NumericToNominal();
            nominal.setAttributeIndices("8");
            nominal.setInputFormat(revData);
            Instances newData = Filter.useFilter(revData, nominal);

            for (int i = 0; i < newData.numAttributes(); i++) {
                System.out.println(newData.attribute(i).name());
            }

            if (newData.classIndex() == -1) {
                newData.setClassIndex(newData.numAttributes() - 1);
            }

            System.out.println(newData.firstInstance());
            System.out.println(newData.classAttribute().name());

            J48 tree = new J48();         // new instance of tree
            tree.buildClassifier(newData);   // build classifier

            Evaluation eval = new Evaluation(newData);
            eval.crossValidateModel(tree, newData, 10, new Random(1));
            System.out.println(eval.toSummaryString("\nResults\n======\n", false));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
