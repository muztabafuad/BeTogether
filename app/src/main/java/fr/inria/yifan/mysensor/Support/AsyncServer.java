package fr.inria.yifan.mysensor.Support;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static android.content.ContentValues.TAG;
import static fr.inria.yifan.mysensor.Support.Configuration.SERVER_PORT;

/**
 * Create a ServerSocket and waits for a connection from a client on a specified port in a background thread.
 */

public class AsyncServer extends AsyncTask<Void, Void, String> {

    @SuppressLint("StaticFieldLeak")
    private Context mContext;
    private Socket mClientSocket;
    private ServerSocket mServerSocket;

    public AsyncServer(Context context) {
        this.mContext = context;
    }

    @Override
    protected String doInBackground(Void... v) {
        try {
            /*
              Create a server socket and wait for client connections. This
              call blocks until a connection is accepted from a client
             */
            mServerSocket = new ServerSocket(SERVER_PORT);
            Socket client = mServerSocket.accept();

            InputStream inputstream = client.getInputStream();
            mServerSocket.close();
            return null;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + result), "image/*");
            mContext.startActivity(intent);
        }
    }

    public void connectTo(InetAddress host) {
        mClientSocket = new Socket();
        byte buf[] = new byte[1024];
        try {
            // Create a client socket with the host, port, and timeout information.
            mClientSocket.bind(null);
            mClientSocket.connect((new InetSocketAddress(host, SERVER_PORT)), 500);

            // Create a byte stream from a JPEG file and pipe it to the output stream of the socket. This data will be retrieved by the server device.
            OutputStream outputStream = mClientSocket.getOutputStream();
            ContentResolver cr = mContext.getContentResolver();
            InputStream inputStream = cr.openInputStream(Uri.parse("path/to/picture.jpg"));
            assert inputStream != null;
            int len = inputStream.read(buf);
            while (len != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (FileNotFoundException e) {
            //catch logic
        } catch (IOException e) {
            //catch logic
        }

        // Clean up any open sockets when done transferring or if an exception occurred.
        finally {
            if (mClientSocket.isConnected()) {
                try {
                    mClientSocket.close();
                } catch (IOException e) {
                    //catch logic
                }
            }
        }
    }

    public void close() {
        if (mClientSocket.isConnected()) {
            try {
                mClientSocket.close();
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}