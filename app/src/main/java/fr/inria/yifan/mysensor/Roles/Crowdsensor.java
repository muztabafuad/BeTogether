package fr.inria.yifan.mysensor.Roles;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * This class provides a crowdsensor and its sensing methods.
 */

public class Crowdsensor {

    private static final String TAG = "Crowdsensor";

    private Context mContext;

    // Type: Data, ...
    private JSONObject DataMsg;

    public Crowdsensor(Context context) {
        mContext = context;
    }

    public void startAggregator() {

    }

    public JSONObject getAggregatorResult() {
        return null;
    }

    public void startCoordinator() {

    }

    public void startLocator(float timeSlot) {

    }

    public JSONObject getLocatorResult() {
        return null;
    }

    public void startProxy() {

    }

    @SuppressWarnings("ConstantConditions")
    public void doProxyWork() {
        // Access a Cloud Firestore instance from Activity
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        HashMap<String, Object> instance = new HashMap<>();
        instance.put("SoundLevel", 30);

        // Add a new document with a generated ID
        db.collection("SensingData").add(instance)
                .addOnSuccessListener(documentReference ->
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId()))
                .addOnFailureListener(e ->
                        Log.w(TAG, "Error adding document", e));

        db.collection("SensingData").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }

    public void startSensor(String sensor, float timeSlot) {

    }

    public JSONObject getSensorResult(float timeSlot) {
        return null;
    }

}
