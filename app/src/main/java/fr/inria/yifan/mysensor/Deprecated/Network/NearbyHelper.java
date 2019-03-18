package fr.inria.yifan.mysensor.Deprecated.Network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import static com.google.android.gms.nearby.connection.Strategy.P2P_CLUSTER;

/**
 * This class provides network methods based on Nearby technology.
 */

public class NearbyHelper {

    private static final String TAG = "Nearby helper";

    private static final String ServiceID = "CrowdSensingGroup";

    private Context mContext;

    // Callback when received the payload from the other
    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                    // A new payload is being sent over.
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
                    // Payload progress has updated.
                }
            };

    // Callback when the Nearby connection state is changed
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    Nearby.getConnectionsClient(mContext).acceptConnection(endpointId, mPayloadCallback);
                }

                @Override
                public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            break;
                        default:
                            // Unknown status code
                    }
                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                }
            };

    // Callback when the discoverer have find an advertiser
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
                    // An endpoint was found. We request a connection to it.
                    Log.e(TAG, "Found an end-point: " + endpointId);
                    //Nearby.getConnectionsClient(mContext).requestConnection("Discoverer", endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(@NonNull String endpointId) {
                    // A previously discovered endpoint has gone away.
                }
            };

    // Constructor initialization
    public NearbyHelper(Context context) {
        mContext = context;
    }

    public void startAdvertising() {
        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder().setStrategy(P2P_CLUSTER).build();
        Nearby.getConnectionsClient(mContext).startAdvertising("Advertiser", ServiceID, connectionLifecycleCallback, advertisingOptions);
    }

    public void startDiscovery() {
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().setStrategy(P2P_CLUSTER).build();
        Nearby.getConnectionsClient(mContext).startDiscovery(ServiceID, endpointDiscoveryCallback, discoveryOptions);
    }

    public void stopAdvertising() {
        Nearby.getConnectionsClient(mContext).stopAdvertising();
    }

    public void stopDiscovery() {
        Nearby.getConnectionsClient(mContext).stopDiscovery();
    }

    // Send the hash map via byte payload
    public void sendMapPayload(HashMap map) {
        try {
            // Convert Map to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(map);
            Payload payload = Payload.fromBytes(bos.toByteArray());
            oos.close();
            bos.close();
            //Once connected, devices can send payloads to each other.
            Nearby.getConnectionsClient(mContext).sendPayload("toEndpointId", payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read the hash map from byte payload
    public HashMap<String, Object> receiveMapPayload(byte[] byt) {
        try {
            // Parse byte array to Map
            ByteArrayInputStream bis = new ByteArrayInputStream(byt);
            ObjectInputStream ois = new ObjectInputStream(bis);
            @SuppressWarnings("unchecked")
            HashMap<String, Object> map = (HashMap<String, Object>) ois.readObject();
            ois.close();
            bis.close();
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
