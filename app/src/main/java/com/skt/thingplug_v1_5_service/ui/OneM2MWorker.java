package com.skt.thingplug_v1_5_service.ui;

import android.content.Context;
import android.util.Log;

import com.skt.thingplug_v1_5_service.Const;
import com.skt.thingplug_v1_5_service.data.UserInfo;

import tp.skt.onem2m_v1_14.api.IMQTT;
import tp.skt.onem2m_v1_14.api.MQTTProcessor;
import tp.skt.onem2m_v1_14.api.oneM2MAPI_V1_14;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.Binder;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.Definitions;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.push.execInstanceControl;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.push.notification;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.request.contentInstance;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.request.execInstance;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.request.mgmtCmd;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.request.subscription;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.response.subscriptionResponse;
import tp.skt.onem2m_v1_14.net.mqtt.MQTTCallback;
import tp.skt.onem2m_v1_14.net.mqtt.MQTTClient;
import tp.skt.onem2m_v1_14.net.mqtt.MQTTConfiguration;

/**
 * oneM2M SDK handler
 * <p>
 * Copyright (C) 2017. SK Telecom, All Rights Reserved.
 * Written 2017, by SK Telecom
 */
public class OneM2MWorker {

    private MQTTClient MQTTClient;
    private IMQTT mqttService;
    private MQTTConfiguration config;

    // client id
    private String mClientID = "";
    // binder
    private Binder mBinder;

    private String accountID;
    private String accountPassword;
    private String uKey;

    private final int MAX_CLIENT_ID_LENGTH = 23;

    //
    private static OneM2MWorker oneM2MWorker;

    private static final String TAG = "TP_ONEM2M_WORKER";

    private Context context;
    private String macAddress;

    private StateListener stateListener;

    /**
     * working state listener
     */
    public interface StateListener {
        enum RESULT {
            FAIL,
            SUCCESS,
            SUSPEND,
        }

        void onConnected(boolean result, String accountID, String accountPassword, String uKey);

        void onDisconnected(boolean result);

        void onRegistered(boolean result, String dKey, String nodeLink);

        void onUnregistered(boolean result);

        RESULT onReceiveCommand(execInstanceControl control);

        void onSensorInfoReceived(notification sensorInfo);
    }

    /**
     * get OneM2MWorker
     *
     * @return OneM2MWorker object
     */
    public static OneM2MWorker getInstance() {
        if (oneM2MWorker == null) {
            oneM2MWorker = new OneM2MWorker();
        }
        return oneM2MWorker;
    }

    public void setStateListener(StateListener stateListener) {
        this.stateListener = stateListener;
    }

    /**
     * set datas
     *
     * @param accountID
     * @param accountPassword
     * @param uKey
     */
    public void setDatas(String accountID, String accountPassword, String uKey) {
        this.accountID = accountID;
        this.accountPassword = accountPassword;
        this.uKey = uKey;
    }

//    /**
//     * set client ID
//     */
//    private void setClientID() {
//        macAddress = Utils.getMacAddress(context);
//        if (TextUtils.isEmpty(accountID) == true || TextUtils.isEmpty(macAddress) == true) {
//            return;
//        }
//        mClientID = accountID + "_" + macAddress.substring(macAddress.length() - 4, macAddress.length());
//        int clientIDLength = mClientID.length();
//        if (clientIDLength > MAX_CLIENT_ID_LENGTH) {
//            mClientID = mClientID.substring(0, clientIDLength - (clientIDLength - MAX_CLIENT_ID_LENGTH));
//        }
//    }

