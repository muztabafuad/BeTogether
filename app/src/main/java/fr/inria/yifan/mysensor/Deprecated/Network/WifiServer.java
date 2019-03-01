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
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static fr.inria.yifan.mysensor.Deprecated.Support.Configuration.SERVER_PORT;

/**
 * Create a ServerSocket and waits for a connection from a client on a specified port in a background thread.
 */

public class WifiServer {

    private static final String TAG = "Wifi-Direct server";

    // Server socket loop thread flag
    private boolean isServerRun;

    // Declare client sockets in list
    private ArrayList<Socket> mClientList = new ArrayList<>();

    // Constructor
    WifiServer() {
        isServerRun = true;
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                try {
                    ServerSocket server = new ServerSocket(SERVER_PORT);
                    ExecutorService executorService = Executors.newCachedThreadPool();
                    while (isServerRun) {
                        Socket client = server.accept();
                        mClientList.add(client);
                        executorService.execute(new Service(client));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Send message to a specific client
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean sendMsgTo(String msg, InetAddress destination) {
        for (Socket client : mClientList) {
            if (client.getInetAddress() == destination) {
                PrintWriter pout;
                try {
                    pout = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8)), true);
                    pout.println(msg);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    // Thread for each client socket
    class Service implements Runnable {
        private String msg;
        private Socket socket;
        private BufferedReader in;

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        Service(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                msg = "Member " + this.socket.getInetAddress() + " has joined group, " + "current members: " + mClientList.size();
                this.broadCastMsg();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            try {
                while (isServerRun) {
                    if ((msg = in.readLine()) != null) {
                        if (msg.equals("bye")) {
                            mClientList.remove(socket);
                            in.close();
                            socket.close();
                            msg = "Member " + socket.getInetAddress() + " quited group, " + "current members: " + mClientList.size();
                            this.broadCastMsg();
                        } else {
                            msg = socket.getInetAddress() + " said: " + msg;
                            this.broadCastMsg();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Send message to all clients
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        private void broadCastMsg() {
            int num = mClientList.size();
            for (int index = 0; index < num; index++) {
                Socket client = mClientList.get(index);
                PrintWriter pout;
                try {
                    pout = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8)), true);
                    pout.println(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}