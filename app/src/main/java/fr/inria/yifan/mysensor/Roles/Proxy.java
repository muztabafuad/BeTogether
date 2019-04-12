package fr.inria.yifan.mysensor.Roles;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;

public class Proxy {

    private static final String TAG = "Proxy";

    public Proxy(Context context) {

        // Access a Cloud Firestore instance from your Activity
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
                        assert task.getResult() != null;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }
}
