package fr.inria.yifan.mysensor;

/*
 * This activity provides functions related to the Wifi Direct service.
 */

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import fr.inria.yifan.mysensor.Network.BluetoothHelper;

public class BluetoothActivity extends AppCompatActivity {

    private static final String TAG = "Bluetooth activity";


    // Bluetooth helper
    private BluetoothHelper mBluetoothHelper;


    // Main activity initialization
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

    }

    // Stop thread when exit!
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onPause() {
        super.onPause();
    }

    // Go to the sensing activity
    public void goSensing(View view) {
        Intent goToSensing = new Intent();
        goToSensing.setClass(this, SensingActivity.class);
        startActivity(goToSensing);
        finish();
    }

    // Go to the network activity
    public void goWifi(View view) {
        Intent goToWifi = new Intent();
        goToWifi.setClass(this, WifiActivity.class);
        startActivity(goToWifi);
        finish();
    }

}
