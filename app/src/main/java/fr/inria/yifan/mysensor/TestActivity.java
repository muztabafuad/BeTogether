package fr.inria.yifan.mysensor;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;

import fr.inria.yifan.mysensor.Context.FeatureHelper;

/**
 * This activity has to be started in the beginning of the application to ensure all user permissions are enabled
 */

public class TestActivity extends AppCompatActivity {

    private static final String TAG = "Test Activity";

    private final Object mLock; // Thread locker

    private TextView contextView;
    //private TextView environmentView;
    //private TextView attributeView;

    private FeatureHelper mFeatureHelper;

    public TestActivity() {
        mLock = new Object();
    }

    // Main activity initialization
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        contextView = findViewById(R.id.activity_view);
        //environmentView = findViewById(R.id.environment_view);
        //attributeView = findViewById(R.id.attribute_view);
        Button feedbackButton = findViewById(R.id.feedback_button);

        mFeatureHelper = new FeatureHelper(this);

        feedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFeatureHelper.updateModels();
            }
        });

        mFeatureHelper.startService();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void run() {

                            contextView.setText(mFeatureHelper.getContext().toString());

                            HashMap<String, String> rules = new HashMap<>();
                            rules.put("InPocket", "False");
                            rules.put("UserActivity", "STILL");
                            rules.put("Internet", "WIFI");
                            //Log.e(TAG, "Rule applied: " + rules.toString());
                            //Log.e(TAG, "Matched rules: " + mFeatureHelper.matchRules(rules));
                        }
                    });

                    synchronized (mLock) {
                        try {
                            mLock.wait(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();

        //mFeatureHelper.stopService();
    }
}
