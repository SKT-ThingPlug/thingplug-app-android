package com.skt.thingplug_v1_5_service.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.skt.thingplug_v1_5_service.Const;
import com.skt.thingplug_v1_5_service.R;
import com.skt.thingplug_v1_5_service.data.UserInfo;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.push.execInstanceControl;
import tp.skt.onem2m_v1_14.binder.mqtt_v1_14.push.notification;

/**
 * activity for device list
 *
 * Copyright (C) 2017. SK Telecom, All Rights Reserved.
 * Written 2017, by SK Telecom
 */
public class DeviceListActivity extends AppCompatActivity {

    private final static String     TAG = DeviceListActivity.class.getSimpleName();
    private UserInfo                userInfo;

    private final boolean USE_LOCAL_INFO = true;
    private OneM2MWorker oneM2MWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        oneM2MWorker = OneM2MWorker.getInstance();
        oneM2MWorker.setStateListener(new OneM2MWorkerListener());

        ActionBar bar = getSupportActionBar();
        bar.setTitle(R.string.actionbar_deviceList);

        userInfo = UserInfo.getInstance(this);

        try {
            List<DeviceInfo> deviceList = new DeviceListTask().execute().get();
            // create device list
            createDeviceList(deviceList);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private class DeviceListTask extends AsyncTask<Void, Void, List<DeviceInfo>> {

        @Override
        protected List<DeviceInfo> doInBackground(Void... voids) {
            // search device list
            ArrayList<DeviceInfo> deviceList = searchDevice(userInfo.loadUKey());
            return deviceList;
        }
    }

    private ArrayList<DeviceInfo> searchDevice(String ukey) {
        ArrayList<DeviceInfo> deviceList = new ArrayList<DeviceInfo>();

        if(USE_LOCAL_INFO) {
            DeviceInfo info = new DeviceInfo(Const.ONEM2M_DEVICE_ID, Const.ONEM2M_SERVICE_ID);
            deviceList.add(info);
        } else {
            try {
                URL url = new URL(userInfo.loadSearchURL() + Const.URL_SEARCH_DEVICE);
                HttpURLConnection request = (HttpURLConnection) url.openConnection();
                request.setRequestMethod("GET");
                request.setRequestProperty("uKey", ukey);
                request.setRequestProperty("locale", "ko");

                int responseCode = request.getResponseCode();
                Log.i(TAG, "[" + url.toString() + "]" + "responseCode : " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = request.getInputStream();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] byteBuffer = new byte[1024];
                    byte[] byteData = null;
                    int nLength = 0;
                    while ((nLength = is.read(byteBuffer, 0, byteBuffer.length)) != -1) {
                        baos.write(byteBuffer, 0, nLength);
                    }
                    byteData = baos.toByteArray();
                    String response = new String(byteData);

                    try {
                        XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                        XmlPullParser xmlParser = xmlFactoryObject.newPullParser();
                        xmlParser.setInput(new StringReader(response));
                        String tagName = null;
                        String device_id = "";
                        String device_name = "";
                        while (xmlParser.getEventType() != XmlPullParser.END_DOCUMENT) {
                            if (xmlParser.getEventType() == XmlPullParser.START_TAG) {
                                if (xmlParser.getName().equalsIgnoreCase("device_Id") == true
                                        || xmlParser.getName().equalsIgnoreCase("device_Name") == true) {
                                    tagName = xmlParser.getName();
                                }
                            } else if (xmlParser.getEventType() == XmlPullParser.TEXT) {
                                if (tagName != null) {
                                    if (tagName.equalsIgnoreCase("device_Id") == true) {
                                        device_id = xmlParser.getText();
                                    } else if (tagName.equalsIgnoreCase("device_Name") == true) {
                                        device_name = xmlParser.getText();
                                    }
                                    if (!device_id.isEmpty() && !device_name.isEmpty()) {
                                        DeviceInfo deviceInfo = new DeviceInfo(device_id);
                                        //deviceInfo.setDeviceName(device_name);
                                        deviceList.add(deviceInfo);
                                        device_id = "";
                                        device_name = "";
                                    }
                                    tagName = "";
                                }
                            }
                            xmlParser.next();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    request.disconnect();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return deviceList;
    }

    private void createDeviceList(List<DeviceInfo> deviceList) {
        ListView deviceListView = (ListView) findViewById(R.id.listView);
        deviceListView.setSmoothScrollbarEnabled(true);
        DeviceListAdapter adapter = new DeviceListAdapter(this, deviceList);

        int lastViewedPosition = deviceListView.getFirstVisiblePosition();
        View v = deviceListView.getChildAt(0);
        int topOffset = (v == null) ? 0 : v.getTop();

        deviceListView.setAdapter(adapter);
        deviceListView.setSelectionFromTop(lastViewedPosition, topOffset);

        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DeviceInfo deviceListModel = (DeviceInfo) parent.getAdapter().getItem(position);
                if (deviceListModel != null) {
                    moveToNext(deviceListModel.getDeviceId());
                }
            }
        });
    }

    private void moveToNext(String deviceId) {
        showProgress(true);
        userInfo.saveDeviceID(deviceId);
        oneM2MWorker.setAEName(deviceId);
        oneM2MWorker.connect(this, false);
//        Intent intent = new Intent(this, SensorListActivity.class);
//        startActivity(intent);
//        finish();
    }

    public class DeviceInfo {
        private String      deviceId;
        private String      serviceId;

        public DeviceInfo(String deviceId) {
            this.deviceId = deviceId;
        }

        public DeviceInfo(String deviceId, String serviceId) {
            this.deviceId = deviceId;
            this.serviceId = serviceId;
        }

        public String getDeviceId(){
            return deviceId;
        }

        public String getServiceId(){
            return serviceId;
        }

        public void setServiceId(String serviceId){
            this.serviceId = serviceId;
        }
    }

    public class DeviceListAdapter extends ArrayAdapter<DeviceInfo> {
        private Context             context;
        private List<DeviceInfo>    deviceInfoList;

        public DeviceListAdapter(Context context, List<DeviceInfo> deviceInfoList) {
            super(context, R.layout.device_list_item, deviceInfoList);

            this.context = context;
            this.deviceInfoList = deviceInfoList;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            DeviceInfo deviceInfo = deviceInfoList.get(position);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.device_list_item, parent, false);
            TextView itemNameView = (TextView)rowView.findViewById(R.id.item_device);
            itemNameView.setText(deviceInfo.getServiceId() + "(" + deviceInfo.getDeviceId() + ")");
            return rowView;
        }
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View listView = findViewById(R.id.listView);
                final View progressView = findViewById(R.id.go);

                // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
                // for very easy animations. If available, use these APIs to fade-in
                // the progress spinner.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                    int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

                    listView.setVisibility(show ? View.GONE : View.VISIBLE);
                    listView.animate().setDuration(shortAnimTime).alpha(
                            show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            listView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });

                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                    progressView.animate().setDuration(shortAnimTime).alpha(
                            show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });
                } else {
                    // The ViewPropertyAnimator APIs are not available, so simply show
                    // and hide the relevant UI components.
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                    listView.setVisibility(show ? View.GONE : View.VISIBLE);
                }

                if (!show) {
                    Toast.makeText(DeviceListActivity.this, getResources().getString(R.string.login_fail), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * oneM2M worker state listener
     */
    private class OneM2MWorkerListener implements OneM2MWorker.StateListener {

        @Override
        public void onConnected(boolean result, String accountID, String accountPassword, String uKey) {
            if (result) {
                Intent intent = new Intent(DeviceListActivity.this, SensorListActivity.class);
                startActivity(intent);
                finish();
            } else {
                showProgress(false);
            }
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
        }
    }
}
