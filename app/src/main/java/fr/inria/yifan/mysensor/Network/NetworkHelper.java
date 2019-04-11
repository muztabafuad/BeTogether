package fr.inria.yifan.mysensor.Network;

import android.util.Log;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class NetworkHelper {

    private static final String TAG = "Network helper";

    private static final int PORT = 8888;

    // Type: Data, ...
    private JSONObject DataMsg;

    protected NetworkHelper() {
        Runnable r = () -> {
            Socket socket = null;
            DataInputStream dataInputStream = null;
            try {
                /*
                 * Create a server socket and wait for client connections. This
                 * call blocks until a connection is accepted from a client
                 */
                ServerSocket serverSocket = new ServerSocket(PORT);
                Log.e(TAG, "Server socket is created!");
                while (true) {
                    socket = serverSocket.accept();
                    dataInputStream = new DataInputStream(socket.getInputStream());

                    // Thread will wait till message received
                    String msg = dataInputStream.readUTF();
                    if (msg != null) {
                        callbackReceived(new JSONObject(msg));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                    if (dataInputStream != null) {
                        dataInputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Thread thread = new Thread(r);
        thread.start();
    }

    // Callback when receive a JSON message from socket
    public abstract void callbackReceived(JSONObject msg);

    // Send a message to the destination
    public void sendMessageTo(String dest, JSONObject msg) {
        Socket socket = new Socket();
        DataOutputStream dataOutputStream;

        try {
            // Create a client socket with the host, port, and timeout information.
            socket.bind(null);
            socket.connect((new InetSocketAddress(dest, PORT)), 500);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            // transfer JSONObject as String to the server
            dataOutputStream.writeUTF(msg.toString());
            dataOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Clean up any open sockets when done transferring or if an exception occurred.
        finally {
            if (socket.isConnected()) {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Send a file to the destination
    public void sendFileTo(String dest, File file) {

    }
}
