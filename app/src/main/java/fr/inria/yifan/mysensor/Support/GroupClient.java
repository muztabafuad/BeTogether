package fr.inria.yifan.mysensor.Support;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import static fr.inria.yifan.mysensor.Support.Configuration.SERVER_PORT;

/**
 * Create a ClientSocket and make connection to a server on a specified port.
 */

public class GroupClient {

    private static final String TAG = "Group client";

    // Socket and thread flag
    private Socket mClientSocket;
    private boolean isClientRun;

    // Buffer in and out for socket
    private BufferedReader in;
    private PrintWriter out;

    // Declare content string
    private String mContent;

    public GroupClient(final InetAddress server) {
        isClientRun = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mClientSocket = new Socket(server, SERVER_PORT);
                    in = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream(), "UTF-8"));
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mClientSocket.getOutputStream())), true);
                    while (isClientRun) {
                        if (mClientSocket.isConnected() && !mClientSocket.isInputShutdown()) {
                            if ((mContent = in.readLine()) != null) {
                                mContent += "\n";
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public String readMessage() {
        return mContent;
    }

    public void sendMessage(String msg) {
        if (mClientSocket.isConnected() && !mClientSocket.isOutputShutdown()) {
            out.println(msg);
        }
    }
}
