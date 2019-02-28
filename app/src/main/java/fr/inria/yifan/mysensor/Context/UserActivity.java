package fr.inria.yifan.mysensor.Context;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;

import java.util.HashMap;

/**
 * This class provides context information about the user activity.
 */

public class UserActivity extends BroadcastReceiver {

    private static final String TAG = "User activity";

    // Time interval between activity updates (milliseconds)
    private static final int ACTIVITY_UPDATE_TIME = 500;

    // Variables
    private Context mContext;
    private int mActivityType;
    private HashMap<String, String> mActivity;

    // Constructor initialization
    public UserActivity(Context context) {
        mContext = context;
        mActivityType = -1;
        mActivity = new HashMap<>();
        mActivity.put("UserActivity", null);
    }

    public void startService() {
        // Google activity recognition API
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 1001, new Intent("ActivityRecognitionResult"), PendingIntent.FLAG_CANCEL_CURRENT);
        ActivityRecognitionClient activityRecognitionClient = ActivityRecognition.getClient(mContext);
        // Register the update receiver
        activityRecognitionClient.requestActivityUpdates(ACTIVITY_UPDATE_TIME, pendingIntent);
        mContext.registerReceiver(this, new IntentFilter("ActivityRecognitionResult"));
    }

    public void stopService() {
        // Unregister the update receiver
        mContext.unregisterReceiver(this);
    }

    // Get the most recent user activity
    public HashMap getUserActivity() {
        switch (mActivityType) {
            case 0:
                mActivity.put("UserActivity", "VEHICLE");
                break;
            case 1:
                mActivity.put("UserActivity", "BICYCLE");
                break;
            case 2:
                mActivity.put("UserActivity", "FOOT");
                break;
            case 3:
                mActivity.put("UserActivity", "STILL");
                break;
            default:
                mActivity.put("UserActivity", "UNKNOWN");
                break;
        }
        return mActivity;
    }

    // Callback when receive a user activity result
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            mActivityType = result.getMostProbableActivity().getType();
            //Log.e(TAG, "Received intent: " + result.getMostProbableActivity().toString());
        }
    }
}
