// V1

package fr.inria.yifan.mysensor.Context;

import weka.classifiers.lazy.LWL;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Training and exportation of multi-class learning models
 */

public class TrainDurationModel {

    // Main method for generating original model
    public static void main(String[] args) throws Exception {

        // Load data from CSV file
        ConverterUtils.DataSource source_train = new ConverterUtils.DataSource("/Users/yifan/OneDrive/INRIA/GroupData/Data_454.csv");
        Instances train = source_train.getDataSet();

        // Only keep used feature attributes
        Remove remove = new Remove();
        // deviceId,latitude,longitude,bias,proximity,activity,day,minute,weekday,ActDuration,LocDuration
        remove.setAttributeIndices("9, 8, 6, 10");
        remove.setInvertSelection(true);
        remove.setInputFormat(train);
        train = Filter.useFilter(train, remove);

        // Set data class label index
        train.setClassIndex(train.numAttributes() - 1);

        // Show all attributes
        System.out.print("Features:");
        for (int i = 0; i < train.numAttributes() - 1; i++) {
            System.out.println(train.attribute(i).name());
        }
        System.out.println("Target:" + train.classAttribute().name());

        //IBk model = new IBk();
        //KStar model = new KStar();
        LWL model = new LWL();

        long startTime = System.nanoTime();
        model.buildClassifier(train);
        long endTime = System.nanoTime();
        System.out.println("Runtime s: " + (endTime - startTime) / 1000000d);

        //Evaluation eva = new Evaluation(train);
        //eva.crossValidateModel(model, train, 10, new Random(1));
        //System.out.println(eva.toSummaryString());

        // Train the initial user activity model
        // Day 1-7; Hour 0-23; Minute 0-59; UA 1 "VEHICLE", 2 "BICYCLE", 3 "FOOT", 4 "STILL"; Duration in minute
        //ArrayList<Attribute> attInfo = new ArrayList<>();
        //attInfo.add(new Attribute("Day"));
        //attInfo.add(new Attribute("Hour"));
        //attInfo.add(new Attribute("Minute"));
        //attInfo.add(new Attribute("Activity"));
        //attInfo.add(new Attribute("Duration"));
        //Instances insts = new Instances("UserActivity", attInfo, 0);
        //insts.setClassIndex(insts.numAttributes() - 1);

        // Create some fake initial entries
        //double[] entry;
        //Instance inst;
        //entry= new double[]{1, 10, 1, 10};
        //inst = new DenseInstance(1, entry);
        //insts.add(inst);
        //entry = new double[]{2, 20, 2, 20};
        //inst = new DenseInstance(1, entry);
        //insts.add(inst);
        //entry = new double[]{3, 30, 3, 30};
        //inst = new DenseInstance(1, entry);
        //insts.add(inst);
        //entry= new double[]{4, 40, 4, 40};
        //inst = new DenseInstance(1, entry);
        //insts.add(inst);
        //entry= new double[]{5, 50, 1, 50};
        //inst = new DenseInstance(1, entry);
        //insts.add(inst);
        //System.out.println(insts);

        //LWL model = new LWL();
        //model.buildClassifier(insts);

        // Prediction on new instance
        // Day 1-7; Hour 0-23; Minute 0-59; UA 1 "VEHICLE", 2 "BICYCLE", 3 "FOOT", 4 "STILL"; label 0
        //double[] test = new double[]{1, 10, 1, 0};
        //Instance test_inst = new DenseInstance(1, test);
        //test_inst.setDataset(insts);

        //System.out.println(model.classifyInstance(test_inst));

        //Evaluation eva = new Evaluation(insts);
        //eva.evaluateModel(model, insts);
        //System.out.println(eva.toSummaryString());

        // Save and load
        //SerializationHelper.write("/Users/Yifan/Documents/SenseGroupTogether/app/src/main/assets/Activity_prediction.model", model);

        /*
        // Train the in-door context model
        // Day 1-7; Hour 0-23; Minute 0-59; Indoor 0 or 1; Duration in minute
        ArrayList<Attribute> attInfo = new ArrayList<>();
        attInfo.add(new Attribute("Day"));
        attInfo.add(new Attribute("Hour"));
        attInfo.add(new Attribute("Minute"));
        attInfo.add(new Attribute("Indoor"));
        attInfo.add(new Attribute("Duration"));
        Instances insts = new Instances("IndoorDetection", attInfo, 0);
        insts.setClassIndex(insts.numAttributes() - 1);

        // Create some fake initial entries
        double[] entry;
        Instance inst;
        entry= new double[]{1, 8, 10, 1, 10};
        inst = new DenseInstance(1, entry);
        insts.add(inst);
        entry = new double[]{2, 10, 20, 1, 20};
        inst = new DenseInstance(1, entry);
        insts.add(inst);
        entry = new double[]{3, 12, 30, 0, 30};
        inst = new DenseInstance(1, entry);
        insts.add(inst);
        entry= new double[]{4, 14, 40, 1, 40};
        inst = new DenseInstance(1, entry);
        insts.add(inst);
        entry= new double[]{5, 16, 50, 0, 50};
        inst = new DenseInstance(1, entry);
        insts.add(inst);
        //System.out.println(insts);

        LWL model = new LWL();
        model.buildClassifier(insts);

        // Prediction on new instance
        // Day 1-7; Hour 0-23; Minute 0-59; Indoor 0 or 1; label 0
        double[] test = new double[]{1, 12, 35, 1, 0};
        Instance test_inst = new DenseInstance(1, test);
        test_inst.setDataset(insts);

        System.out.println(model.classifyInstance(test_inst));

        // Save and load
        SerializationHelper.write("/Users/Yifan/Documents/SenseGroupTogether/app/src/main/assets/Door_prediction.model", model);
        */

        /*
        // Train the under-ground context model
        // Day 1-7; Hour 0-23; Minute 0-59; Underground 0 or 1; Duration in minute
        ArrayList<Attribute> attInfo = new ArrayList<>();
        attInfo.add(new Attribute("Day"));
        attInfo.add(new Attribute("Hour"));
        attInfo.add(new Attribute("Minute"));
        attInfo.add(new Attribute("Underground"));
        attInfo.add(new Attribute("Duration"));
        Instances insts = new Instances("UndergroundDetection", attInfo, 0);
        insts.setClassIndex(insts.numAttributes() - 1);

        // Create some fake initial entries
        double[] entry;
        Instance inst;
        entry = new double[]{1, 8, 10, 1, 10};
        inst = new DenseInstance(1, entry);
        insts.add(inst);
        entry = new double[]{2, 10, 20, 0, 20};
        inst = new DenseInstance(1, entry);
        insts.add(inst);
        entry = new double[]{3, 12, 30, 0, 30};
        inst = new DenseInstance(1, entry);
        insts.add(inst);
        entry = new double[]{4, 14, 40, 0, 40};
        inst = new DenseInstance(1, entry);
        insts.add(inst);
        entry = new double[]{5, 16, 50, 0, 50};
        inst = new DenseInstance(1, entry);
        insts.add(inst);

        //System.out.println(insts);

        LWL model = new LWL();
        model.buildClassifier(insts);

        // Prediction on new instance
        // Day 1-7; Hour 0-23; Minute 0-59; Underground 0 or 1; label 0
        double[] test = new double[]{1, 12, 35, 1, 0};
        Instance test_inst = new DenseInstance(1, test);
        test_inst.setDataset(insts);

        System.out.println(model.classifyInstance(test_inst));
        */

        // Save and load
        //SerializationHelper.write("/Users/Yifan/Documents/SenseGroupTogether/app/src/main/assets/Ground_prediction.model", model);
    }
}
