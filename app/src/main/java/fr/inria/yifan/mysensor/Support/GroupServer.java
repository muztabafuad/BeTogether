package fr.inria.yifan.mysensor.Support;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static fr.inria.yifan.mysensor.Support.Configuration.SERVER_PORT;

/**
 * Create a ServerSocket and waits for a connection from a client on a specified port in a background thread.
 */

public class GroupServer {

    private static final String TAG = "Group server";

    // Declare client socket list and server socket
    private ArrayList<Socket> mList = new ArrayList<Socket>();

    public GroupServer() {
        try {
            ServerSocket server = new ServerSocket(SERVER_PORT);
            ExecutorService mExecutorService = Executors.newCachedThreadPool();
            Socket client;
            while (true) {
                client = server.accept();
                mList.add(client);
                mExecutorService.execute(new Service(client));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new GroupServer();
    }

    class Service implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private String msg;

        Service(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                msg = "Member " + this.socket.getInetAddress() + " has joined group, "
                        + "current members: " + mList.size();
                this.broadCastMsg();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if ((msg = in.readLine()) != null) {
                        if (msg.equals("bye")) {
                            mList.remove(socket);
                            in.close();
                            msg = "Member " + socket.getInetAddress() + " quited group, "
                                    + "current members: " + mList.size();
                            socket.close();
                            this.broadCastMsg();
                            break;
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
        void broadCastMsg() {
            int num = mList.size();
            for (int index = 0; index < num; index++) {
                Socket mSocket = mList.get(index);
                PrintWriter pout;
                try {
                    pout = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream(), "UTF-8")), true);
                    pout.println(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}