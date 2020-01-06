// V1

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

import static fr.inria.yifan.mysensor.Context.ContextHelper.MIN_UPDATE_TIME;

/**
 * This class provides context information about the user activity.
 */

public class UserActivity extends BroadcastReceiver {

    private static final String TAG = "User activity";

    // Variables
    private Context mContext;
    private int mActivityType;
    private HashMap<String, String> mUserActiv;

    // Constructor
    public UserActivity(Context context) {
        mContext = context;
        mActivityType = -1; // UNKNOWN
        mUserActiv = new HashMap<>();
        mUserActiv.put("UserActivity", null);
    }

    // Start the service
    public void startService() {
        // Google activity recognition API
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 1001, new Intent("ActivityRecognitionResult"), PendingIntent.FLAG_CANCEL_CURRENT);
        ActivityRecognitionClient activityRecognitionClient = ActivityRecognition.getClient(mContext);
        // Register the update receiver
        activityRecognitionClient.requestActivityUpdates(MIN_UPDATE_TIME, pendingIntent);
        mContext.registerReceiver(this, new IntentFilter("ActivityRecognitionResult"));
    }

    // Get the most recent user activity
    public HashMap getUserActivity() {
        // Map the numeric to a string
        switch (mActivityType) {
            case 0:
                mUserActiv.put("UserActivity", "VEHICLE");
                break;
            case 1:
                mUserActiv.put("UserActivity", "BICYCLE");
                break;
            case 2:
                mUserActiv.put("UserActivity", "FOOT");
                break;
            case 3:
                mUserActiv.put("UserActivity", "STILL");
                break;
            default:
                mUserActiv.put("UserActivity", "UNKNOWN");
                break;
        }
        return mUserActiv;
    }

    // Stop the service
    public void stopService() {
        // Unregister the update receiver
        mContext.unregisterReceiver(this);
    }

    // Callback when receive a user activity result
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            mActivityType = result.getMostProbableActivity().getType();
        }
    }
}
