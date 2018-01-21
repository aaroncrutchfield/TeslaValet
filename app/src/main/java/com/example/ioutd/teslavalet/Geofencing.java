package com.example.ioutd.teslavalet;

import android.app.PendingIntent;
import android.content.Context;

import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Created by ioutd on 1/20/2018.
 */

public class Geofencing {
    private Context context;
    private GoogleApiClient apiClient;
    private PendingIntent geofencePendingIntent;
    private
    // TODO: 1/20/2018 Create a geofence using the latlng for the center point and a predefined radius

    Geofencing(Context context, GoogleApiClient apiClient) {
        this.context = context;
        this.apiClient = apiClient;
    }
    // Create a geofence request object
    // PendingIntent will define what to do when the geofence is triggered
        //Use a broadcast receiver and in the onReceive method do the logic

}
