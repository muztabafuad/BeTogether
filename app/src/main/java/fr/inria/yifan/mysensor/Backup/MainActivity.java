package fr.inria.yifan.mysensor.Backup;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import fr.inria.yifan.mysensor.Inference.InferHelper;
import fr.inria.yifan.mysensor.R;

public class MainActivity extends AppCompatActivity {

    private static final String INPUT_NODE = "I";
    private static final String OUTPUT_NODE = "O";

    private static final int[] INPUT_SIZE = {1, 3};
    private InferHelper inferHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inferHelper = new InferHelper(this);

        final Button button = findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            public void onClick(View v) {

                final EditText editNum1 = findViewById(R.id.editNum1);
                final EditText editNum2 = findViewById(R.id.editNum2);
                final EditText editNum3 = findViewById(R.id.editNum3);

                float num1 = Float.parseFloat(editNum1.getText().toString());
                float num2 = Float.parseFloat(editNum2.getText().toString());
                float num3 = Float.parseFloat(editNum3.getText().toString());

                float[] inputFloats = {num1, num2, num3};

                float[] resu = {0, 0};

                final TextView textViewR = findViewById(R.id.txtViewResult);
                textViewR.setText(Float.toString(resu[0]) + ", " + Float.toString(resu[1]));
            }
        });

    }


}
