package fr.inria.yifan.mysensor.Inference;

/*
 This class implements the decision stump (weak learner) for AdaBoost.
 */

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_POCKET;

// 1 daytime, 2 light, 3 magnetic, 4 GSM, 5 GPS accuracy, 6 GPS speed, 7 proximity

public class InferHelper {

    private static final String TAG = "Inference helper";

    private Context mContext;
    private AdaBoost mAdaBoost;

    public InferHelper(Context context) {
        mContext = context;
        // Check local model existence
        File file = mContext.getFileStreamPath(MODEL_POCKET);
        if (!file.exists()) {
            Log.d(TAG, "Local model does not exist.");
            // Initialize trained model
            try {
                FileInputStream fileInputStream = mContext.getAssets().openFd(MODEL_POCKET).createInputStream();
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                mAdaBoost = (AdaBoost) objectInputStream.readObject();
                objectInputStream.close();
                fileInputStream.close();
                Log.d(TAG, "Success in loading from file.");
                FileOutputStream fileOutputStream = mContext.openFileOutput(MODEL_POCKET, Context.MODE_PRIVATE);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(mAdaBoost);
                objectOutputStream.close();
                fileOutputStream.close();
                Log.d(TAG, "Success in saving into file.");
            } catch (Exception e) {
                Log.d(TAG, "Error when loading from file: " + e);
            }
        } else {
            Log.d(TAG, "Local model already exist.");
            try {
                FileInputStream fileInputStream = context.openFileInput(MODEL_POCKET);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                mAdaBoost = (AdaBoost) objectInputStream.readObject();
                objectInputStream.close();
                fileInputStream.close();
                Log.d(TAG, "Success in loading from file.");
            } catch (Exception e) {
                Log.d(TAG, "Error when loading model file: " + e);
            }
        }
    }

    public int InferPocket(double[] sample) {
        assert mAdaBoost != null;
        return mAdaBoost.Predict(sample);
    }

    // Feedback on wrong inference
    public void FeedBack(double[] sample) {
        assert mAdaBoost != null;
        if (mAdaBoost.Predict(sample) != sample[sample.length - 1]) {
            mAdaBoost.PoissonUpdate(sample);
        }

        // Save trained model
        try {
            FileOutputStream fileOutputStream = mContext.openFileOutput(MODEL_POCKET, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(mAdaBoost);
            objectOutputStream.close();
            fileOutputStream.close();
            Log.d(TAG, "Success in updating model file.");
        } catch (Exception e) {
            Log.d(TAG, "Error when updating model file: " + e);
        }
    }

}
