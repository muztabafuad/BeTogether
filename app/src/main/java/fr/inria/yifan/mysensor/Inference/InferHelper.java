package fr.inria.yifan.mysensor.Inference;

/**
 * This class represents the inference helper for environmental contexts.
 */

public class InferHelper {

    private static final String TAG = "Inference helper";

    public InferHelper(){

    }

    public boolean inPocketInfer(){
        return false;
    }

    public boolean inDoorInfer(){
        return false;
    }

    public boolean underGroundInfer(){
        return false;
    }

    public void updateModel(int index, float[] sample){
        switch (index) {
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
        }
    }
}
