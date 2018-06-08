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
            DataSource source_train = new DataSource("/Users/yifan/OneDrive/INRIA/Context Sense/Training Data/GT-I9505_1527175592592.csv");
            DataSource source_test = new DataSource("/Users/yifan/OneDrive/INRIA/Context Sense/Training Data/Redmi Note 4_1527233438107.csv");
            Instances train = source_train.getDataSet();
            Instances test = source_test.getDataSet();

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
            remove.setInputFormat(train);

            Instances revTrain = Filter.useFilter(train, remove);
            Instances revTest = Filter.useFilter(test, remove);

            // Convert class to nominal
            NumericToNominal nominal = new NumericToNominal();
            nominal.setAttributeIndices("3");
            nominal.setInputFormat(revTrain);

            Instances newTrain = Filter.useFilter(revTrain, nominal);
            Instances newTest = Filter.useFilter(revTest, nominal);

            // Set data class label index
            newTrain.setClassIndex(newTrain.numAttributes() - 1);
            newTest.setClassIndex(newTest.numAttributes() - 1);
            newTest.randomize(new Random());

            for (int i = 0; i < newTrain.numAttributes(); i++) {
                System.out.print(newTrain.attribute(i).name());
            }

            // 10-fold cross validation
            HoeffdingTree tree = new HoeffdingTree(); // new instance of tree
            Evaluation cv = new Evaluation(newTrain);
            cv.crossValidateModel(tree, newTrain, 10, new Random(1));
            System.out.println(cv.toSummaryString());

            long startTime;
            long endTime;
            long totalTime;

            // Batch training
            startTime = System.nanoTime();
            tree.buildClassifier(newTrain);   // build classifier
            endTime = System.nanoTime();
            System.out.println("Training time (batch): " + (endTime - startTime));

            // Evaluate classifier on data set
            Evaluation eval = new Evaluation(newTest);
            eval.evaluateModel(tree, newTest);
            System.out.println(eval.toSummaryString());

            // Incremental training
            int boostRun = 1000;
            totalTime = 0;
            for (int i = 0; i < newTest.numInstances(); i++) {
                startTime = System.nanoTime();
                // Boosting the incrementL learning
                for (int j = 0; j < boostRun; j++){
                    tree.updateClassifier(newTest.instance(i));
                }
                endTime = System.nanoTime();
                totalTime += endTime - startTime;
                Evaluation ev = new Evaluation(newTest);
                ev.evaluateModel(tree, newTest);
                System.out.println("Iteration: " + i + " Accuracy: " + ev.pctCorrect());
            }
            System.out.println("Training time (mean, nano): " + totalTime / newTest.numInstances());

            // Save and load
            SerializationHelper.write("/Users/yifan/Documents/MySensor/app/src/main/assets/HoeffdingTree.model", tree);
            tree = (HoeffdingTree) SerializationHelper.read("/Users/yifan/Documents/MySensor/app/src/main/assets/Tree.model");

            // Classify new instance
            Instances dataSet = new Instances(newTrain, 0);
            Instance inst = new DenseInstance(3);
            inst.setValue(0, 200.0f);
            inst.setValue(1, 8.0f);
            inst.setDataset(dataSet);
            int result = (int) tree.classifyInstance(inst);
            //System.out.println("Sample: " + inst + ", Inference: " + result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
