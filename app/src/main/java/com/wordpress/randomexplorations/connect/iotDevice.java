package com.wordpress.randomexplorations.connect;

/**
 * Created by maniksin on 9/24/16.
 */
public abstract class iotDevice {

    // Device Presence
    public static final int IOT_DEVICE_PRESENCE_UNKNOWN = 0;
    public static final int IOT_DEVICE_PRESENCE_AVAILABLE = 1;
    public static final int IOT_DEVICE_PRESENCE_REMOVED = 2;

    // Device state
    public static final int IOT_DEVICE_STATUS_UNKNOWN = 100;
    public static final int IOT_DEVICE_STATUS_ON = 101;
    public static final int IOT_DEVICE_STATUS_OFF = 102;

    // Device types
    public static final int IOT_DEVICE_TYPE_UNKNOWN = 1000;
    public static final int IOT_DEVICE_TYPE_SOCKET = 1001;

    public String deviceId = null;
    public String deviceAddr = null;
    public int type = IOT_DEVICE_TYPE_UNKNOWN;
    public int status = IOT_DEVICE_STATUS_UNKNOWN;

    public abstract int get_status();
    public abstract int change_state(int new_status);


    /*
    * Abstract API for device discovery
    * The client shall call the APIs in following sequence
    * - initiate_discovery
    *   If the API returns true, call -scan_devices immediately in a loop
    *   till the API keeps returning non-null iotDevices
    * - cleanup_discovery
     */
    public abstract boolean initiate_discovery(discoveryProtocol prot);
    public abstract iotDevice scan_devices(discoveryProtocol prot);
    public abstract void cleanup_discovery(discoveryProtocol prot);

    public abstract boolean requires_multicast_socket();

    public boolean matches(iotDevice dev) {
        return (deviceAddr.equals(dev.deviceAddr) && deviceId.equals(dev.deviceId));
    }


    public iotDevice() {
    }


}
