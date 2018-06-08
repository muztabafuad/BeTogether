package fr.inria.yifan.mysensor.Inference;

import java.io.FileOutputStream;
import java.util.Random;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.HoeffdingTree;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.attribute.Remove;

public class TrainModel {

    // Main method for generating original model
    public static void main(String[] args) {

        try {
            // Load data from csv file
            DataSource source_train = new DataSource("/Users/yifan/OneDrive/INRIA/Context Sense/Training Data/GT-I9505.csv");
            DataSource source_test = new DataSource("/Users/yifan/OneDrive/INRIA/Context Sense/Training Data/Redmi-Note4.csv");
            Instances train = source_train.getDataSet();
            Instances test = source_test.getDataSet();

            /*
            1 timestamp, 2 daytime (b), 3 light density (lx), 4 magnetic strength (μT), 5 GSM active (b),
            6 RSSI level, 7 RSSI value (dBm), 8 GPS accuracy (m), 9 Wifi active (b), 10 Wifi RSSI (dBm),
            11 proximity (b), 12 sound level (dBA), 13 temperature (C), 14 pressure (hPa), 15 humidity (%),
            16 in-pocket label, 17 in-door label, 18 under-ground label"
            */

            // Only keep used attributes
            Remove remove = new Remove();
            remove.setAttributeIndices("6, 8, 13, 9, 7, 14, 10, 15, 18");
            remove.setInvertSelection(true);
            remove.setInputFormat(train);
            Instances revTrain = Filter.useFilter(train, remove);
            Instances revTest = Filter.useFilter(test, remove);

            // Convert class to nominal
            NumericToNominal nominal = new NumericToNominal();
            nominal.setAttributeIndices(String.valueOf(revTrain.numAttributes()));
            nominal.setInputFormat(revTrain);
            Instances newTrain = Filter.useFilter(revTrain, nominal);
            Instances newTest = Filter.useFilter(revTest, nominal);

            // Set data class label index
            newTrain.setClassIndex(newTrain.numAttributes() - 1);
            newTest.setClassIndex(newTest.numAttributes() - 1);
            newTrain.randomize(new Random());
            newTest.randomize(new Random());

            // Show all attributes
            for (int i = 0; i < newTrain.numAttributes() - 1; i++) {
                System.out.print(newTrain.attribute(i).name());
            }
            System.out.print(" Target:" + newTrain.classAttribute().name());

            // 10-fold cross validation
            HoeffdingTree classifier = new HoeffdingTree();
            Evaluation cross = new Evaluation(newTrain);
            cross.crossValidateModel(classifier, newTrain, 10, new Random());
            System.out.println(cross.toSummaryString());

            long startTime;
            long endTime;
            long totalTime;

            // Batch training
            startTime = System.nanoTime();
            classifier.buildClassifier(newTrain);   // build classifier
            endTime = System.nanoTime();
            System.out.println("Training time (in batch): " + (endTime - startTime));

            // Evaluate classifier on data set
            Evaluation eval = new Evaluation(newTest);
            eval.evaluateModel(classifier, newTest);
            System.out.println(eval.toSummaryString());

            // Incremental training
            int boost = 10;
            int counter = 0;
            totalTime = 0;
            Random random = new Random();
            StringBuilder logging = new StringBuilder();
            for (int i = 0; i < 500; i++) {
                Evaluation ev = new Evaluation(newTest);
                ev.evaluateModel(classifier, newTest);
                // Opportunistic feedback on wrong inference
                if (random.nextInt(100) > ev.pctCorrect()) {
                    startTime = System.nanoTime();
                    for (int j = 0; j < boost; j++) {
                        classifier.updateClassifier(newTest.instance(i));
                    }
                    endTime = System.nanoTime();
                    totalTime += endTime - startTime;
                    counter++;
                    System.out.println("Feedback number: " + counter + " Accuracy: " + ev.pctCorrect());
                    logging.append(counter).append(", ").append(ev.pctCorrect()).append("\n");
                }
            }
            System.out.println("Training time (incremental): " + totalTime / counter);

            // Save the log file
            String logfile = "/Users/yifan/Documents/MySensor/app/src/main/assets/ClassificationAccuracy";
            FileOutputStream output = new FileOutputStream(logfile);
            output.write(logging.toString().getBytes());
            output.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        // Save and load
        //SerializationHelper.write("/Users/yifan/Documents/MySensor/app/src/main/assets/Classifier.model", classifier);
        //classifier = (HoeffdingTree) SerializationHelper.read("/Users/yifan/Documents/MySensor/app/src/main/assets/Classifier.model");

        // Classify new instance
        //Instances dataSet = new Instances(newTrain, 0);
        //Instance inst = new DenseInstance(1, new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0});
        //inst.setDataset(dataSet);
        //int result = (int) classifier.classifyInstance(inst);
        //System.out.println("Sample: " + inst + ", Inference: " + result);
    }
}
