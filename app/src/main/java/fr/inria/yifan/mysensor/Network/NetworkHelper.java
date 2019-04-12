package fr.inria.yifan.mysensor.Network;

import android.util.Log;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public abstract class NetworkHelper {

    private static final String TAG = "Network helper";

    private static final int PORT = 28005;

    // Type: Data, ...
    private JSONObject DataMsg;

    // Accepted client sockets
    private List<Socket> mClients;

    // Constructor
    NetworkHelper(boolean isServer) {
        // The server listens for incoming socket
        if (isServer) {
            mClients = new ArrayList<>();
            new Thread(() -> {
                Socket socket = null;
                DataInputStream dataInputStream = null;
                DataOutputStream dataOutputStream = null;
                try {
                    /*
                     * Create a server socket and wait for client connections. This
                     * call blocks until a connection is accepted from a client
                     */
                    ServerSocket serverSocket = new ServerSocket(); // <-- create an unbound socket first
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(PORT)); // <-- now bind it
                    Log.e(TAG, "Server socket is created!");

                    while (true) {
                        socket = serverSocket.accept();
                        mClients.add(socket);
                        dataInputStream = new DataInputStream(socket.getInputStream());

                        // Thread will wait till message received
                        String msg = dataInputStream.readUTF();
                        if (msg != null) {
                            JSONObject reply = serverCallbackReceiveReply(new JSONObject(msg), socket.getInetAddress().getHostAddress());
                            if (reply != null) {
                                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                                // Transfer JSONObject as String to the server
                                dataOutputStream.writeUTF(reply.toString());
                            }
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
                        if (dataOutputStream != null) {
                            dataOutputStream.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    // Clear all client socket on server side
    public void clearSockets() {
        mClients.clear();
    }

    // Send a message to the destination and wait for reply
    void sendAndReceive(String dest, JSONObject msg) {
        // Network operation must use a new thread
        new Thread(() -> {
            Socket socket = new Socket();
            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;
            try {
                // Create a client socket with the host, port, and timeout information.
                socket.bind(null);
                socket.connect((new InetSocketAddress(dest, PORT)), 500);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                // Transfer JSONObject as String to the server
                dataOutputStream.writeUTF(msg.toString());

                // Read the reply
                dataInputStream = new DataInputStream(socket.getInputStream());
                // Thread will wait till server replies
                String res = dataInputStream.readUTF();
                if (res != null) {
                    JSONObject reply = clientCallbackReceiveReply(new JSONObject(res), socket.getInetAddress().getHostAddress());
                    if (reply != null) {
                        dataOutputStream = new DataOutputStream(socket.getOutputStream());
                        // Transfer JSONObject as String to the server
                        dataOutputStream.writeUTF(reply.toString());
                    }
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

    // Callback when server receive a JSON message, return the message to reply
    abstract JSONObject serverCallbackReceiveReply(JSONObject msg, String source);

    // Callback when client receive a JSON message, return the message to reply
    abstract JSONObject clientCallbackReceiveReply(JSONObject msg, String source);

    // Send a message to the client
    void serverReply(String dest, JSONObject msg) {


    }

    // Send a message to the destination only
    void sendOnly(String dest, JSONObject msg) {
        // Network operation must use a new thread
        new Thread(() -> {
            Socket socket = new Socket();
            DataOutputStream dataOutputStream = null;
            try {
                // Create a client socket with the host, port, and timeout information.
                socket.bind(null);
                socket.connect((new InetSocketAddress(dest, PORT)), 500);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                // Transfer JSONObject as String to the server
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

    // Send a file to the destination
    public void sendFileTo(String dest, File file) {
        //TODO
    }

}
