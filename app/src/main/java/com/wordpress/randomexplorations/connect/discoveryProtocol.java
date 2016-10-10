package com.wordpress.randomexplorations.connect;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by maniksin on 10/10/16.
 * This class is used transparently by the APP UI.
 * It is used internally by the IoT device agents to maintain the
 * protocol states while discovering their devices.
 */
public class discoveryProtocol {

    // Parameters filled by client
    InetAddress myBroadcastAddress;

    // Parameters used internally by the iotDevice functions
    DatagramSocket dgram_sock;
}
