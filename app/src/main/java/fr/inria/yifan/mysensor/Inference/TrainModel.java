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
            System.out.println(" Target:" + newTrain.classAttribute().name());

            // Multiply runs for evaluation
            int run = 100;

            /*
            // Runtime evaluation
            long startTime;
            long endTime;
            HoeffdingTree classifier = new HoeffdingTree();
            startTime = System.nanoTime();
            // Batch training
            classifier.buildClassifier(newTrain);
            endTime = System.nanoTime();
            System.out.println("Training time (batch): " + (endTime - startTime));

            long totalTime = 0;
            for (int i = 0; i < run; i++) {
                startTime = System.nanoTime();
                classifier.updateClassifier(newTest.instance(i));
                endTime = System.nanoTime();
                totalTime += (endTime - startTime);
            }
            System.out.println("Updating time (online): " + totalTime / run);
            */

            // 10-fold cross validation
            //Evaluation cross = new Evaluation(newTrain);
            //cross.crossValidateModel(classifier, newTrain, 10, new Random());
            //System.out.println(cross.toSummaryString());

            // Evaluate classifier on data set
            //Evaluation eval = new Evaluation(newTest);
            //eval.evaluateModel(classifier, newTest);
            //System.out.println(eval.toSummaryString());

            // Accuracy evaluation
            int count;
            int count_max;
            double acc_max;
            StringBuilder logging = new StringBuilder();

            // Loop for multiple runs
            for (int i = 0; i < run; i++) {
                count = 0;
                count_max = 0;
                acc_max = 0;
                // Randomize each run!
                Random random = new Random();
                newTest.randomize(random);
                // New classifier each run!
                HoeffdingTree classifier = new HoeffdingTree();
                classifier.buildClassifier(newTrain);

                // Limit the feedback amount
                for (int j = 0; j < 50; j++) {
                    Evaluation eva = new Evaluation(newTest);
                    eva.evaluateModel(classifier, newTest);

                    // Opportunistic feedback on wrong inference
                    if (random.nextInt(100) > eva.pctCorrect()) {
                        // Generate Poisson number
                        //double lambda = 1d;
                        //int k = AdaBoost.Poisson(lambda);
                        //System.out.println("K value = " + k);

                        // Repeat learning from one sample
                        //for (int j = 0; j < k; j++) {
                        classifier.updateClassifier(newTest.instance(j));
                        //}
                        count++;
                        double acc = eva.pctCorrect();
                        if (acc > acc_max) {
                            acc_max = acc;
                            count_max = count;
                        }
                    }
                }
                System.out.println(i + "th Feedback number: " + count_max + ", Max accuracy: " + acc_max);
                logging.append(count_max).append(", ").append(acc_max).append("\n");
            }

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
