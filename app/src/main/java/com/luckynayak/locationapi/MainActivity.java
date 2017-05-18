package com.luckynayak.locationapi;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.luckynayak.library.LocationRequestApi;
import com.luckynayak.library.MapAnimator;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationRequestApi.ILocationChangeListener, LocationRequestApi.PolyLineListener, OnMapReadyCallback, LocationRequestApi.OnDrawDirectionPath {

    private LocationRequestApi requestApi;
    private GoogleMap mGoogleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment fragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().add(R.id.map, fragment).commit();
        requestApi = new LocationRequestApi(this);
        fragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        requestApi.setLocationListener(this, false);
    }

    @Override
    public void onLocationChanges(Location location) {
        LatLng source = new LatLng(location.getLatitude(), location.getLongitude());
        //if you nead Direction from source to destination
        LatLng destination = new LatLng(13.0383897, 77.589661);
        ArrayList<LatLng> wayPoints = new ArrayList<>();
        requestApi.getDirection(source, destination, wayPoints, this);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(source);
        builder.include(destination);
        LatLngBounds bounds = builder.build();
        if (mGoogleMap != null)
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 18));

        // directly plot the route on map
        //requestApi.drawDirection(source, destination, new ArrayList<LatLng>(), mGoogleMap, this);
    }

    @Override
    public void onPolyLineSting(String polyline) {
        // It gives Overview Polyline String
    }

    @Override
    public void onPolyLinePoints(List<LatLng> points) {
        //LatLng point for Direction
        if (points.size() > 1) {
            if (mGoogleMap != null) {
                MapAnimator.getInstance().animateRoute(mGoogleMap, points);
            } else {
                Toast.makeText(getApplicationContext(), "Map not ready", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDrawDirection(boolean isSuccses) {

    }
}
/*
* LocationRequestApi

        method names

getContext
setLocationListener
getDirectionsUrl
removeUpdate
setPolyLineListener
getDirection
getDirection
drawDirection
connectApi
onConnectionSuspended
onLocationChanged
getApiClient
getLatLng
getPolyline
addMarker
addMarker*/
