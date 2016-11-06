package test.yaz.assignment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.kml.KmlLayer;
import org.xmlpull.v1.XmlPullParserException;
import test.yaz.assignment.features.foursquare.FoursquarePresenter;
import test.yaz.assignment.features.foursquare.FoursquareResponse;
import test.yaz.assignment.features.foursquare.FoursquareView;
import test.yaz.assignment.features.foursquare.RestauranatsDialog;
import test.yaz.assignment.features.whereami.LocationDialog;
import test.yaz.assignment.features.whereami.WorldPresenter;
import test.yaz.assignment.features.whereami.WorldView;
import test.yaz.assignment.utils.PermissionUtils;
import timber.log.Timber;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<LocationSettingsResult>, FoursquareView, BottomNavigationView.OnNavigationItemSelectedListener, WorldView {

    @Retention(SOURCE)
    @IntDef({MODE_FOURSQUARE, MODE_MYLOCATION})
    public @interface Mode {

    }

    public static final int MODE_FOURSQUARE = 0;

    public static final int MODE_MYLOCATION = 1;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Constant used in the location settings dialog.
     */
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private static final float MIN_DISTANCE_TO_TRIGGER_METERS = 50f;

    @BindView(R.id.bottom_navigation)
    BottomNavigationView bottomNavigation;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    private boolean mPermissionDenied = false;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    protected LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    private GoogleMap mMap;

    private boolean mRequestingLocationUpdates;

    private SupportMapFragment mapFragment;

    @Inject
    FoursquarePresenter foursquarePresenter;

    @Inject
    WorldPresenter worldPresenter;

    private ArrayList<String> mVenuesTitles = new ArrayList<>();

    private boolean distanceTriggered = true;//true for the first call

    private WeakLocationListener weakLocationListener;

    private String mCurrentCountry;

    private
    @Mode
    int mode = MODE_FOURSQUARE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        buildGoogleApiClient();
        App.get(this).getAppComponent().inject(this);
        foursquarePresenter.attachView(this);
        worldPresenter.attachView(this);
        bottomNavigation.setOnNavigationItemSelectedListener(this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        weakLocationListener = null;
        foursquarePresenter.detachView();
        worldPresenter.detachView();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        menu.clear();
        if (mode == MODE_FOURSQUARE && !mVenuesTitles.isEmpty()) {
            getMenuInflater().inflate(R.menu.menu_foursquare, menu);
            return true;
        } else if (mode == MODE_MYLOCATION && !TextUtils.isEmpty(mCurrentCountry)) {
            getMenuInflater().inflate(R.menu.menu_location, menu);
            return true;
        } else {
            return super.onCreateOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_list:
                //show dialog
                RestauranatsDialog.newInstance(mVenuesTitles).show(getSupportFragmentManager(), "listDialog");
                return true;

            case R.id.action_location:
                //show dialog
                LocationDialog.newInstance(mCurrentCountry).show(getSupportFragmentManager(), "locationDialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Timber.i("Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else {
            createLocationRequest();
            buildLocationSettingsRequest();
            checkLocationSettings();
        }
    }

    protected void checkLocationSettings() {
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleApiClient,
                        mLocationSettingsRequest
                );
        result.setResultCallback(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // recommended in applications that request frequent location updates.
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient,
                weakLocationListener
        ).setResultCallback(status -> {
            mRequestingLocationUpdates = false;
        });
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    @SuppressWarnings("MissingPermission")
    protected void startLocationUpdates() {
        weakLocationListener = new WeakLocationListener(this);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                mLocationRequest,
                weakLocationListener)
                .setResultCallback(status ->
                        mRequestingLocationUpdates = true
                );

        if (mMap != null) {
            Timber.d("setMyLocationEnabled");
            mMap.setMyLocationEnabled(true);

            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setAllGesturesEnabled(true);
        }
    }

    @Override
    public void onLocationChanged(final Location location) {
        Timber.d("onLocationChanged() called with: " + "location = [" + location + "]");
        switch (mode) {
            case MODE_FOURSQUARE:
                if (mCurrentLocation != null && location.distanceTo(mCurrentLocation) > MIN_DISTANCE_TO_TRIGGER_METERS) {
                    Toast.makeText(this, getResources().getString(R.string.distance_triggered),
                            Toast.LENGTH_SHORT).show();
                    //flag should hit API
                    distanceTriggered = true;
                }
                mCurrentLocation = location;

                if (mCurrentLocation != null && distanceTriggered) {
                    foursquarePresenter.fetchRestaurants(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                }
                break;
            case MODE_MYLOCATION:
                if (location == null) return;
                worldPresenter.determineBounds(new LatLng(location.getLatitude(), location.getLongitude()));
                break;
        }
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        Timber.i("Connected to GoogleApiClient");
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }

    }

    @Override
    public void onConnectionSuspended(final int i) {
        Timber.i("Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        Timber.e("onConnectionFailed() called with: " + "connectionResult = [" + connectionResult + "]");
    }

    @Override
    public void onResult(@NonNull final LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Timber.i("All location settings are satisfied.");
                startLocationUpdates();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Timber.i("Location settings are not satisfied. Show the user a dialog to" +
                        "upgrade location settings ");
                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result
                    // in onActivityResult().
                    status.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    Timber.e("PendingIntent unable to execute request.");
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Timber.i("Location settings are inadequate, and cannot be fixed here. Dialog " +
                        "not created.");
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Timber.d("onActivityResult() called with: " + "requestCode = [" + requestCode + "], resultCode = [" + resultCode + "], data = [" + data + "]");
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Timber.i("User agreed to make required location settings changes.");
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Timber.i("User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
    }

    @Override
    public void displayPoints(final List<FoursquareResponse.Venue> points) {
        distanceTriggered = false;
        mMap.clear();
        mVenuesTitles.clear();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (final FoursquareResponse.Venue point : points) {
            mVenuesTitles.add(point.name);
            LatLng latLng = new LatLng(point.location.lat, point.location.lng);
            builder.include(latLng);
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(point.name)
                    .snippet(point.location.address)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
        }
        if (!mVenuesTitles.isEmpty()) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
            supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void flipProgress(final boolean displayProgress) {
        progressBar.setVisibility(displayProgress ?
                View.VISIBLE :
                View.INVISIBLE);
    }

    @Override
    public void displayError(final String message) {
        Toast.makeText(this, "Error: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        supportInvalidateOptionsMenu();
        switch (item.getItemId()) {
            case R.id.action_first:
                if (mode == MODE_FOURSQUARE) return true;
                mode = MODE_FOURSQUARE;
                mMap.clear();
                distanceTriggered = true;
                onLocationChanged(mCurrentLocation);
                return true;
            case R.id.action_second:
                if (mode == MODE_MYLOCATION) return true;
                mode = MODE_MYLOCATION;
                mMap.clear();
                worldPresenter.loadKmlLayer(mMap);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void renderLayer(final KmlLayer kmlLayer) {
        try {
            kmlLayer.addLayerToMap();
        } catch (IOException | XmlPullParserException e) {
            Timber.e(e);
        }
        if (mCurrentLocation != null) {
            worldPresenter.determineBounds(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
        }
    }

    @Override
    public void notifyPlaceIsKnown(final String countryName, @Nullable LatLngBounds bounds) {
        mCurrentCountry = countryName;
        supportInvalidateOptionsMenu();
        if (bounds != null)
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
    }

    @Override
    public void notifyPlaceIsUnknown() {
        mCurrentCountry = null;
        Toast.makeText(this, R.string.location_unknown, Toast.LENGTH_SHORT).show();
        supportInvalidateOptionsMenu();
    }

    public class WeakLocationListener implements LocationListener {

        private final WeakReference<LocationListener> locationListenerRef;

        public WeakLocationListener(@NonNull LocationListener locationListener) {
            locationListenerRef = new WeakReference<>(locationListener);
        }

        @Override
        public void onLocationChanged(Location location) {
            if (locationListenerRef.get() == null) {
                return;
            }
            locationListenerRef.get().onLocationChanged(location);
        }
    }
}
