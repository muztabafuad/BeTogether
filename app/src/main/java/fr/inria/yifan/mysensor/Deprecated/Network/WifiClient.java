package fr.inria.yifan.mysensor.Deprecated.Network;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static fr.inria.yifan.mysensor.Deprecated.Support.Configuration.SERVER_PORT;

/**
 * Create a ClientSocket and make connection to a server on a specified port.
 */

public class WifiClient {

    private static final String TAG = "Wifi-Direct client";

    // Socket and thread flag
    private Socket mClientSocket;
    private boolean isClientRun;

    // Buffer in and out for socket
    private BufferedReader in;
    private PrintWriter out;

    // Declare content string
    private String mContent;

    // Constructor
    WifiClient(final InetAddress server) {
        isClientRun = true;
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                try {
                    mClientSocket = new Socket(server, SERVER_PORT);
                    in = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream(), StandardCharsets.UTF_8));
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

    // Read current message in memory
    public String readMessage() {
        return mContent;
    }

    // Upload a message to server
    public void uploadMessage(String msg) {
        if (mClientSocket.isConnected() && !mClientSocket.isOutputShutdown()) {
            out.println(msg);
        }
    }

}
