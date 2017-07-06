package com.skt.thingplug_v1_5_service.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.skt.thingplug_v1_5_service.Const;
import com.skt.thingplug_v1_5_service.R;
import com.skt.thingplug_v1_5_service.data.GoogleDriveHandler;
import com.skt.thingplug_v1_5_service.data.SensorInfo;
import com.skt.thingplug_v1_5_service.data.SensorType;
import com.skt.thingplug_v1_5_service.data.TTVBuilder;
import com.skt.thingplug_v1_5_service.data.TLVBuilder;
import com.skt.thingplug_v1_5_service.data.UserInfo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.Definitions;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.push.execInstanceControl;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.push.notification;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.response.contentInstanceResponse;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.response.execInstanceResponse;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.response.mgmtCmdResponse;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.response.subscriptionResponse;
import tp.skt.onem2m_v1_14.net.mqtt.MQTTCallback;

/**
 * activity for sensor list
 *
 * Copyright (C) 2017. SK Telecom, All Rights Reserved.
 * Written 2017, by SK Telecom
 */
public class SensorListActivity extends AppCompatActivity {

    private final static String     TAG = SensorListActivity.class.getSimpleName();

    public static final String      EXTRA_SENSOR_TYPE = "sensorType";

    private final int               SETTING_RESULT = 100;
    public static final int         SETTING_RESULT_SAVE = 1;
    public static final int         SETTING_RESULT_LOGOUT = 2;

    private static List<SensorInfo> sensorInfos = new ArrayList<>();

    private UserInfo                userInfo;
    private OneM2MWorker            oneM2MWorker;
    private GoogleDriveHandler      googleDriveHandler;
//    private Timer                   timer;

    private ListItemClickListener       listItemClickListener = new ListItemClickListener();
    private ActivateClickListener       activateClickListener = new ActivateClickListener();
    private ActuatorRunClickListener    actuatorRunClickListener = new ActuatorRunClickListener();

    private ListView listView;
    private View contentView;
    private TextView content;

//    private SensorInfo deviceInfo;
    private int oldTransferInterval = 0;
    private boolean showContent = false;

    /**
     * get sensor info list
     * @return sensor info list
     */
    public static List<SensorInfo> getSensorInfos() {
        return sensorInfos;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_list);

        listView = (ListView) findViewById(R.id.listView);
//        View footer = getLayoutInflater().inflate(R.layout.listview_footer, null, false) ;

        listView.setSmoothScrollbarEnabled(true);
        // header view
        contentView = getLayoutInflater().inflate(R.layout.listview_header, null, false);
        listView.addHeaderView(contentView);
        content = (TextView) findViewById(R.id.content);

        // listView에 header, footer 추가.
//        listView.addHeaderView(contentView) ;

        userInfo = UserInfo.getInstance(this);

        ActionBar bar = getSupportActionBar();
        bar.setTitle(getString(R.string.actionbar_list) + " (" + userInfo.loadDeviceID() + ")");

        oneM2MWorker = OneM2MWorker.getInstance();
        oneM2MWorker.setStateListener(new OneM2MWorkerListener());

        googleDriveHandler = new GoogleDriveHandler(this, new GoogleDriveCommandListener());
        //googleDriveHandler.connect();

//        deviceInfo = new SensorInfo(SensorType.DEVICE);
        oldTransferInterval = userInfo.loadTransferInterval();

        // create sensor list
        createSensorList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sensor_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_setting:
                Intent intent = new Intent(this, SettingActivity.class);
                startActivityForResult(intent, SETTING_RESULT);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (googleDriveHandler.onActivityResult(requestCode, resultCode, data)) {
            return;
        }