    /**
     * connect
     *
     * @param context
     * @param registerDevice true : register device <-> false
     */
    public void connect(Context context, final boolean registerDevice) {
        this.context = context;
//        setClientID();
//        String appEUI = UserInfo.getInstance(context).loadAppEUI();
//        String host = UserInfo.getInstance(context).loadServerURL();
//        if (TextUtils.isEmpty(mClientID) || TextUtils.isEmpty(appEUI) || TextUtils.isEmpty(host)) {
//            Log.e(TAG, "Invalid info!");
//            return;
//        }
        String host = Const.MQTT_HOST;
        boolean useTLS = UserInfo.getInstance(context).loadUseTLS();
        if (useTLS == true) {
            host = "ssl://" + host;
        } else {
            host = "tcp://" + host;
        }
        Log.e(TAG, "host : " + host);

        MQTTClient.Builder builder = new MQTTClient.Builder(context)
                .baseUrl(host)
                .userName(Const.ACCOUNT_USER_ID)
                .password(Const.ACCOUNT_CREDENTIAL_ID)
                .setLog(true);

        MQTTClient = builder.build();
//        String to = "/" + appEUI + Const.ONEM2M_TO;
//        config = new MQTTConfiguration(Const.ONEM2M_SERVICE_ID,
//                Const.ONEM2M_AE_NAME,           // selected device id
//                Const.ACCOUNT_USER_ID,
//                Const.ACCOUNT_CREDENTIAL_ID);

        mBinder = new Binder();
        mqttService = MQTTClient.connect(IMQTT.class, config, mBinder, new MQTTProcessor.MQTTListener() {
                    @Override
                    public void onPush(execInstanceControl control) {
                        Log.e(TAG, "push control!");
                    }

                    @Override
                    public void onPush(notification sensorInfo) {
                        Log.e(TAG, "push sensorInfo!");
                        if(stateListener != null) {
                            stateListener.onSensorInfoReceived(sensorInfo);
                        }
                    }

                    @Override
                    public void onDisconnected() {
                        Log.e(TAG, "disconnect success!");
                    }

                    @Override
                    public void onDisconnectFailure() {
                        Log.e(TAG, "disconnect failure!");
                    }

                    @Override
                    public void onSubscribed() {
                        Log.e(TAG, "subscribe success!");
                        if (stateListener != null) {
                            stateListener.onConnected(true, accountID, accountPassword, uKey);
                        }
                    }

                    @Override
                    public void onSubscribeFailure() {
                        Log.e(TAG, "subscribe failure!");
                        MQTTClient.disconnect();
                        if (stateListener != null) {
                            stateListener.onConnected(false, null, null, null);
                        }
                    }

                    @Override
                    public void onConnected() {
                        Log.e(TAG, "connect success!");
                    }

                    @Override
                    public void onConnectFailure() {
                        Log.e(TAG, "connect fail!");
                        if (stateListener != null) {
                            stateListener.onConnected(false, null, null, null);
                        }
                    }

                    @Override
                    public void onConnectionLost() {
                        Log.e(TAG, "connection lost!");
                    }

                    @Override
                    public void onDelivered() {
                        //reportMessageDelivered = true;
                    }
                }
        );
    }

    /**
     * disconnect
     */
    public void disconnect() {
        if (MQTTClient != null) {
            MQTTClient.disconnect();
        }
    }

