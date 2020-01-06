// V1

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

import java.util.HashMap;

import fr.inria.yifan.mysensor.Context.ContextHelper;

/**
 * This activity shows the context information of the crowdsensor.
 */

public class ContextActivity extends AppCompatActivity {

    private static final String TAG = "Context Activity";

    private final Object mLock; // Thread locker
    private boolean isRunning;

    private TextView contextView;
    private TextView intentView;

    private ContextHelper mContextHelper;

    public ContextActivity() {
        mLock = new Object();
    }

    // Main activity initialization
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_context);
        contextView = findViewById(R.id.context_view);
        intentView = findViewById(R.id.intents_view);
        Button feedbackButton = findViewById(R.id.feedback_button);

        mContextHelper = new ContextHelper(this);
        feedbackButton.setOnClickListener(v -> mContextHelper.updatePEModels());

        isRunning = true;
        mContextHelper.startService();

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
                    HashMap context = mContextHelper.getContext();
                    contextView.setText(context.toString());
                    HashMap intent = mContextHelper.getIntentValues(new int[]{1, 1, 1, 1});
                    intentView.setText(intent.toString());
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
