package fr.inria.yifan.mysensor.Transmission;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides methods related to the Client/Server socket communication.
 */

public abstract class NetworkHelper {

    private static final String TAG = "Network helper";

    // Using a large number unused port for socket
    private static final int PORT = 28005;

    // Accepted client sockets on server
    private List<Socket> mClients;

    // Constructor
    protected NetworkHelper(boolean isServer) {
        // The server listens for incoming socket
        if (isServer) {
            mClients = new ArrayList<>();
            keepListenServer();
        }
    }

    // Clear all client socket on server side
    public void clearSockets() {
        if (!mClients.isEmpty()) {
            mClients.clear();
        }
    }

    // Keep listening on the port, for the server
    private void keepListenServer() {
        // Network operation must use a new thread
        new Thread(() -> {
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;
            try {
                // Create a server socket and wait for client connections
                // This call blocks until a connection is accepted from a client
                ServerSocket serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));
                //Log.e(TAG, "Server socket is created!");

                // Loop for listening
                while (true) {
                    socket = serverSocket.accept();
                    mClients.add(socket);

                    // Thread will wait till message received
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    String msg = dataInputStream.readUTF();
                    if (msg != null) {
                        // Handle the incoming message and get the reply from the callback
                        JSONObject reply = callbackReceiveReply(new JSONObject(msg), socket.getInetAddress().getHostAddress());
                        if (reply != null) {
                            // Transfer JSONObject as String to reply the client
                            dataOutputStream = new DataOutputStream(socket.getOutputStream());
                            dataOutputStream.writeUTF(reply.toString());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Clean up any open sockets when done transferring or if an exception occurred.
            finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                    if (dataInputStream != null) {
                        dataInputStream.close();
                    }
                    if (dataOutputStream != null) {
                        dataOutputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Callback when receives a JSON message, return the message to reply
    public abstract JSONObject callbackReceiveReply(JSONObject msg, String source);

    // Send a message to the destination and wait for a reply, for client
    public void sendAndReceive(String dest, JSONObject msg) {
        // Network operation must use a new thread
        new Thread(() -> {
            Socket socket = new Socket();
            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;
            try {
                // Create a client socket with the host, port, and timeout information.
                socket.bind(null);
                socket.connect((new InetSocketAddress(dest, PORT)), 500);

                // Transfer JSONObject as String to the server
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeUTF(msg.toString());

                // Read the reply, thread will wait till server replies
                dataInputStream = new DataInputStream(socket.getInputStream());
                String reply = dataInputStream.readUTF();
                if (reply != null) {
                    callbackReceiveReply(new JSONObject(reply), socket.getInetAddress().getHostAddress());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Clean up any open sockets when done transferring or if an exception occurred.
            finally {
                try {
                    if (socket.isConnected()) {
                        socket.close();
                    }
                    if (dataOutputStream != null) {
                        dataOutputStream.close();
                    }
                    if (dataInputStream != null) {
                        dataInputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Send a message to the destination only, for client
    void sendOnly(String dest, JSONObject msg) {
        // Network operation must use a new thread
        new Thread(() -> {
            Socket socket = new Socket();
            DataOutputStream dataOutputStream = null;
            try {
                // Create a client socket with the host, port, and timeout information.
                socket.bind(null);
                socket.connect((new InetSocketAddress(dest, PORT)), 500);

                // Transfer JSONObject as String to the destination
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeUTF(msg.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Clean up any open sockets when done transferring or if an exception occurred.
            finally {
                try {
                    if (socket.isConnected()) {
                        socket.close();
                    }
                    if (dataOutputStream != null) {
                        dataOutputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Send a file to the destination only, for client
    public void sendFileTo(String dest, File file) {
        //TODO
    }

}
