package in.lubble.app.auth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

import in.lubble.app.BuildConfig;
import in.lubble.app.LubbleSharedPrefs;
import in.lubble.app.R;
import in.lubble.app.analytics.Analytics;
import in.lubble.app.analytics.AnalyticsEvents;
import in.lubble.app.firebase.RealtimeDbHelper;
import in.lubble.app.utils.StringUtils;

public class LocationActivity extends AppCompatActivity {

    private static final String TAG = "LocationActivity";
    private static final int REQUEST_SYSTEM_LOCATION = 859;
    private static final int REQUEST_LOCATION_ON = 150;

    private FusedLocationProviderClient fusedLocationClient;
    private LinearLayout invalidLocContainer;
    private TextView locHintTv;
    private Button okBtn;
    private Parcelable idpResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        invalidLocContainer = findViewById(R.id.invalid_loc_container);
        locHintTv = findViewById(R.id.tv_loc_hint);
        okBtn = findViewById(R.id.btn_invalid_loc_ok);

        idpResponse = getIntent().getParcelableExtra("idpResponse");

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });

        checkSystemLocPerm();
    }


    private void checkSystemLocPerm() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setMessage("We need your location to connect you with people nearby.\nWithout this permission, you cannot use Lubble");
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(LocationActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_SYSTEM_LOCATION);
                    }
                });
                alertDialog.setCancelable(false);
                alertDialog.show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_SYSTEM_LOCATION);
            }
        } else {
            // Permission has already been granted
            // check if location is enabled
            checkLocationSettings();
        }
    }

    private void checkLocationSettings() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(getLocationRequest());

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                getLocation();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(LocationActivity.this, REQUEST_LOCATION_ON);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_SYSTEM_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    checkLocationSettings();
                } else {
                    // permission denied, boo!
                    checkSystemLocPerm();
                }
                return;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOCATION_ON) {
            if (resultCode == Activity.RESULT_OK) {
                //fetch current location
                getLocation();
            } else {
                checkLocationSettings();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null &&
                        SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()
                                < TimeUnit.MINUTES.toNanos(10)) {
                    // Logic to handle location object
                    validateUserLocation(location);
                } else {
                    // get fresh loc
                    fusedLocationClient.requestLocationUpdates(getLocationRequest().setNumUpdates(1), new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            if (locationResult == null) {
                                return;
                            }
                            validateUserLocation(locationResult.getLastLocation());
                        }

                        ;
                    }, null);
                }
            }
        });
    }

    private void validateUserLocation(Location location) {
        final Location centralLocation = new Location("Saraswati Vihar");
        centralLocation.setLatitude(28.696660);
        centralLocation.setLongitude(77.124772);
        if (location.distanceTo(centralLocation) < 700) {
            locationCheckSuccess();
        } else {
            checkBackdoorAccess();
        }
    }

    private void checkBackdoorAccess() {
        String backdoorKey = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        if (backdoorKey == null && BuildConfig.DEBUG) {
            // just for ishaan's emulator to allow email ID
            backdoorKey = FirebaseAuth.getInstance().getUid();
        }
        RealtimeDbHelper.getBackdoorRef().child(backdoorKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final String lubbleId = dataSnapshot.getValue(String.class);
                        if (StringUtils.isValidString(lubbleId)) {
                            LubbleSharedPrefs.getInstance().setLubbleId(lubbleId);
                            locationCheckSuccess();
                        } else {
                            locationCheckFailed();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // user has NO backdoor access
                        locationCheckFailed();
                    }
                });
    }

    private void locationCheckSuccess() {
        Intent intent = new Intent();
        intent.putExtra("idpResponse", idpResponse);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void locationCheckFailed() {
        locHintTv.setVisibility(View.GONE);
        invalidLocContainer.setVisibility(View.VISIBLE);
        Analytics.triggerEvent(AnalyticsEvents.LOC_CHECK_FAILED, this);
    }

    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

}