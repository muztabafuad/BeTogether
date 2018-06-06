package fr.inria.yifan.mysensor.Inference;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.Standardize;

public class TrainModel {

    // Main method for generating original model
    public static void main(String[] args) {

        DataSource source;
        try {
            source = new DataSource("C:/Users/Yifan/OneDrive/INRIA/Context Sense/Training Data/GT-I9505_1527175592592.csv");
            Instances data = source.getDataSet();
            // Set the filter
            Remove remove = new Remove();
            remove.setAttributeIndices("0, 1, 4, 10, 11, 12, 13, 14, 16");
            remove.setInputFormat(data);
            // Configures the Filter based on train instances and returns filtered instances
            Instances data_new = Filter.useFilter(data, remove);
            System.out.println(data_new);

            for (int i = 0; i < data.numAttributes(); i++) {
                System.out.println(data.attribute(i).name());
            }

            //rm.setInputFormat(data); // initializing the filter once with training set
              // remove attributes

            for (int i = 0; i < data_new.numAttributes(); i++) {
                System.out.println(data_new.attribute(i).name());
            }
            // setting class attribute if the data format does not provide this information
            // For example, the XRFF format saves the class attribute information as well
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }
            System.out.println(data.firstInstance());
            System.out.println(data.classAttribute().name());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
