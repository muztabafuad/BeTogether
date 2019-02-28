package fr.inria.yifan.mysensor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import fr.inria.yifan.mysensor.Context.PhysicalEnvironment;
import fr.inria.yifan.mysensor.Context.UserActivity;

/**
 * This activity has to be started in the beginning of the application to ensure all user permissions are enabled
 */

public class TestActivity extends AppCompatActivity {

    private final Object mLock; // Thread locker
    private UserActivity mUserActivity;
    private PhysicalEnvironment mPhysicalEnvironment;
    private String mActivityResult;
    private String mEnvironmentRsult;
    private TextView activityView;
    private TextView environmentView;

    public TestActivity() {
        mLock = new Object();
    }

    // Main activity initialization
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        activityView = findViewById(R.id.activity_view);
        environmentView = findViewById(R.id.environment_view);
        Button feedbackButton = findViewById(R.id.feedback_button);
        feedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhysicalEnvironment.updateModels();
            }
        });

        mUserActivity = new UserActivity(this);
        mUserActivity.startService();
        mPhysicalEnvironment = new PhysicalEnvironment(this);
        mPhysicalEnvironment.startService();


        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mActivityResult = mUserActivity.getUserActivity().toString();
                            activityView.setText(mActivityResult);
                            mEnvironmentRsult = mPhysicalEnvironment.getPhysicalEnv().toString();
                            environmentView.setText(mEnvironmentRsult);
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

        //mUserActivity.stopService();
    }
}
