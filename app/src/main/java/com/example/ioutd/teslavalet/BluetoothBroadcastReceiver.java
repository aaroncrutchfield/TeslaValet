package com.example.ioutd.teslavalet;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

/**
 * Created by ioutd on 1/20/2018.
 */

public class BluetoothBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = BluetoothBroadcastReceiver.class.getSimpleName();
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private BluetoothA2dp bluetoothA2dp;
    private List<BluetoothDevice> bluetoothDeviceList;

    BluetoothConnectionListener bluetoothConnectionListener;
    static boolean connected = false;

    public BluetoothBroadcastReceiver(){}

    BluetoothBroadcastReceiver(BluetoothConnectionListener disconnectionListener) {
        bluetoothConnectionListener = disconnectionListener;
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "bluetooth isn't supported", Toast.LENGTH_LONG).show();
            return;
        }

        // Make sure the bluetooth is turned on
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            Activity activity = (Activity) context;
            activity.startActivityForResult(enableBluetooth, REQUEST_ENABLE_BLUETOOTH);
        }

        BluetoothProfile.ServiceListener serviceListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
                if (i == BluetoothProfile.A2DP) {
                    Log.d(TAG, "onServiceConnected: profile= " + i);
                    Log.d(TAG, "onServiceConnected: bluetooth=" + bluetoothProfile);
                    bluetoothA2dp = (BluetoothA2dp) bluetoothProfile;
                    bluetoothDeviceList = bluetoothProfile.getConnectedDevices();

                    BluetoothA2dp a2dpService = (BluetoothA2dp)bluetoothProfile;

                    List<BluetoothDevice> bluetoothDeviceList = a2dpService.getConnectedDevices();

                    Log.d(TAG, "onServiceConnected: List= " + bluetoothDeviceList);
                }
            }

            @Override
            public void onServiceDisconnected(int i) {
                if (i == BluetoothProfile.A2DP) {
                    bluetoothA2dp = null;

                }
            }
        };


        bluetoothAdapter.getProfileProxy(context, serviceListener, BluetoothProfile.A2DP);

        if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action) && bluetoothDeviceList != null) {
            if (bluetoothDeviceList.isEmpty()) {
                bluetoothConnectionListener.onBluetoothDisconnect();
                Toast.makeText(context, "Device Disconnected", Toast.LENGTH_LONG).show();
                return;
            }

            BluetoothDevice device = bluetoothDeviceList.get(0);
            int connectionState = bluetoothA2dp.getConnectionState(device);

            if (connectionState == BluetoothA2dp.STATE_CONNECTED) {
                Toast.makeText(context, "Device Connected", Toast.LENGTH_LONG).show();
                bluetoothConnectionListener.onBluetoothConnect();
            }
        }
    }
}
