package fr.inria.yifan.mysensor.Support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import static fr.inria.yifan.mysensor.Support.Configuration.SERVER_PORT;

/**
 * Create a ClientSocket and make connection to a server on a specified port.
 */

public class GroupClient {

    private Socket mClientSocket;

    public void connectTo(InetAddress host) {
        mClientSocket = new Socket();
        byte buf[] = new byte[1024];
        try {
            // Create a client socket with the host, port, and timeout information.
            mClientSocket.bind(null);
            mClientSocket.connect((new InetSocketAddress(host, SERVER_PORT)), 500);

            // Create a byte stream from a JPEG file and pipe it to the output stream of the socket. This data will be retrieved by the server device.
            OutputStream outputStream = mClientSocket.getOutputStream();
            /*
            InputStream inputStream = null;
            int len = inputStream.read(buf);
            while (len != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
            */
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
