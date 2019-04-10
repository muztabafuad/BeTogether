package fr.inria.yifan.mysensor.Network;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkHelper extends AsyncTask {

    private static final String TAG = "Coordinator";

    private static final int PORT = 8888;

    @Override
    protected Object doInBackground(Object[] objects) {
        try {
            /*
             * Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            ServerSocket serverSocket = new ServerSocket(PORT);
            Socket client = serverSocket.accept();

            client.getInetAddress();

            serverSocket.close();
            return null;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    // Send a message to the destination
    public void sendMessageTo(String dest, String msg) {
        Socket socket = new Socket();
        try {
            // Create a client socket with the host, port, and timeout information.
            socket.bind(null);
            socket.connect((new InetSocketAddress(dest, PORT)), 500);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        // Clean up any open sockets when done transferring or if an exception occurred.
        finally {
            if (socket.isConnected()) {
                try {
                    socket.close();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }

    // Send a file to the destination
    public void sendFileTo(String dest, File file) {

    }
}
