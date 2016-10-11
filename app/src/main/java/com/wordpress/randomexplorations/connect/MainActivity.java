package com.wordpress.randomexplorations.connect;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.TextView;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    List<iotDevice> connected_devices;
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        connected_devices = new ArrayList<iotDevice>();

        ImageButton bt = (ImageButton)findViewById(R.id.device1);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Assuming only one device for now
                if (connected_devices.size() > 0) {
                    iotDevice dev = connected_devices.get(0);
                    int new_status = iotDevice.IOT_DEVICE_STATUS_ON;

                    if (dev.status == iotDevice.IOT_DEVICE_STATUS_ON) {
                        new_status = iotDevice.IOT_DEVICE_STATUS_OFF;
                    }

                    DeviceUpdater upd = new DeviceUpdater(new_status);
                    upd.execute(dev);
                }
            }
        });

        registerForContextMenu(bt);

        // Screen refresh
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(true);

                ImageButton bt = (ImageButton)findViewById(R.id.device1);
                bt.setImageResource(R.drawable.unknown);
                connected_devices.clear();

                new deviceScanner().execute(smartomeSocket.generate_dummy_device());
            }
        });

        new deviceScanner().execute(smartomeSocket.generate_dummy_device());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void refresh_device() {
        ImageButton bt = (ImageButton)findViewById(R.id.device1);
        bt.setImageResource(R.drawable.unknown);

        if (connected_devices.size() > 0) {
            DeviceUpdater upd = new DeviceUpdater(iotDevice.IOT_DEVICE_STATUS_UNKNOWN);
            upd.execute(connected_devices.get(0));  // Assume only one device for now
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.cmenu_refresh:
                refresh_device();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.device_context_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private InetAddress getBroadcastAddress() {
        try {
            WifiManager myWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            DhcpInfo myDhcpInfo = myWifiManager.getDhcpInfo();
            if (myDhcpInfo == null) {
                System.out.println("Could not get broadcast address");
                return null;
            }
            int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask)
                    | ~myDhcpInfo.netmask;
            byte[] quads = new byte[4];
            for (int k = 0; k < 4; k++)
                quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
            return InetAddress.getByAddress(quads);
        } catch (Exception e) {
            Log.d("this", "Exception getting broadcast: " + e.getMessage());
            return null;
        }
    }

    private class DeviceUpdater extends AsyncTask<iotDevice, Void, iotDevice> {
        public int new_status = iotDevice.IOT_DEVICE_STATUS_UNKNOWN;

        public DeviceUpdater(int status) {
            new_status = status;
        }

        protected iotDevice doInBackground(iotDevice... devs) {
            iotDevice dev = devs[0];
            dev.change_state(new_status);
            return dev;
        }

        protected void onPostExecute(iotDevice dev) {

            /*
             * Right now we assume only one device
             */
            ImageButton bt = (ImageButton)findViewById(R.id.device1);
            if (dev.status == iotDevice.IOT_DEVICE_STATUS_OFF) {
                bt.setImageResource(R.drawable.off);
            } else if (dev.status == iotDevice.IOT_DEVICE_STATUS_ON) {
                bt.setImageResource(R.drawable.on);
            } else {
                bt.setImageResource(R.drawable.unknown);
            }

        }
    }

    private class deviceScanner extends AsyncTask<iotDevice, iotDevice, Boolean> {

        protected Boolean doInBackground(iotDevice... dummy_devices) {

            discoveryProtocol prot = new discoveryProtocol();
            Boolean result = false;
            prot.myBroadcastAddress = getBroadcastAddress();
            Log.d("this", "Starting IoT discovery with my broadcast: " + prot.myBroadcastAddress.getHostAddress());
            iotDevice dummy_device = dummy_devices[0];
            WifiManager.MulticastLock lock = null;

            if (dummy_device.requires_multicast_socket()) {
                WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
                lock = wifi.createMulticastLock("my_wifi_lock");
                lock.acquire();
            }

            Boolean started = dummy_device.initiate_discovery(prot);
            if (started) {
                iotDevice s;
                do {
                    s = dummy_device.scan_devices(prot);

                    if (s != null) {
                        result = true;
                        s.get_status();
                        publishProgress(s);
                    }

                } while (s != null);

                dummy_device.cleanup_discovery(prot);
            }

            if (dummy_device.requires_multicast_socket()) {
                lock.release();
            }

            return true;
        }

        protected void onProgressUpdate(iotDevice... devs) {
            iotDevice dev = devs[0];
            int i = 0;
            boolean found = false;

            for (i = 0; i < connected_devices.size(); i++) {
                if (connected_devices.get(i).matches(dev)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                connected_devices.add(dev);
            }

            /*
             * Right now we assume only one device
             */
            ImageButton bt = (ImageButton)findViewById(R.id.device1);
            if (dev.status == iotDevice.IOT_DEVICE_STATUS_OFF) {
                bt.setImageResource(R.drawable.off);
            } else if (dev.status == iotDevice.IOT_DEVICE_STATUS_ON) {
                bt.setImageResource(R.drawable.on);
            } else {
                bt.setImageResource(R.drawable.unknown);
            }

        }

        protected void onPostExecute(Boolean result) {
            mSwipeRefreshLayout.setRefreshing(false);
        }

    }

}
