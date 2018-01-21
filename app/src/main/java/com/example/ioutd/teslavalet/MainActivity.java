package com.example.ioutd.teslavalet;

import android.app.PendingIntent;
import android.bluetooth.BluetoothA2dp;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.jacksonandroidnetworking.JacksonParserFactory;

import org.json.JSONObject;

import java.util.List;


public class MainActivity extends AppCompatActivity implements BluetoothConnectionListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    public static final int HOURS_24 = 60000 * 60 * 24;

    private GeoDataClient geoDataClient;
    private PlaceDetectionClient placesDetectionClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean mLocationPermissionGranted;
    private LatLng coordinates;

    private List geofenceList;
    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;
    private int radius = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AndroidNetworking.initialize(getApplicationContext());

        // Then set the JacksonParserFactory like below
        AndroidNetworking.setParserFactory(new JacksonParserFactory());

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openTrunk();
            }
        });
        requestLocationPermissions();

        createBroadcastReceiver();
    }

    private void openTrunk() {
        //RESTful API
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

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    private void createBroadcastReceiver() {
        BluetoothBroadcastReceiver receiver = new BluetoothBroadcastReceiver(this);
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);

        registerReceiver(receiver, filter);
    }

    private void getCurrentLocation() {
        if (mLocationPermissionGranted) {
            try {
                Task locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Location lastKnownLocation = (Location) task.getResult();

                            coordinates = new LatLng(lastKnownLocation.getLatitude(),
                                    lastKnownLocation.getLongitude());
                            Log.d(TAG, "getCurrentLocation: " + coordinates.toString());
                        }
                    }

                });
            } catch (SecurityException e) {
                throw new UnsupportedOperationException("Location permission have not been granted");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: was called");
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_FINE_LOCATION:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    mLocationPermissionGranted = true;
        }
    }

    @Override
    public void onBluetoothDisconnect() {
        getCurrentLocation();
        createGeofence();
        addGeofences();
    }

    @Override
    public void onBluetoothConnect() {
        removeGeofences();
    }

    // Use the builder to create a geofence
    public void createGeofence() {
        geofencingClient = LocationServices.getGeofencingClient(this);

        geofenceList.add(new Geofence.Builder()
                .setRequestId("parkingSpot")
                .setCircularRegion(
                        coordinates.latitude,
                        coordinates.longitude,
                        radius
                )
                .setExpirationDuration(HOURS_24)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        );
    }

    // Specify the geofences to monitor and set the triggers
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL);
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    // Define an Intent for the geofence transitions
    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(this, Geofencing.class);
        // FLAG_UPDATE_CURRENT ensures we get the same pending intent when callind add or remove geofences
        geofencePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    private void addGeofences() {
        if (mLocationPermissionGranted) {
            try {
                geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // Geofences have been added
                            }
                        })
                        .addOnFailureListener(this, new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });
            } catch (SecurityException e){
                e.printStackTrace();
            }
        }
    }

    private void removeGeofences() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences have been removed
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to add Geofences
                    }
                });

    }

}
