package fr.inria.yifan.mysensor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import fr.inria.yifan.mysensor.Deprecated.SensingActivity;

/**
 * This activity has to be started in the beginning of the application to ensure all user permissions are enabled
 */

public class InitializeActivity extends AppCompatActivity {
// TODO

    private static final String TAG = "Initialization";

    private static final int PERMS_REQUEST_RECORD = 1000;
    private static final int PERMS_REQUEST_STORAGE = 1001;
    private static final int PERMS_REQUEST_LOCATION = 1002;

    // Declare microphone permissions
    private static final String[] RECORD_PERMS = {Manifest.permission.RECORD_AUDIO};

    // Declare file storage permissions
    private static final String[] STORAGE_PERMS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    // Declare GPS and network permissions
    private static final String[] LOCATION_PERMS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET};

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize);

        TextView welcomeView = findViewById(R.id.welcome_view);
        welcomeView.setText(R.string.hint_initial);

        checkPermission();
    }

    // Check related user permissions
    private void checkPermission() {
        // Check user permission for microphone
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this, "Requesting microphone permission", Toast.LENGTH_SHORT).show();
            // Request permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(RECORD_PERMS, PERMS_REQUEST_RECORD);
            } else {
                Toast.makeText(this, "Please give microphone permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        // Check user permission for file storage
        else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this, "Requesting storage permission", Toast.LENGTH_SHORT).show();
            // Request permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(STORAGE_PERMS, PERMS_REQUEST_STORAGE);
            } else {
                Toast.makeText(this, "Please give storage permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        // Check user permission for GPS location
        else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this, "Requesting location permission", Toast.LENGTH_SHORT).show();
            // Request permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(LOCATION_PERMS, PERMS_REQUEST_LOCATION);
            } else {
                Toast.makeText(this, "Please give location permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Permission checked OK", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    // Callback for user allowing permission
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMS_REQUEST_RECORD:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, String.valueOf(grantResults[0]));
                    Toast.makeText(this, "Please give microphone permission", Toast.LENGTH_LONG).show();
                }
                checkPermission();
                break;
            case PERMS_REQUEST_STORAGE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, String.valueOf(grantResults[0]));
                    Toast.makeText(this, "Please give storage permission", Toast.LENGTH_LONG).show();
                }
                checkPermission();
                break;
            case PERMS_REQUEST_LOCATION:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, String.valueOf(grantResults[0]));
                    Toast.makeText(this, "Please give location permission", Toast.LENGTH_LONG).show();
                }
                checkPermission();
                break;
        }
    }

    // Go to the context activity
    public void goContext(View view) {
        Intent goToContext = new Intent();
        goToContext.setClass(this, ContextActivity.class);
        startActivity(goToContext);
        finish();
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
