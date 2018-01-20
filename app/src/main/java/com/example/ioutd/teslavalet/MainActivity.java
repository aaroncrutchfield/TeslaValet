package com.example.ioutd.teslavalet;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;

    // Get the default bluetooth adapter
    BluetoothHeadset bluetoothHeadset;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)actionBar.setTitle("A2DP watcher");
        
         AndroidNetworking.initialize(getApplicationContext());

        // Then set the JacksonParserFactory like below
        AndroidNetworking.setParserFactory(new JacksonParserFactory());
        //RESTful API

        Button button=findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            AndroidNetworking.post("http://hackathon.intrepidcs.com/api/data")
                    .addHeaders("Authorization", "Bearer c367b9df3ed900f462b2fc8dea1b73c26d5bd798d0fd732019133f8cb9ee7671")
                    .addBodyParameter("command", "trunk")
                    .setTag("trunk_test")
                    .setPriority(Priority.MEDIUM)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // do anything with response
                            Log.d("Trunk_test", "response " + response);
                        }
                        @Override
                        public void onError(ANError error) {
                            // handle error
                            Log.d("Trunk_test", "response_error " + error.getErrorBody());
                        }
                    });
            }
        });
        BluetoothBroadcastReceiver receiver = new BluetoothBroadcastReceiver();
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
//        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
//        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        registerReceiver(receiver, filter);

        new Runnable() {
            @Override
            public void run() {

            }
        };
    }



}
