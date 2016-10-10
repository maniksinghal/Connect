package com.wordpress.randomexplorations.connect;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;

/**
 * Created by maniksin on 9/24/16.
 */
public class smartomeSocket extends iotDevice {

    private static final int SMARTOME_SOCKET_COMM_PORT = 6002;
    private static final int SMARTOME_SOCKET_DISCOVERY_PORT = 15002;

    public smartomeSocket(String id, String addr) {
        deviceId = id;
        deviceAddr = addr;
    }

    public final static boolean initiate_discovery(discoveryProtocol prot) {
        try {
            prot.dgram_sock =
                    new DatagramSocket(SMARTOME_SOCKET_DISCOVERY_PORT, InetAddress.getByName("0.0.0.0"));
            prot.dgram_sock.setBroadcast(true);
            prot.dgram_sock.setSoTimeout(1000);

            String message = "Probe#2016-09-24-09-05-48-6";
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, prot.myBroadcastAddress, SMARTOME_SOCKET_DISCOVERY_PORT);
            prot.dgram_sock.send(sendPacket);
            Log.d("this", "Sent probe for nearby Smartome devices");

        } catch (Exception e) {
            Log.d("this", "Exception during Smartome Socket initiate_discovery: " + e.getMessage());
            return false;
        }

        return true;
    }

    public final static iotDevice scan_devices(discoveryProtocol prot) {
        try {
            byte[] recvBuf = new byte[1500];
            DatagramPacket pak = new DatagramPacket(recvBuf, recvBuf.length);

            prot.dgram_sock.receive(pak);
            String response_code = new String(pak.getData(), Charset.forName("UTF-8"));
            String dev_id = response_code.substring(0, 14);  // Looking for HD1-12903-043c type
            String addr = pak.getAddress().getHostAddress();

            Log.d("this", "Scanned device: " + response_code + " from " + addr);
            smartomeSocket smartSock = new smartomeSocket(dev_id, addr);
            return smartSock;

        } catch (SocketTimeoutException timeout) {
            // No more devices
            Log.d("this", "No more devices responded");
            return null;

        } catch (Exception e) {
            Log.d("this", "Unexpected exception in scan_devices: " + e.getMessage());
            return null;
        }

    }

    public final static void cleanup_discovery(discoveryProtocol prot) {
        if (prot.dgram_sock != null) {
            prot.dgram_sock.close();
            prot.dgram_sock = null;
        }
    }

    public int refresh_status() {
        try {
            Socket sock = new Socket(deviceAddr, SMARTOME_SOCKET_COMM_PORT);
            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
            InputStream in = sock.getInputStream();

            // Send status request
            String status_cmd = "##0041{\"app_cmd\":\"12\",\"imei\":\"" + deviceId + "\",\"SubDev\":\"00\",\"seq\":\"26\"}&&";
            Log.d("this", "Smartome_socket: Sending status cmd: " + status_cmd);
            out.println(status_cmd);

            // Read response, looking for:
            //##00a4{"wifi_cmd":"12","SubDev":"00","onoff":[{"on":"0","on1":"0","dtm":"0","ntm":"","on2":"0","sk":"0"}],"vol":"0","cur":"0","pow":"0","eng":"0","ver":"1.1.5","suc":"0"}&&
            byte[] buf = new byte[6];
            in.read(buf);

            String response_code = new String(buf, Charset.forName("UTF-8"));

            Log.d("this", "Smartome_socket: Got status response code: " + response_code);
            if (response_code.contains("##00a4")) {
                // Read valid response, read rest of the packet
                buf = new byte[166];
                in.read(buf);

                String response_payload = new String(buf, Charset.forName("UTF-8"));
                Log.d("this", "Smartome socket: status response payload: " + response_payload);
                return iotDevice.IOT_DEVICE_PRESENCE_AVAILABLE;
            }

            in.close();
            out.close();
            sock.close();

            return iotDevice.IOT_DEVICE_PRESENCE_REMOVED;

        } catch (Exception e) {
            Log.d("this", "Exception during smartomeSocket refresh_status: " + e.getMessage());
            return iotDevice.IOT_DEVICE_PRESENCE_REMOVED;
        }

    }

}