    /**
     * set AE-name
     *
     * @param aeName
     */
    public void setAEName(String aeName) {
        config = new MQTTConfiguration(Const.ONEM2M_SERVICE_ID,
                aeName,           // selected device id
                Const.ACCOUNT_USER_ID,
                Const.ACCOUNT_CREDENTIAL_ID,
                Const.ACCOUNT_USER_ID + "_Service");
//        config.setAEName(aeName);
    }

//    /**
//     * get sensor info
//     *
//     * @param callback
//     */
//    public void lookUp(MQTTCallback callback) {
//        if (mqttService == null || MQTTClient == null || MQTTClient.isMQTTConnected() == false) {
//            return;
//        }
//        String uKey = UserInfo.getInstance(context).loadUKey();
//        if (TextUtils.isEmpty(uKey) == true) {
//            return;
//        }
//
//        try {
//            contentInstance contentInstanceRetrieve = new contentInstance.Builder(Definitions.Operation.Retrieve).
//                    containerName(Const.CONTAINER_TTV_NAME).
//                    uKey(uKey).
//                    nm("latest").build();
//
//            mqttService.publish(contentInstanceRetrieve, callback);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * get photo URL info
     *
     * @param callback
     */
    public void getPhotoURL(MQTTCallback callback) {
        if (mqttService == null || MQTTClient == null || MQTTClient.isMQTTConnected() == false) {
            return;
        }
//        String uKey = UserInfo.getInstance(context).loadUKey();
//        if (TextUtils.isEmpty(uKey) == true) {
//            return;
//        }

        try {
            contentInstance contentInstanceRetrieve = new contentInstance.Builder(Definitions.Operation.Retrieve).
                    fr(Const.ONEM2M_AE_ID).
                    containerName(Const.CONTAINER_PHOTOURL_NAME).
                    suffix("la").build();

            mqttService.publish(contentInstanceRetrieve, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * send control
     *
     * @param exra     execReqArgs
     * @param callback callback
     */
    public void sendControl(String exra, MQTTCallback callback) {
        if (mqttService == null || MQTTClient == null || MQTTClient.isMQTTConnected() == false) {
            return;
        }
//        String uKey = UserInfo.getInstance(context).loadUKey();
        String deviceID = UserInfo.getInstance(context).loadDeviceID();
//        if (TextUtils.isEmpty(uKey) == true || TextUtils.isEmpty(deviceID) == true) {
//            return;
//        }
        //String mgmtCmdName = Const.ONEM2M_DEVICE_ID + "_" + Const.MGMTCMD_NAME;
        String mgmtCmdName = deviceID; // ThingPlug 1.5 Service Platform default

        try {
            mgmtCmd mgmtCmdUpdate = new mgmtCmd.Builder(Definitions.Operation.Update).
                    fr(Const.ONEM2M_AE_ID).
                    rn(mgmtCmdName).
                    cmt("1").
                    exe("true").
                    exra(exra).build();

            mqttService.publish(mgmtCmdUpdate, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * control result look up
     *
     * @param nm mgmtCmd name
     * @param rn execInstance resource name
     * @param callback
     */
    public void controlResultLookUp(String nm, String rn, MQTTCallback callback) {
        if (mqttService == null || MQTTClient == null || MQTTClient.isMQTTConnected() == false) {
            return;
        }
//        String uKey = UserInfo.getInstance(context).loadUKey();
//        if (TextUtils.isEmpty(uKey) == true) {
//            return;
//        }

        try {
            execInstance execInstanceRetrieve = new execInstance.Builder(Definitions.Operation.Retrieve).
                    fr(Const.ONEM2M_AE_ID).
                    nm(nm).
                    rn(rn).build();
            mqttService.publish(execInstanceRetrieve, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * set subscription
     *
     */
    public void setSubscription() {
        // create subscription
        oneM2MAPI_V1_14.getInstance().tpRegisterContainerSubscription(mqttService, Const.ONEM2M_AE_ID,
                Const.CONTAINER_TTV_NAME, Const.SUBSCRIPTION_NAME, "3", "1", new MQTTCallback<subscriptionResponse>() {
                    @Override
                    public void onResponse(subscriptionResponse response) {
                        Log.e(TAG, "subscription created!");
                    }

                    @Override
                    public void onFailure(int errorCode, String message) {

                    }
                });
            }

    /**
     * remove subscription
     *
     * @param callback
     */
    public void removeSubscription(MQTTCallback callback) {
        try {
            subscription subscriptionDelete = new subscription.Builder(Definitions.Operation.Delete)
                    .fr(Const.ONEM2M_AE_ID)
                    .containerName(Const.CONTAINER_TTV_NAME)
                    .rn(Const.SUBSCRIPTION_NAME).build();
            mqttService.publish(subscriptionDelete, callback);
        } catch (Exception e) {
            e.printStackTrace();
            callback.onFailure(0, "Application error!");
        }
    }
}