        if (requestCode == SETTING_RESULT) {
            switch (resultCode) {
                case SETTING_RESULT_SAVE:
                    int newTransferInterval = userInfo.loadTransferInterval();
                    if (oldTransferInterval != newTransferInterval) {
                        SensorInfo deviceInfo = new SensorInfo(SensorType.DEVICE);
                        controlDevice(deviceInfo, newTransferInterval);
                    }
                    clearSensorList();
                    createSensorList();
                    break;
                case SETTING_RESULT_LOGOUT:
                    userInfo.clear(false);

                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy!");
        oneM2MWorker.disconnect();
        //googleDriveHandler.disconnect();
        // clear sensor list
        clearSensorList();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed!");
        super.onBackPressed();
        oneM2MWorker.removeSubscription(new MQTTCallback<subscriptionResponse>() {
            @Override
            public void onResponse(subscriptionResponse response) {
                Log.d(TAG, "success!");
                SensorListActivity.this.finish();
            }

            @Override
            public void onFailure(int errorCode, String message) {
                Log.d(TAG, "failure!");
                SensorListActivity.this.finish();
            }
        });
    }

    /**
     * create sensor list
     */
    private void addSensorInfo(SensorType sensorType) {
        SensorInfo sensorInfo = new SensorInfo(sensorType);
        sensorInfo.setActivated(true);
        sensorInfos.add(sensorInfo);
    }

    private void createSensorList() {
        // add sensor item
        addSensorInfo(SensorType.AMBIENT_TEMPERATURE);
        addSensorInfo(SensorType.RELATIVE_HUMIDITY);
        addSensorInfo(SensorType.LIGHT);
        addSensorInfo(SensorType.MEMORY);
        addSensorInfo(SensorType.BUZZER);
        addSensorInfo(SensorType.LED);

        /*
        for (SensorType sensorType : SensorType.values()) {
            if (sensorType != SensorType.NONE && sensorType != SensorType.DEVICE) {
                SensorInfo sensorInfo = new SensorInfo(sensorType);
//                sensorInfo.setActivated(false);
                sensorInfos.add(sensorInfo);
            }
        }
        */

        // add to listview
//        ListView listView = (ListView) findViewById(R.id.listView);
//        listView.setSmoothScrollbarEnabled(true);
        listView.setAdapter(new SensorListAdapter(this, sensorInfos));

//        timer = new Timer();
//
//        // create timer for view
//        final Handler handler = new Handler();
//        TimerTask viewTask = new TimerTask() {
//            public void run() {
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        ListView listView = (ListView) findViewById(R.id.listView);
//                        listView.invalidateViews();
//                    }
//                });
//            }
//        };
//        timer.schedule(viewTask, 0, userInfo.loadListInterval());
//
//        // create timer for report
//        TimerTask reportTask = new TimerTask() {
//            public void run() {
//                // rececive sensor data from oneM2M server
//                oneM2MWorker.lookUp(new MQTTCallback<contentInstanceResponse>() {
//                    @Override
//                    public void onResponse(contentInstanceResponse response) {
//                        String tlvArray = response.getCon();
//
//                        // apply to local sensor list
//                        if (TextUtils.isEmpty(tlvArray) == false) {
//                            List<SensorInfo> sensorArray = null;
//                            if (userInfo.loadUseTLV()) {
//                                sensorArray = TLVBuilder.parseSensorData(tlvArray);
//                            }
//                            else {
//                                sensorArray = TTVBuilder.parseSensorData(tlvArray);
//                            }
//                            for (SensorInfo localInfo : sensorInfos) {
//                                localInfo.setActivated(false);
//                                if (!localInfo.isControlling()) {
//                                    localInfo.setSuspend(false);
//                                }
//
//                                for (SensorInfo remoteInfo : sensorArray) {
//                                    if (localInfo.getType() == remoteInfo.getType()) {
//                                        localInfo.setActivated(true);
//                                        localInfo.setValues(remoteInfo.getValues());
//                                    }
//                                }
//                            }
//                        }
//                    }
//
//                    @Override
//                    public void onFailure(int errorCode, String message) {
//                        Log.i(TAG, errorCode + " : " + message);
//                    }
//                });
//            }
//        };
//        timer.schedule(reportTask, 0, userInfo.loadTransferInterval());
        oneM2MWorker.setSubscription();

//        if(userInfo.loadShowContent() == true) {
//            if(listView.getHeaderViewsCount() == 0) {
//                contentView = getLayoutInflater().inflate(R.layout.listview_header, null, false);
//                listView.addHeaderView(contentView);
//                content = (TextView) findViewById(R.id.content);
//            }
//            contentView.setVisibility(View.VISIBLE);
//        } else {
//            listView.removeHeaderView(contentView);
//            contentView.setVisibility(View.GONE);
//        }

        if(userInfo.loadShowContent() == true) {
            contentView.setVisibility(View.VISIBLE);
        } else {
            content.setText("content");
            contentView.setVisibility(View.GONE);
        }
    }

    /**
     * clear sensor list
     */
    private void clearSensorList() {
//        timer.cancel();

        // clear view
        sensorInfos.clear();
//        ListView listView = (ListView) findViewById(R.id.listView);
        listView.deferNotifyDataSetChanged();
    }

    /**
     * list adapter
     */
    private class SensorListAdapter extends ArrayAdapter<SensorInfo> {
        private final Context context;
        private final List<SensorInfo> sensorInfos;

        /**
         * constructor
         * @param context        context
         * @param sensorInfos    sensor info list
         */
        public SensorListAdapter(Context context, List<SensorInfo> sensorInfos) {
            super(context, R.layout.sensor_list_item, sensorInfos);

            this.context = context;
            this.sensorInfos = sensorInfos;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.sensor_list_item, parent, false);
            LinearLayout itemLayout = (LinearLayout) rowView.findViewById(R.id.item_layout);
            ImageView itemImageView = (ImageView) rowView.findViewById(R.id.item_image);
            TextView itemNameView = (TextView) rowView.findViewById(R.id.item_name);
            TextView itemStatus = (TextView) rowView.findViewById(R.id.item_status);
            ToggleButton itemEnable = (ToggleButton) rowView.findViewById(R.id.item_enable);
            ToggleButton itemActivate = (ToggleButton) rowView.findViewById(R.id.item_activate);
            Button itemActuatorRun = (Button) rowView.findViewById(R.id.item_actuator_run);
            itemEnable.setVisibility(View.GONE);

            SensorInfo sensorInfo = sensorInfos.get(position);
            itemImageView.setImageResource(sensorInfo.getType().getImage());
            itemNameView.setText(sensorInfo.getType().getNickname());
//            itemEnable.setChecked(sensorInfo.isEnable());
            if (sensorInfo.getType().getCategory() == SensorType.Category.ACTUATOR) {
                itemStatus.setVisibility(View.GONE);
                itemActivate.setVisibility(View.GONE);
                itemActuatorRun.setEnabled(!sensorInfo.isControlling()); // && !sensorInfo.isSuspend());
            }
            else {
                itemActuatorRun.setVisibility(View.GONE);
                itemStatus.setText(sensorInfo.toString());
                itemActivate.setChecked(sensorInfo.isActivated());
                itemActivate.setEnabled(!sensorInfo.isControlling()); // !sensorInfo.isSuspend());
            }

            // sensor item click
            itemLayout.setTag(sensorInfo);
            itemLayout.setOnClickListener(listItemClickListener);

            // sensor activation button click
            itemActivate.setTag(sensorInfo);
            itemActivate.setOnClickListener(activateClickListener);

            // actuator run button click
            itemActuatorRun.setTag(sensorInfo);
            itemActuatorRun.setOnClickListener(actuatorRunClickListener);

            return rowView;
        }
    }

