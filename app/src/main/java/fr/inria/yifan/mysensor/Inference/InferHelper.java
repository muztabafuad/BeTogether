package fr.inria.yifan.mysensor.Inference;

/**
 * This class represents the inference helper for environmental contexts.
 */

public class InferHelper {

    private static final String TAG = "Inference helper";

    public InferHelper(){
        // Load the base model

    }

    public boolean inferInPocket(){
        return false;
    }

    public boolean inferInDoor(){
        return false;
    }

    public boolean inferUnderGround(){
        return false;
    }

    public void updateModel(int index, float[] sample){
        // 1 pocket, 2 door, 3 ground
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
