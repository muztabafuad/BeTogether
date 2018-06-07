package fr.inria.yifan.mysensor.Inference;

import java.util.Random;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.HoeffdingTree;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.attribute.Remove;

public class TrainModel {

    // Main method for generating original model
    public static void main(String[] args) {
        try {

            // Load data from csv file
            DataSource source = new DataSource("/Users/yifan/OneDrive/INRIA/Context Sense/Training Data/GT-I9505_1527175592592.csv");
            Instances data = source.getDataSet();

            /*
            1 timestamp 2 daytime (b) 3 light density (lx) 4 magnetic strength (μT) 5 GSM active (b)
            6 RSSI level 7 GPS accuracy (m) 8 Wifi active (b) 9 Wifi RSSI (dBm) 10 proximity (b)
            11 sound level (dBA) 12 temperature (C) 13 pressure (hPa) 14 humidity (%)
            15 in-pocket label 16 in-door label 17 under-ground label
            */

            // Remove unused attributes
            Remove remove = new Remove();
            remove.setAttributeIndices("3, 10, 15");
            remove.setInvertSelection(true);
            remove.setInputFormat(data);
            Instances revData = Filter.useFilter(data, remove);

            // Convert class to nominal
            NumericToNominal nominal = new NumericToNominal();
            nominal.setAttributeIndices("3");
            nominal.setInputFormat(revData);
            Instances newData = Filter.useFilter(revData, nominal);

            // Set data class label index
            newData.setClassIndex(newData.numAttributes() - 1);

            for (int i = 0; i < newData.numAttributes(); i++) {
                System.out.print(newData.attribute(i).name());
            }

            // 10-fold cross validation
            HoeffdingTree tree = new HoeffdingTree(); // new instance of tree
            Evaluation cv = new Evaluation(newData);
            cv.crossValidateModel(tree, newData, 10, new Random(1));
            System.out.println(cv.toSummaryString());

            // Batch training
            tree.buildClassifier(newData);   // build classifier

            long totalTime = 0;
            // Incremental training
            for (int i = 0; i < newData.numInstances(); i++) {
                long startTime = System.nanoTime();
                tree.updateClassifier(newData.instance(i));
                long endTime = System.nanoTime();
                totalTime += endTime - startTime;
            }
            System.out.println("Training time (mean, nano): " + totalTime/newData.numInstances());

            // Save and load
            SerializationHelper.write("/Users/yifan/Documents/MySensor/app/src/main/assets/Tree.model", tree);
            tree = (HoeffdingTree) SerializationHelper.read("/Users/yifan/Documents/MySensor/app/src/main/assets/Tree.model");

            // Evaluate classifier on data set
            Evaluation eval = new Evaluation(newData);
            eval.evaluateModel(tree, newData);
            System.out.println(eval.toSummaryString());

            // Classify new instance
            Instance inst = new DenseInstance(3);
            inst.setValue(0, 200.0f);
            inst.setValue(1, 8.0f);
            inst.setDataset(newData);
            int result = (int) tree.classifyInstance(inst);
            System.out.println("Sample: " + inst + ", Inference: " + result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
