package fr.inria.yifan.mysensor.Support;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.support.v4.app.ActivityCompat.requestPermissions;

/**
 * This class provides functions including storing and reading sensing data file.
 */

public class FilesIOHelper {

    private static final String TAG = "File IO helper";

    // Declare file storage permissions
    @SuppressLint("InlinedApi")
    private static final String[] STORAGE_PERMS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private Context context;

    public FilesIOHelper(Context context) {
        super();
        this.context = context;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Requesting storage permission", Toast.LENGTH_SHORT).show();
            // Request permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions((Activity) context, STORAGE_PERMS, Configuration.PERMS_REQUEST_STORAGE);
            } else {
                Toast.makeText(context, "Please give storage permission", Toast.LENGTH_SHORT).show();
            }
        }
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