    /**
     * list item click listener
     */
    private class ListItemClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            final SensorInfo sensorInfo = (SensorInfo) view.getTag();

            if (sensorInfo.getType().getCategory() == SensorType.Category.ACTUATOR) {
                switch (sensorInfo.getType()) {
                    case CAMERA:
                        showImageViewDialog();
                        break;
                }
            }
            else if (sensorInfo.isActivated()) {
                Intent intent = new Intent(SensorListActivity.this, SensorDetailActivity.class);
                intent.putExtra(EXTRA_SENSOR_TYPE, sensorInfo.getType());
                startActivity(intent);
            }
        }
    }

    /**
     * sensor activate button click listener
     */
    private class ActivateClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            SensorInfo sensorInfo = (SensorInfo) view.getTag();
            ToggleButton button = (ToggleButton) view;

            // send sensor activation command to device App.
            controlDevice(sensorInfo, button.isChecked() ? 1 : 0, button);
        }
    }

    /**
     * actuator run button click listener
     */
    private class ActuatorRunClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            SensorInfo sensorInfo = (SensorInfo) view.getTag();
            Button button = (Button) view;

//            if (!sensorInfo.isActivated()) {
//                Toast.makeText(SensorListActivity.this, R.string.actuator_disabled, Toast.LENGTH_SHORT).show();
//                return;
//            }

            switch (sensorInfo.getType()) {
                case BUZZER:    showBuzzerControlDialog(sensorInfo, button);    break;
                case LED:       showLedControlDialog(sensorInfo, button);       break;
                case CAMERA:    showCameraControlDialog(sensorInfo, button);    break;
            }
        }
    }

    /**
     * show buzzer control dialog
     */
    private void showBuzzerControlDialog(final SensorInfo sensorInfo, final Button button) {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.actuator_dialog_title);
        dialog.setSingleChoiceItems(R.array.buzzer_items, 0, null)
                .setPositiveButton(R.string.actuator_dialog_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ListView lv = ((AlertDialog) dialog).getListView();
                        int selectedItem = 0;
                        switch (lv.getCheckedItemPosition()) {
                            case 0:     selectedItem = 0;       break;
                            case 1:     selectedItem = 1;       break;
                            case 2:     selectedItem = 2;       break;
                            case 3:     selectedItem = 4;       break;
                        }

                        // send buzzer control command to device App
                        controlDevice(sensorInfo, selectedItem, button);
                    }
                }).setNegativeButton(R.string.actuator_dialog_cancel, null);
        dialog.show();
    }

    /**
     * show LED control dialog
     */
    private void showLedControlDialog(final SensorInfo sensorInfo, final Button button) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.actuator_dialog_title);
        dialog.setSingleChoiceItems(R.array.led_items, 0, null)
                .setPositiveButton(R.string.actuator_dialog_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ListView lv = ((AlertDialog) dialog).getListView();
                        int selectedItem = lv.getCheckedItemPosition();

                        // send LED control command to device App.
                        controlDevice(sensorInfo, selectedItem, button);
                    }
                }).setNegativeButton(R.string.actuator_dialog_cancel, null);
        dialog.show();
    }

    /**
     * show camera control dialog
     */
    private void showCameraControlDialog(final SensorInfo sensorInfo, final Button button) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.actuator_dialog_title);
        dialog.setSingleChoiceItems(R.array.camera_items, 0, null)
                .setPositiveButton(R.string.actuator_dialog_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ListView lv = ((AlertDialog) dialog).getListView();
                        int selectedItem = lv.getCheckedItemPosition();

                        // send camera control command to device App.
                        controlDevice(sensorInfo, selectedItem, button);
                    }
                }).setNegativeButton(R.string.actuator_dialog_cancel, null);
        dialog.show();
    }

    /**
     * send sensor control command
     * @param sensorInfo    sensor info
     * @param command       data for sending
     * @param button        control button
     */
    private void controlDevice(final SensorInfo sensorInfo, int command, final Button button) {
//        button.setEnabled(false);
        sensorInfo.setActivated(!sensorInfo.isActivated());
//        sensorInfo.setSuspend(true);
        sensorInfo.setControlling(true);

        // update listview
//        ListView listView = (ListView) findViewById(R.id.listView);
        listView.invalidateViews();

        // build control
        String exra = "";
        if (userInfo.loadUseTLV()) {
            exra = TLVBuilder.buildSensorControlCommand(sensorInfo, command);
        }
        else {
            exra = TTVBuilder.buildSensorControlCommand(sensorInfo, command);
        }

        // send control
        oneM2MWorker.sendControl(exra, new MQTTCallback<mgmtCmdResponse>() {
            @Override
            public void onResponse(final mgmtCmdResponse response) {
                int responseCode = 0;
                if(response.rsc != null) {
                    responseCode = Integer.valueOf(response.rsc);
                }
                if(responseCode == Definitions.ResponseStatusCode.CHANGED) {
                    // send success
                    final Handler handler = new Handler();
                    final Runnable statusChecker = new Runnable() {
                        private int runCount = 0;

                        @Override
                        public void run() {
                            Log.i(TAG, "control count : " + runCount);
                            if (!sensorInfo.isControlling()) {
                                return;
                            }
                            runCount++;

                            try {
                                oneM2MWorker.controlResultLookUp(response.getRn(),
                                        response.getExinRn(),
                                        new MQTTCallback<execInstanceResponse>() {
                                            @Override
                                            public void onResponse(execInstanceResponse response) {
                                                int responseCode = 0;
                                                if (response.rsc != null) {
                                                    responseCode = Integer.valueOf(response.rsc);
                                                }

                                                if (responseCode == Definitions.ResponseStatusCode.OK) {
                                                    Log.i(TAG, "control result : " + response.getExr());
                                                    String exs = response.getExs();
                                                    String exr = response.getExr();
                                                    if (exs != null &&  exs.equals(Const.EXECSTATUS_PENDING) == false && exr != null) {
                                                        sensorInfo.setControlling(false);
                                                        if (exr.equals(Const.EXECRESULT_SUCCESS)) {
                                                            if (sensorInfo.getType() == SensorType.CAMERA) {
                                                                oneM2MWorker.getPhotoURL(new MQTTCallback<contentInstanceResponse>() {
                                                                    @Override
                                                                    public void onResponse(contentInstanceResponse response) {
                                                                        Log.i(TAG, "getPhotoURL() onResponse:" + response.getCon());
                                                                        googleDriveHandler.loadPicture(response.getCon());
                                                                    }

                                                                    @Override
                                                                    public void onFailure(int errorCode, String message) {
                                                                        resultControlDevice(false, R.string.control_fail, sensorInfo, button);
                                                                    }
                                                                });
                                                            } else {
                                                                resultControlDevice(true, R.string.control_success, sensorInfo, button);
                                                            }
                                                        }
                                                        else if (exr.equals(Const.EXECRESULT_DENIED)) {
                                                            this.onFailure(responseCode, response.RSM);
                                                        }
                                                    }
                                                }
                                                else {
                                                    this.onFailure(responseCode, response.RSM);
                                                }
                                            }
                                            @Override
                                            public void onFailure(int errorCode, String message) {
                                                Log.i(TAG, errorCode + " : " + message);

                                                resultControlDevice(false, R.string.control_fail, sensorInfo, button);
                                            }
                                        });
                            } finally {
                                if(runCount < (sensorInfo.getType() == SensorType.CAMERA ?
                                        Const.CONTROL_CAMERA_LOOKUP_MAX_COUNT:
                                        Const.CONTROL_LOOKUP_MAX_COUNT)) {
                                    handler.postDelayed(this, (sensorInfo.getType() == SensorType.CAMERA ?
                                            Const.CONTROL_CAMERA_LOOKUP_INTERVAL:
                                            Const.CONTROL_LOOKUP_INTERVAL));
                                } else {
                                    handler.removeCallbacks(this);

                                    resultControlDevice(false, R.string.control_timeout, sensorInfo, button);
                                }
                            }
                        }
                    };
                    handler.postDelayed(statusChecker, (sensorInfo.getType() == SensorType.CAMERA ?
                            Const.CONTROL_CAMERA_LOOKUP_INTERVAL:
                            Const.CONTROL_LOOKUP_INTERVAL));
                } else {
                    this.onFailure(responseCode, response.RSM);
                }
            }

            @Override
            public void onFailure(int errorCode, String message) {
                Log.i(TAG, errorCode + " : " + message);

                resultControlDevice(false, R.string.control_fail, sensorInfo, button);
            }
        });
    }

    /**
     * send sensor control command
     * @param sensorInfo    sensor info
     * @param command       data for sending
     */
    private void controlDevice(final SensorInfo sensorInfo, final int command) {
        sensorInfo.setControlling(true);
        // build control
        String exra = "";
        if (userInfo.loadUseTLV()) {
            exra = TLVBuilder.buildDeviceControlCommand(sensorInfo, command);
        }
        else {
            exra = TTVBuilder.buildSensorControlCommand(sensorInfo, command);
        }

        // send control
        final OneM2MWorker oneM2MWorker = OneM2MWorker.getInstance();
        oneM2MWorker.sendControl(exra, new MQTTCallback<mgmtCmdResponse>() {
            @Override
            public void onResponse(final mgmtCmdResponse response) {
                int responseCode = 0;
                if(response.rsc != null) {
                    responseCode = Integer.valueOf(response.rsc);
                }
                if(responseCode == Definitions.ResponseStatusCode.CHANGED) {
                    // send success
                    final Handler handler = new Handler();
                    final Runnable statusChecker = new Runnable() {
                        private int runCount = 0;

                        @Override
                        public void run() {
                            Log.i(TAG, "control count : " + runCount);
                            if (sensorInfo.isControlling() == false) {
                                return;
                            }
                            runCount++;

                            try {
                                oneM2MWorker.controlResultLookUp(response.getRn(),
                                        response.getExinRn(),
                                        new MQTTCallback<execInstanceResponse>() {
                                            @Override
                                            public void onResponse(execInstanceResponse response) {

                                                int responseCode = 0;
                                                if (response.rsc != null) {
                                                    responseCode = Integer.valueOf(response.rsc);
                                                }

                                                if (responseCode == Definitions.ResponseStatusCode.OK) {
                                                    Log.i(TAG, "control result : " + response.getExr());
                                                    String exs = response.getExs();
                                                    String exr = response.getExr();
                                                    if (exs != null &&  exs.equals(Const.EXECSTATUS_PENDING) == false && exr != null) {
                                                        sensorInfo.setControlling(false);
                                                        if (exr.equals(Const.EXECRESULT_SUCCESS)) {
//                                                            userInfo.saveTransferInterval(command);
                                                            oldTransferInterval = command;
                                                            userInfo.saveTransferInterval(oldTransferInterval);
                                                            Toast.makeText(SensorListActivity.this, R.string.reporting_cycle_control_success, Toast.LENGTH_SHORT).show();
                                                        }
                                                        else if (exr.equals(Const.EXECRESULT_DENIED)) {
                                                            this.onFailure(responseCode, response.RSM);
                                                        }
                                                    }
                                                }
                                                else {
                                                    this.onFailure(responseCode, response.RSM);
                                                }
                                            }
                                            @Override
                                            public void onFailure(int errorCode, String message) {
                                                Log.i(TAG, errorCode + " : " + message);
                                                userInfo.saveTransferInterval(oldTransferInterval);
                                                Toast.makeText(SensorListActivity.this, R.string.reporting_cycle_control_fail, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } finally {
                                if(runCount < Const.CONTROL_LOOKUP_MAX_COUNT) {
                                    handler.postDelayed(this, Const.CONTROL_LOOKUP_INTERVAL);
                                } else {
                                    handler.removeCallbacks(this);
                                    Toast.makeText(SensorListActivity.this, R.string.reporting_cycle_control_fail, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    };
                    handler.postDelayed(statusChecker, Const.CONTROL_LOOKUP_INTERVAL);
                } else {
                    this.onFailure(responseCode, response.RSM);
                }
            }

            @Override
            public void onFailure(int errorCode, String message) {
                Log.i(TAG, errorCode + " : " + message);
                userInfo.saveTransferInterval(oldTransferInterval);
            }
        });
    }

    private void resultControlDevice(boolean isSuccess, int resString, SensorInfo sensorInfo, Button button) {
//        if(button != null) {
//            button.setEnabled(true);
//        } else {
//            sensorInfo.setSuspend(false);
//        }
        sensorInfo.setControlling(false);

        if (!isSuccess) {
            if (sensorInfo.getType().getCategory() != SensorType.Category.ACTUATOR) {
                sensorInfo.setActivated(!sensorInfo.isActivated());
            }
//            sensorInfo.setSuspend(false);
        }

        String toastMessage = String.format(getResources().getString(resString), sensorInfo.getType().getNickname());
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        if(listView != null) {
            listView.invalidateViews();
        }
    }

    /**
     * show dialog for Google drive image
     */
    private void showImageViewDialog() {
        int rotate = 0;
        String fileName = getFilesDir().getPath() + "/temp.jpg";
        try {
            ExifInterface exif = new ExifInterface(fileName);
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_NORMAL:      rotate = 0;     break;
                case ExifInterface.ORIENTATION_ROTATE_90:   rotate = 90;    break;
                case ExifInterface.ORIENTATION_ROTATE_180:  rotate = 180;   break;
                case ExifInterface.ORIENTATION_ROTATE_270:  rotate = 270;   break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // create bitmap from file
        Bitmap bitmap = BitmapFactory.decodeFile(fileName);
        if (bitmap == null) {
            return;
        }

        // rotate by orientation
        Matrix mat = new Matrix();
        mat.postRotate(rotate);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);

        // create dialog
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_image_view);

        ImageView imageView = (ImageView) dialog.findViewById(R.id.image_view);
        imageView.setImageBitmap(bitmap);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * Google drive command listener
     */
    private class GoogleDriveCommandListener implements GoogleDriveHandler.CommandListener {

        @Override
        public void onSavedPicture(boolean result, String resourceId) {
            Log.i(TAG, "onSavedPicture : " + result + ", resourceId:" + resourceId);
        }

        @Override
        public void onLoadedPicture(boolean result, byte[] imageData) {
            Log.i(TAG, "onLoadedPicture : " + result);

            // find camera sensor
            SensorInfo cameraSensor = null;
            for (SensorInfo sensorInfo : sensorInfos) {
                if (sensorInfo.getType() == SensorType.CAMERA) {
                    cameraSensor = sensorInfo;
                    break;
                }
            }

            if (result) {
                // save file
                String fileName = getFilesDir().getPath() + "/temp.jpg";
                try {
                    FileOutputStream fo = new FileOutputStream(fileName);
                    fo.write(imageData);
                    fo.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

//                cameraSensor.setSuspend(false);

                // show image dialog
                showImageViewDialog();
            }
            else {
                resultControlDevice(false, R.string.control_fail, cameraSensor, null);
            }
        }
    }

    /**
     * oneM2M worker state listener
     */
    private class OneM2MWorkerListener implements OneM2MWorker.StateListener {

        @Override
        public void onConnected(boolean result, String accountID, String accountPassword, String uKey) {
        }

        @Override
        public void onDisconnected(boolean result) {
        }

        @Override
        public void onRegistered(boolean result, String dKey, String nodeLink) {
        }

        @Override
        public void onUnregistered(boolean result) {
        }

        @Override
        public RESULT onReceiveCommand(execInstanceControl control) {
            return RESULT.SUSPEND;
        }

        @Override
        public void onSensorInfoReceived(notification sensorInfo) {
            Log.d(TAG, "onSensorInfoReceived");
//            String deviceID = UserInfo.getInstance(SensorListActivity.this).loadDeviceID();
//            if(deviceID == null) { // || sensorInfo.fr.equals(deviceID) == false) {
//                return;
//            }
            String tlvArray = sensorInfo.getCon();
            if(userInfo.loadShowContent() == true) {
                content.setText(tlvArray);
            }
            // apply to local sensor list
            if (TextUtils.isEmpty(tlvArray) == false) {
                List<SensorInfo> sensorArray = null;
                if (userInfo.loadUseTLV()) {
                    sensorArray = TLVBuilder.parseSensorData(tlvArray);
                }
                else {
                    sensorArray = TTVBuilder.parseSensorData(tlvArray);
                }
                boolean hasValue;
                for (SensorInfo localInfo : sensorInfos) {
                    hasValue = false;
                    if(localInfo.getType().getCategory() != SensorType.Category.ACTUATOR) {
                        for (SensorInfo remoteInfo : sensorArray) {
                            if (localInfo.getType() == remoteInfo.getType()) {
                                localInfo.setValues(remoteInfo.getValues());
                                hasValue = true;
                            }
                        }
                        if (!localInfo.isControlling()) {
                            localInfo.setActivated(hasValue);
                        }
                    }
//                    localInfo.setActivated(false);
//                    if (!localInfo.isControlling()) {
//                        localInfo.setSuspend(false);
//                    }
//                    for (SensorInfo remoteInfo : sensorArray) {
//                        if (localInfo.getType() == remoteInfo.getType()) {
//                            localInfo.setActivated(true);
//                            localInfo.setValues(remoteInfo.getValues());
//                        }
//                    }
                }
            }
            listView.invalidateViews();
        }
    }
}
