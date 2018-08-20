package fr.inria.yifan.mysensor.Inference;

import android.content.Context;
import android.util.Log;

/**
 * This class represents the inference helper for environmental contexts.
 */

public class InferHelper {

    private static final String TAG = "Inference helper";

    private Context mContext;

    public InferHelper(Context context) {
        mContext = context;
        // Load the base model

    }

    public boolean inferInPocket(double[] sample) {
        return false;
    }

    public boolean inferInDoor(double[] sample) {
        return false;
    }

    public boolean inferUnderGround(double[] sample) {
        return false;
    }

    public void updateModel(String model, double[] sample) {
        switch (model) {
            case "Pocket":
                break;
            case "Door":
                break;
            case "Ground":
            default:
                Log.e(TAG, "Wrong parameter of model type: " + model);
        }
    }

}
