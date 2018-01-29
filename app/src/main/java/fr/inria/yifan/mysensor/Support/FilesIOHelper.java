package fr.inria.yifan.mysensor.Support;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class provides functions including storing and reading sensing data file.
 */

public class FilesIOHelper {

    private static final String TAG = "File IO helper";

    private Context context;

    public FilesIOHelper(Context context) {
        super();
        this.context = context;
    }

    // Write file to storage
    public void saveFile(String filename, String filecontent) throws Exception {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            filename = Environment.getExternalStorageDirectory().getCanonicalPath() + "/Documents/" + filename;
            //Log.d(TAG, filename);
            FileOutputStream output = new FileOutputStream(filename);
            output.write(filecontent.getBytes());
            output.close();
        } else {
            Toast.makeText(context, "Failed in writing file", Toast.LENGTH_SHORT).show();
        }
    }

    // Read file from storage
    public String readFile(String filename) throws IOException {
        StringBuilder sb = new StringBuilder("");
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            filename = Environment.getExternalStorageDirectory().getCanonicalPath() + "/Documents/" + filename;
            FileInputStream input = new FileInputStream(filename);
            byte[] temp = new byte[1024];
            int len;
            while ((len = input.read(temp)) > 0) {
                sb.append(new String(temp, 0, len));
            }
            input.close();
        } else {
            Toast.makeText(context, "Failed in writing file", Toast.LENGTH_SHORT).show();
        }
        return sb.toString();
    }
}
