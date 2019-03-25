package fr.inria.yifan.mysensor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;

import fr.inria.yifan.mysensor.Context.FeatureHelper;
import fr.inria.yifan.mysensor.Deprecated.SensingActivity;

/**
 * This activity show the context information of the crowdsensor
 */

public class ContextActivity extends AppCompatActivity {
    // TODO
    private static final String TAG = "Context Activity";

    private final Object mLock; // Thread locker
    private boolean isRunning;

    private TextView contextView;
    private TextView attributeView;
    private FeatureHelper mFeatureHelper;

    public ContextActivity() {
        mLock = new Object();
        isRunning = true;
    }

    // Main activity initialization
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_context);
        contextView = findViewById(R.id.context_view);
        attributeView = findViewById(R.id.intents_view);

        mFeatureHelper = new FeatureHelper(this);
        mFeatureHelper.startService();

        Button feedbackButton = findViewById(R.id.feedback_button);
        feedbackButton.setOnClickListener(v -> mFeatureHelper.updateModels());

        new Thread(() -> {
            while (isRunning) {
                // Delay
                synchronized (mLock) {
                    try {
                        mLock.wait(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                runOnUiThread(() -> {
                    contextView.setText(mFeatureHelper.getContext().toString());
                    HashMap<String, String> rules = new HashMap<>();
                    rules.put("InPocket", "False");
                    rules.put("UserActivity", "STILL");
                    rules.put("Internet", "WIFI");
                    Log.e(TAG, "Rule applied: " + rules.toString());
                    Log.e(TAG, "Matched rules: " + mFeatureHelper.matchRules(rules));
                    attributeView.setText(mFeatureHelper.getIntentValues(new int[]{1, 0, 1}).toString());
                });

            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
        mFeatureHelper.stopService();
    }

    // Go to the sensing activity
    public void goSensing(View view) {
        Intent goToSensing = new Intent();
        goToSensing.setClass(this, SensingActivity.class);
        startActivity(goToSensing);
        finish();
    }

    // Go to the service activity
    public void goService(View view) {
        Intent goToService = new Intent();
        goToService.setClass(this, ServiceActivity.class);
        startActivity(goToService);
        finish();
    }

}
