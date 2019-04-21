package fr.inria.yifan.mysensor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import fr.inria.yifan.mysensor.Context.ContextHelper;
import fr.inria.yifan.mysensor.Deprecated.SensingActivity;

/**
 * This activity show the context information of the crowdsensor.
 */

public class ContextActivity extends AppCompatActivity {
    // TODO
    private static final String TAG = "Context Activity";

    private final Object mLock; // Thread locker
    private boolean isRunning;

    private TextView contextView;
    private TextView attributeView;
    private ContextHelper mContextHelper;

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

        mContextHelper = new ContextHelper(this);
        mContextHelper.startService();

        Button feedbackButton = findViewById(R.id.feedback_button);
        feedbackButton.setOnClickListener(v -> mContextHelper.updatePEModels());

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
                    contextView.setText(mContextHelper.getContext().toString());
                    //HashMap<String, String> rules = new HashMap<>();
                    //rules.put("InPocket", "False");
                    //rules.put("UserActivity", "STILL");
                    //rules.put("Internet", "WIFI");
                    //Log.e(TAG, "Rule applied: " + rules.toString());
                    //Log.e(TAG, "Matched rules: " + mContextHelper.matchRules(rules));
                    attributeView.setText(mContextHelper.getIntentValues(new int[]{1, 0, 1}).toString());
                });

            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        mContextHelper.stopService();
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
