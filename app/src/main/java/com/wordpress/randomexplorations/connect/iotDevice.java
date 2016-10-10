package com.wordpress.randomexplorations.connect;

/**
 * Created by maniksin on 9/24/16.
 */
public abstract class iotDevice {

    public static final int IOT_DEVICE_PRESENCE_UNKNOWN = 0;
    public static final int IOT_DEVICE_PRESENCE_AVAILABLE = 1;
    public static final int IOT_DEVICE_PRESENCE_REMOVED = 2;

    public static final int IOT_DEVICE_STATUS_ON = 100;
    public static final int IOT_DEVICE_STATUS_OFF = 101;

    public String deviceId = null;
    public String deviceAddr = null;

    public abstract int refresh_status();


    public iotDevice() {}

}
