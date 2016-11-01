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
        type = IOT_DEVICE_TYPE_SOCKET;
    }

    public smartomeSocket() {

    }

    public static smartomeSocket generate_dummy_device() {
        smartomeSocket s = new smartomeSocket();
        s.type = IOT_DEVICE_TYPE_UNKNOWN;
        return s;
    }

    public boolean initiate_discovery(discoveryProtocol prot) {
        int i = 0;
        try {
            prot.dgram_sock =
                    new DatagramSocket(SMARTOME_SOCKET_DISCOVERY_PORT, InetAddress.getByName("0.0.0.0"));
            prot.dgram_sock.setBroadcast(true);
            prot.dgram_sock.setSoTimeout(1000);

            String message = "Probe#2016-09-24-09-05-48-6";
            byte[] sendData = message.getBytes();

            // Send 2 requests for redundancy
            while (i < 2) {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, prot.myBroadcastAddress, SMARTOME_SOCKET_DISCOVERY_PORT);
                prot.dgram_sock.send(sendPacket);
                i = i + 1;
                Log.d("this", "Sent probe " + String.valueOf(i) + " for nearby Smartome devices");
                Thread.sleep(200, 0);
            }

        } catch (Exception e) {
            Log.d("this", "Exception during Smartome Socket initiate_discovery: " + e.getMessage());
            return false;
        }

        return true;
    }

    public boolean requires_multicast_socket()
    {
        return true;
    }

    public iotDevice scan_devices(discoveryProtocol prot) {
        try {
            do {
                byte[] recvBuf = new byte[1500];
                DatagramPacket pak = new DatagramPacket(recvBuf, recvBuf.length);

                prot.dgram_sock.receive(pak);
                String response_code = new String(pak.getData(), Charset.forName("UTF-8"));
                String probe = response_code.substring(0, 5);

                if (probe.equals("Probe")) {
                    // This is our probe message received by the stack due to broadcast
                    // Ignore it
                    Log.d("this", "Ignoring the looped back Probe message");
                    continue;
                }

                String dev_id = response_code.substring(0, 14);  // Looking for HD1-12903-043c type
                String addr = pak.getAddress().getHostAddress();

                Log.d("this", "Scanned device: " + dev_id + " from " + addr);
                smartomeSocket smartSock = new smartomeSocket(dev_id, addr);
                return smartSock;
            } while (true);

        } catch (SocketTimeoutException timeout) {
            // No more devices
            Log.d("this", "No more devices responded");
            return null;

        } catch (Exception e) {
            Log.d("this", "Unexpected exception in scan_devices: " + e.getMessage());
            return null;
        }

    }

    public void cleanup_discovery(discoveryProtocol prot) {
        if (prot.dgram_sock != null) {
            prot.dgram_sock.close();
            prot.dgram_sock = null;
        }
    }

    public int get_status() {
        try {
            Socket sock = new Socket(deviceAddr, SMARTOME_SOCKET_COMM_PORT);
            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
            InputStream in = sock.getInputStream();
            status = IOT_DEVICE_STATUS_UNKNOWN;

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

                if (response_payload.contains("\"on\":\"0\"")) {
                    status = iotDevice.IOT_DEVICE_STATUS_OFF;
                } else if (response_payload.contains("\"on\":\"1\"")) {
                    status = iotDevice.IOT_DEVICE_STATUS_ON;
                } else {
                    Log.d("this", "Unknown status response from smartome socket");
                    status = iotDevice.IOT_DEVICE_STATUS_UNKNOWN;
                }
            }

            in.close();
            out.close();
            sock.close();

            return status;

        } catch (Exception e) {
            Log.d("this", "Exception during smartomeSocket refresh_status: " + e.getMessage());
            return iotDevice.IOT_DEVICE_STATUS_UNKNOWN;
        }

    }

    public int change_state(int new_status) {
        try {

            String status_string = null;

            if (new_status == IOT_DEVICE_STATUS_ON) {
                status_string = "1";
            } else if (new_status == IOT_DEVICE_STATUS_OFF) {
                status_string = "0";
            } else if (new_status == IOT_DEVICE_STATUS_UNKNOWN) {
                // STATUS UNKNOWN is actually a GET STATE QUERY
                return get_status();
            }

            Socket sock = new Socket(deviceAddr, SMARTOME_SOCKET_COMM_PORT);
            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
            InputStream in = sock.getInputStream();

            Log.d("this", "Changing socket state to: " + status_string);

            // Send status request
            String status_cmd = "##0053{\"app_cmd\":\"11\",\"imei\":\"" + deviceId + "\",\"SubDev\":\"00\",\"on\":\"" + status_string + "\",\"sk\":\"0\",\"seq\":\"21\"}&&";
            Log.d("this", "Smartome_socket: Sending status cmd: " + status_cmd);
            out.println(status_cmd);

            // Read response, looking for:
            //##00a4{"wifi_cmd":"12","SubDev":"00","onoff":[{"on":"0","on1":"0","dtm":"0","ntm":"","on2":"0","sk":"0"}],"vol":"0","cur":"0","pow":"0","eng":"0","ver":"1.1.5","suc":"0"}&&
            byte[] buf = new byte[6];
            in.read(buf);

            String response_code = new String(buf, Charset.forName("UTF-8"));

            Log.d("this", "Smartome_socket: Got status response code: " + response_code);
            if (response_code.contains("##001d")) {
                // Read valid response, read rest of the packet
                buf = new byte[31];
                in.read(buf);

                String response_payload = new String(buf, Charset.forName("UTF-8"));
                Log.d("this", "Smartome socket: status response payload: " + response_payload);

                if (response_payload.contains("\"result\":\"0\"")) {
                    status = new_status;
                } else {
                    Log.d("this", "Unknown status response from smartome socket");
                    status = iotDevice.IOT_DEVICE_STATUS_UNKNOWN;
                }
            }

            in.close();
            out.close();
            sock.close();

            return status;

        } catch (Exception e) {
            Log.d("this", "Exception during smartomeSocket refresh_status: " + e.getMessage());
            status = iotDevice.IOT_DEVICE_STATUS_UNKNOWN;
            return iotDevice.IOT_DEVICE_STATUS_UNKNOWN;
        }

    }
}
