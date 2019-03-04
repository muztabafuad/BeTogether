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

import static fr.inria.yifan.mysensor.Context.FeatureHelper.MIN_UPDATE_TIME;

/**
 * This class provides context information about the user activity.
 * The key to retrieve the value is "UserActivity".
 */

public class UserActivity extends BroadcastReceiver {

    private static final String TAG = "User activity";

    // Variables
    private Context mContext;
    private int mActivityType;
    private HashMap<String, String> mUserActiv;

    // Constructor initialization
    public UserActivity(Context context) {
        mContext = context;
        mActivityType = -1;
        mUserActiv = new HashMap<>();
        mUserActiv.put("UserActivity", null);
    }

    public void startService() {
        // Google activity recognition API
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 1001, new Intent("ActivityRecognitionResult"), PendingIntent.FLAG_CANCEL_CURRENT);
        ActivityRecognitionClient activityRecognitionClient = ActivityRecognition.getClient(mContext);
        // Register the update receiver
        activityRecognitionClient.requestActivityUpdates(MIN_UPDATE_TIME, pendingIntent);
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
