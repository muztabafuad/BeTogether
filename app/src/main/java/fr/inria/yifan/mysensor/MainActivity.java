package fr.inria.yifan.mysensor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    // Declare views
    private EditText milesEdit;
    private EditText kmEdit;
    private Button mileToKmButton;
    private Button kmToMileButton;

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        milesEdit = findViewById(R.id.editMiles);
        kmEdit = findViewById(R.id.editKm);
        mileToKmButton = findViewById(R.id.buttonMiltToKm);
        mileToKmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double miles = Double.valueOf(milesEdit.getText().toString());
                double km = miles / 0.62137;
                DecimalFormat formater = new DecimalFormat("##.##");
                kmEdit.setText(formater.format(km));
            }
        });
        kmToMileButton = findViewById(R.id.buttonKmtoMile);
        kmToMileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double km = Double.valueOf(kmEdit.getText().toString());
                double miles = km * 0.62137;
                DecimalFormat formater = new DecimalFormat("##.##");
                milesEdit.setText(formater.format(miles));
            }
        });


    }

}
