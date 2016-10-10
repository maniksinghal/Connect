package com.wordpress.randomexplorations.connect;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

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

        new deviceScanner().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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

    private class deviceScanner extends AsyncTask<Void, iotDevice, Boolean> {

        private iotDevice dev;

        protected Boolean doInBackground(Void... a) {

            discoveryProtocol prot = new discoveryProtocol();
            Boolean result = false;
            prot.myBroadcastAddress = getBroadcastAddress();
            Log.d("this", "Starting smartome discovery with my broadcast: " + prot.myBroadcastAddress.getHostAddress());

            WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            WifiManager.MulticastLock lock = wifi.createMulticastLock("my_wifi_lock");
            lock.acquire();
            Boolean started = smartomeSocket.initiate_discovery(prot);
            if (started) {
                iotDevice s;
                do {
                    s = smartomeSocket.scan_devices(prot);

                    if (s != null) {
                        result = true;
                    }

                } while (s != null);

                smartomeSocket.cleanup_discovery(prot);
            }

            lock.release();

            return result;
        }

        protected void onPostExecute(Boolean result) {

            TextView tv = (TextView)findViewById(R.id.hello_world);

            if (result) {
                tv.setText("Device is available");
            } else {
                tv.setText("Device is not available");
            }

        }

    }

}
