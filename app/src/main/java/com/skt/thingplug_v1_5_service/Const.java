package com.skt.thingplug_v1_5_service;

import android.hardware.Sensor;

/**
 * default values for application
 *
 * Copyright (C) 2017. SK Telecom, All Rights Reserved.
 * Written 2017, by SK Telecom
 */
public class Const {

    // ThingPlug URLs
    public static final String URL_JOIN_THINGPLUG = "https://thingplug.sktiot.com/";
    public static final String URL_LOGIN_DEFAULT = "https://thingplugtest.sktiot.com:9443";
    public static final String URL_SEARCH_DEFAULT = "https://thingplugtest.sktiot.com:9443";
    public static final String URL_REGISTER_DEFAULT = "https://thingplug.sktiot.com:443";
    public static final String URL_LOGIN = "/ThingPlug?division=user&function=login";
    public static final String URL_SEARCH_DEVICE = "/ThingPlug?division=searchDevice&function=myDevice&startIndex=1&countPerPage=50";
    public static final String URL_REGISTER_DEVICE = "/ThingPlug?division=device&function=regist";

    public static final String URL_SERVER_DEFAULT = "mqtt.thingplug.net:1883";
    public static final String SERVER_APPEUI_DEFAULT = "Android";
    public static final boolean USE_TLS_DEFAULT = false;
    public static final boolean USE_TLV_DEFAULT = false;
    public static final boolean SHOW_CONTENT_DEFAULT = false;

    // read time delay (msec)
    public static final int SENSOR_DEFAULT_READ_PERIOD = 1000;
    public static final int SENSOR_DEFAULT_TRANSFER_INTERVAL = 5000;
    public static final int SENSOR_DEFAULT_LIST_UPDATE_INTERVAL = 1000;
    public static final int SENSOR_DEFAULT_GRAPH_UPDATE_INTERVAL = 1000;
    public static final int SENSOR_MIN_READ_PERIOD = 100;
    public static final int SENSOR_MIN_TRANSFER_INTERVAL = 1000;
    public static final int SENSOR_MIN_LIST_UPDATE_INTERVAL = 1000;
    public static final int SENSOR_MIN_GRAPH_UPDATE_INTERVAL = 100;

    // sensor type definition
    public static final int SENSOR_TYPE_BATTERY = Sensor.TYPE_DEVICE_PRIVATE_BASE + 1;
    public static final int SENSOR_TYPE_GPS = Sensor.TYPE_DEVICE_PRIVATE_BASE + 2;
    public static final int SENSOR_TYPE_BUZZER = Sensor.TYPE_DEVICE_PRIVATE_BASE + 3;
    public static final int SENSOR_TYPE_LED = Sensor.TYPE_DEVICE_PRIVATE_BASE + 4;
    public static final int SENSOR_TYPE_CAMERA = Sensor.TYPE_DEVICE_PRIVATE_BASE + 5;
    public static final int SENSOR_TYPE_NOISE = Sensor.TYPE_DEVICE_PRIVATE_BASE + 6;

    // OneM2MWorker
    public static final String ONEM2M_TO = "/v1_0";

    public static final String CONTAINER_TTV_NAME = "TTV";
    public static final String CONTAINER_PHOTOURL_NAME = "PhotoURL";

    public static final String MGMTCMD_NAME = "Android";

    public static final int CONTROL_LOOKUP_INTERVAL = 500; // ms
    public static final int CONTROL_LOOKUP_MAX_COUNT = 6;

    public static final int CONTROL_CAMERA_LOOKUP_INTERVAL = 1000; // ms
    public static final int CONTROL_CAMERA_LOOKUP_MAX_COUNT = 20;

    public static final String EXECRESULT_SUCCESS = "0";
    public static final String EXECRESULT_DENIED = "2";
    public static final String EXECSTATUS_PENDING = "2";

    // ThingPlug 1.5 (oneM2M v1.14)
    public static final String MQTT_HOST = "mqtt.thingplug.net";
    public static final String MQTT_SECURE_HOST = "ssl://mqtt.thingplug.net";

    public static final String ONEM2M_DEVICE_ID = "(Enter Device ID here)";
    public static final String ONEM2M_SERVICE_ID = "(Enter Service ID here)";
    public static final String ONEM2M_AE_ID = "(Enter Service AE-ID here)";

    public static final String ACCOUNT_USER_ID = "(Enter ThingPlug ID here)";
    public static final String ACCOUNT_CREDENTIAL_ID = "(Enter ThingPlug Credential ID here)";

    public static final String SUBSCRIPTION_NAME = "android";
}