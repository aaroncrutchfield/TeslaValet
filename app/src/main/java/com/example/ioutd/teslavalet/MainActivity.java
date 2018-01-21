package com.example.ioutd.teslavalet;

import android.app.PendingIntent;
import android.bluetooth.BluetoothA2dp;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.jacksonandroidnetworking.JacksonParserFactory;

import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements BluetoothConnectionListener,
        OnMapReadyCallback {

    static final String TAG = MainActivity.class.getSimpleName();
    static final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    static final int HOURS_24 = 60000 * 60 * 24;

    GeoDataClient geoDataClient;
    PlaceDetectionClient placesDetectionClient;
    boolean mLocationPermissionGranted;
    LatLng coordinates;

    Geofence geofence;
    GeofencingClient geofencingClient;
    PendingIntent geofencePendingIntent;
    int radius = 5;
    GoogleMap googleMap;

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
                openDoors();
            }
        });
        requestLocationPermissions();

        createBroadcastReceiver();

        configSharedPreferences();

        getCurrentLocation();

        // Add the map fragment
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void configSharedPreferences() {
        Switch trunk = findViewById(R.id.trunkSwitch);
        trunk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getSharedPreferences(getString(R.string.trunk_pref), Context.MODE_PRIVATE).edit().putBoolean(getString(R.string.trunk_pref), b).apply();
            }
        });

        Switch frunk = findViewById(R.id.frunkSwitch);
        frunk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getSharedPreferences(getString(R.string.frunk_pref), Context.MODE_PRIVATE).edit().putBoolean(getString(R.string.frunk_pref), b).apply();
            }
        });

        Switch driver = findViewById(R.id.driverSwitch);
        driver.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getSharedPreferences(getString(R.string.driver_pref), Context.MODE_PRIVATE).edit().putBoolean(getString(R.string.driver_pref), b).apply();
            }
        });

        Switch passenger = findViewById(R.id.passengerSwitch);
        passenger.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getSharedPreferences(getString(R.string.passenger_pref), Context.MODE_PRIVATE).edit().putBoolean(getString(R.string.passenger_pref), b).apply();
            }
        });

        Switch left = findViewById(R.id.leftSwitch);
        left.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getSharedPreferences(getString(R.string.left_pref), Context.MODE_PRIVATE).edit().putBoolean(getString(R.string.left_pref), b).apply();
            }
        });

        Switch right = findViewById(R.id.rightSwitch);
        right.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getSharedPreferences(getString(R.string.right_pref), Context.MODE_PRIVATE).edit().putBoolean(getString(R.string.right_pref), b).apply();
            }
        });

        trunk.setChecked(getSharedPreferences(getString(R.string.trunk_pref), Context.MODE_PRIVATE).getBoolean(getString(R.string.trunk_pref), false));
        frunk.setChecked(getSharedPreferences(getString(R.string.frunk_pref), Context.MODE_PRIVATE).getBoolean(getString(R.string.frunk_pref), false));
        driver.setChecked(getSharedPreferences(getString(R.string.driver_pref), Context.MODE_PRIVATE).getBoolean(getString(R.string.driver_pref), true));
        passenger.setChecked(getSharedPreferences(getString(R.string.passenger_pref), Context.MODE_PRIVATE).getBoolean(getString(R.string.passenger_pref), false));
        left.setChecked(getSharedPreferences(getString(R.string.left_pref), Context.MODE_PRIVATE).getBoolean(getString(R.string.left_pref), false));
        right.setChecked(getSharedPreferences(getString(R.string.right_pref), Context.MODE_PRIVATE).getBoolean(getString(R.string.right_pref), false));
    }

    public void openDoors() {
        //RESTful API

        String commandFrunk = null;
        String frunk = null;

        String commandTrunk = null;
        String trunk = null;

        String commandDriver = null;
        String driver = null;

        String commandPassenger = null;
        String passenger = null;

        String commandLeft = null;
        String left = null;

        String commandRight = null;
        String right = null;

        if(getSharedPreferences(getString(R.string.frunk_pref), Context.MODE_PRIVATE).getBoolean(getString(R.string.frunk_pref), false)){
            commandFrunk = "command";
            frunk = "frunk";
        }
        if(getSharedPreferences(getString(R.string.trunk_pref), Context.MODE_PRIVATE).getBoolean(getString(R.string.trunk_pref), false)){
            commandTrunk = "command";
            trunk = "trunk";
        }
        if(getSharedPreferences(getString(R.string.driver_pref), Context.MODE_PRIVATE).getBoolean(getString(R.string.driver_pref), true)){
            commandDriver = "command";
            driver = "openFrontDriverDoor";
        }
        if(getSharedPreferences(getString(R.string.passenger_pref), Context.MODE_PRIVATE).getBoolean(getString(R.string.passenger_pref), false)){
            commandPassenger = "command";
            passenger = "openFrontPassengerDoor";
        }
        if(getSharedPreferences(getString(R.string.left_pref), Context.MODE_PRIVATE).getBoolean(getString(R.string.left_pref), false)){
            commandLeft = "command";
            left = "openBackPassengerDoor";
        }
        if(getSharedPreferences(getString(R.string.right_pref), Context.MODE_PRIVATE).getBoolean(getString(R.string.right_pref), false)){
            commandRight = "command";
            right = "openBackDriverDoor";
        }


        AndroidNetworking.post("http://hackathon.intrepidcs.com/api/data")
                .addHeaders("Authorization", "Bearer c367b9df3ed900f462b2fc8dea1b73c26d5bd798d0fd732019133f8cb9ee7671")
                .addBodyParameter(commandFrunk, frunk)
                .addBodyParameter(commandTrunk, trunk)
                .addBodyParameter(commandDriver, driver)
                .addBodyParameter(commandPassenger, passenger)
                .addBodyParameter(commandLeft, left)
                .addBodyParameter(commandRight, right)
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
        Log.d(TAG, "getCurrentLocation: was called");
        if (mLocationPermissionGranted) {
            try {
                FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                Task locationResult = fusedLocationProviderClient.getLastLocation();
                Log.d(TAG, "getCurrentLocation: locationResult= " + locationResult);
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        Log.d(TAG, "onComplete: task= " + task);
                        if (task.isSuccessful()) {
                            Location lastKnownLocation = (Location) task.getResult();

                            coordinates = new LatLng(lastKnownLocation.getLatitude(),
                                    lastKnownLocation.getLongitude());

                            Log.d(TAG, "onComplete: location= " + coordinates);

                            // TODO: 1/21/2018 find out why circle for geofence isn't filled in
                            googleMap.addCircle(new CircleOptions()
                                    .center(coordinates)
                                    .strokeColor(Color.CYAN));
                            googleMap.addMarker(new MarkerOptions()
                                    .position(coordinates)
                                    .title("Parking Location"));

                            createGeofence();

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
        Log.d(TAG, "onBluetoothDisconnect: was called");
        getCurrentLocation();
    }

    @Override
    public void onBluetoothConnect() {
        Log.d(TAG, "onBluetoothConnect: was called");
        if (geofencingClient != null) {
            removeGeofences();
        }
    }

    // Use the builder to create a geofence
    public void createGeofence() {
        Log.d(TAG, "createGeofence: was called");
        geofencingClient = LocationServices.getGeofencingClient(this);

        geofence = new Geofence.Builder()
                .setRequestId("parkingSpot")
                .setCircularRegion(
                        coordinates.latitude,
                        coordinates.longitude,
                        radius
                )
                .setExpirationDuration(HOURS_24)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        addGeofences();
    }

    // Specify the geofences to monitor and set the triggers
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL);
        builder.addGeofence(geofence);
        Log.d(TAG, "getGeofencingRequest: " + geofence.toString());
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
        Log.d(TAG, "addGeofences: was called");
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

    }
}
