package com.example.ioutd.teslavalet;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.json.JSONObject;

/**
 * Created by ioutd on 1/20/2018.
 */

public class Geofencing extends IntentService{

    private static final String TAG = Geofencing.class.getSimpleName();

    public Geofencing(){super(TAG);}

    public Geofencing(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent != null && geofencingEvent.hasError()) {
            int errorCode = geofencingEvent.getErrorCode();
            Log.d(TAG, "onHandleIntent: errorCode= " + errorCode);
        }

        // Get the transition type
        if(geofencingEvent != null){
            int geofenceTransition = geofencingEvent.getGeofenceTransition();

            // Make sure the transition is the one desired
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                // TODO: 1/21/2018 send an HTTP REQUEST to open doors/trunk
                new MainActivity().openDoors();
            }
        }
    }

}
