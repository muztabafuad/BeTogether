package fr.inria.yifan.mysensor.Deprecated.Inference;

import java.util.ArrayList;

import weka.classifiers.lazy.LWL;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Training and exportation of multi-class learning model
 */

public class TrainDurationModel {

    // Main method for generating original model
    public static void main(String[] args) throws Exception {


        ArrayList<Attribute> attInfo = new ArrayList<>();
        attInfo.add(new Attribute("Day"));
        attInfo.add(new Attribute("Hour"));
        attInfo.add(new Attribute("Minute"));
        attInfo.add(new Attribute("Activity"));
        attInfo.add(new Attribute("Duration"));
        Instances insts = new Instances("UserActivity", attInfo, 1);
        insts.setClassIndex(insts.numAttributes() - 1);

        // Day 1-7, hour 0-23, minute 0-59, context index 1-4, duration in minute
        double[] entry = new double[]{1, 12, 30, 5, 30};
        Instance inst = new DenseInstance(1, entry);
        insts.add(inst);

        entry = new double[]{1, 12, 32, 5, 29};
        inst = new DenseInstance(1, entry);
        insts.add(inst);
        entry = new double[]{1, 12, 33, 5, 28};
        inst = new DenseInstance(1, entry);
        insts.add(inst);
        entry = new double[]{1, 12, 34, 5, 27};
        inst = new DenseInstance(1, entry);
        insts.add(inst);
        entry = new double[]{1, 12, 35, 5, 26};
        inst = new DenseInstance(1, entry);
        insts.add(inst);
        entry = new double[]{1, 12, 36, 5, 25};
        inst = new DenseInstance(1, entry);
        insts.add(inst);
        inst.setDataset(insts);

        System.out.println(insts);

        LWL model = new LWL();
        model.buildClassifier(insts);

        double[] entry1 = new double[]{1, 12, 27, 5, 0};
        Instance inst1 = new DenseInstance(1, entry1);
        inst1.setDataset(insts);

        System.out.println(model.toString());
        System.out.println(model.classifyInstance(inst1));

        /*
        // 10-fold cross validation
        Evaluation cross = new Evaluation(newTrain);
        cross.crossValidateModel(classifier, newTrain, 10, new Random());
        System.out.println(cross.toSummaryString());

        // Save and load
        SerializationHelper.write("/Users/yifan/Documents/MySensor/app/src/main/assets/Classifier_one.model", classifier);
        Instances dataSet = new Instances(newTrain, 0);
        SerializationHelper.write("/Users/yifan/Documents/MySensor/app/src/main/assets/Dataset_one.model", dataSet);

        */
    }
}
