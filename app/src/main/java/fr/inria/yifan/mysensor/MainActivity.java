package fr.inria.yifan.mysensor;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.text.DecimalFormat;

/*
* This activity provides a demo of interactive UI.
*/

public class MainActivity extends AppCompatActivity {

    // Declare all views
    private EditText milesEdit;
    private EditText kmEdit;
    private Button mileToKmButton;
    private Button kmToMileButton;
    private BottomNavigationView navigation = findViewById(R.id.navigation);

    // Navigator button events listener
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    //PASS
                    return true;
                case R.id.navigation_dashboard:
                    //PASS
                    return true;
                case R.id.navigation_notifications:
                    //PASS
                    return true;
            }
            return false;
        }
    };

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        milesEdit = findViewById(R.id.editMiles);
        kmEdit = findViewById(R.id.editKm);
        mileToKmButton = findViewById(R.id.buttonMiltToKm);
        kmToMileButton = findViewById(R.id.buttonKmtoMile);

        mileToKmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double miles = Double.valueOf(milesEdit.getText().toString());
                double km = miles / 0.62137;
                DecimalFormat formatter = new DecimalFormat("##.##");
                kmEdit.setText(formatter.format(km));
            }
        });

        kmToMileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double km = Double.valueOf(kmEdit.getText().toString());
                double miles = km * 0.62137;
                DecimalFormat formatter = new DecimalFormat("##.##");
                milesEdit.setText(formatter.format(miles));
            }
        });
    }

}
