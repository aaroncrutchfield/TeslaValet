package com.example.ioutd.teslavalet;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.Set;

/**
 * Created by ioutd on 1/20/2018.
 */

public class BluetoothBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = BluetoothBroadcastReceiver.class.getSimpleName();
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private BluetoothA2dp bluetoothA2dp;
    private List<BluetoothDevice> bluetoothDeviceList;

    BluetoothDisconnectionListener bluetoothDisconnectionListener;

    BluetoothBroadcastReceiver(BluetoothDisconnectionListener disconnectionListener) {
        bluetoothDisconnectionListener = disconnectionListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
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

//        List<BluetoothDevice> bluetoothDeviceList = bluetoothA2dp.getConnectedDevices();

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)){
            Toast.makeText(context, "Device Connected", Toast.LENGTH_LONG).show();
            Log.d(TAG, "onReceive() returned: " + action);
//            BluetoothDevice device = bluetoothDeviceList.get(0);
//            Log.d(TAG, "onReceive: devices= " + device.getName());

            // TODO: 1/20/2018 check if the device is the tesla, if so, save the mac address
        }

        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
            Toast.makeText(context, "Device Disconnected", Toast.LENGTH_LONG).show();
            Log.d(TAG, "onReceive() returned: " + action);
//            BluetoothDevice device = bluetoothDeviceList.get(0);
//            BluetoothDevice device = bluetoothDeviceList.get(0);
//            Log.d(TAG, "onReceive: devices= " + device.getName());
//            Log.d(TAG, "onReceive: devices= " + device.getName());
            // TODO: 1/20/2018 if the mac address matches the tesla, get the current GPS location
            bluetoothDisconnectionListener.onBluetoothDisconnect();

            // TODO: 1/20/2018 use the GPS location to set the Geofence
            // TODO: 1/20/2018 if the Geofence is entered, check if the current location is a grocery store
            // TODO: 1/20/2018 if the location is a grocery store, open the trunk, else, open the door
        }

    }
}
