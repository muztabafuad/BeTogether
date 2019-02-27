package fr.inria.yifan.mysensor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import fr.inria.yifan.mysensor.Context.UserActivity;

/**
 * This activity has to be started in the beginning of the application to ensure all user permissions are enabled
 */

public class TestActivity extends AppCompatActivity {

    private final Object mLock; // Thread locker
    private UserActivity mUserActivity;
    private String mActivityResult;
    private TextView welcomeView;

    public TestActivity() {
        mLock = new Object();
    }

    // Main activity initialization
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        welcomeView = findViewById(R.id.welcome_view);

        mUserActivity = new UserActivity(this);
        mUserActivity.startService();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mActivityResult = mUserActivity.getUserActivity().toString();
                            welcomeView.setText(mActivityResult);
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
