package com.luckynayak.library;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;

/**
 * Created by Lucky on 2/7/2017.
 */

public class LocationRequestApi implements LocationListener, GoogleApiClient.ConnectionCallbacks {
    private final Context context;
    private boolean needLocationUpdate;
    private com.android.volley.RequestQueue q;
    private ILocationChangeListener locationChangeListener;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    public Context getContext() {
        return context;
    }

    private PolyLineListener polyLineListener;

    public LocationRequestApi(Context context) {
        this.context = context;
        q = Volley.newRequestQueue(context);
        requestPermission();
        buildGoogleApiClient();
    }


    public void setLocationListener(ILocationChangeListener listener, boolean needLocationUpdate) {
        locationChangeListener = listener;
        this.needLocationUpdate = needLocationUpdate;
        buildGoogleApiClient();
    }

    private String getDirectionsUrl(@NonNull LatLng source, @NonNull LatLng dest, ArrayList<LatLng> wayPoints) {

        String enRoute = "";

        // Origin of route
        String str_origin = "origin=" + source.latitude + "," + source.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Waypoints
        if (wayPoints != null)
            if (!wayPoints.isEmpty()) {
                enRoute = "&waypoints=" + "optimize:true" + "|";
                for (LatLng latLng : wayPoints) {
                    enRoute += latLng.latitude + "," + latLng.longitude + "|";
                }
            }
        String parameters = str_origin + "&" + str_dest + "&" + sensor + enRoute;

        String output = "json";

        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&mode=driving";

        Log.wtf("url", url);
        return url;
    }

    public void requestPermission() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions((Activity) getContext(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100);
        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            createLocationRequest();
            //showGPSDisabledAlertToUser();
        }

    }

    public void createLocationRequest() {
        if (mGoogleApiClient == null)
            buildGoogleApiClient();
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        //final GoogleApiClient finalMGoogleApiClient = mGoogleApiClient;
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {

                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        //((BaseActivity)getContext()).onActivityResult(LocationSettingsStatusCodes.SUCCESS, 1, null);
                        //enabledLock.unlock();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult((Activity) getContext(), 42);
                        } catch (IntentSender.SendIntentException e) {
                        }
                        break;
                }

            }
        });
    }

    protected void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(10000); //10 seconds
            mLocationRequest.setFastestInterval(10000); //10 seconds
            mLocationRequest.setPriority(PRIORITY_BALANCED_POWER_ACCURACY);
            //mLocationRequest.setSmallestDisplacement(0.1F); //1/10 meter

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch (Exception e) {

        }
    }

    public void removeUpdate() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    public void setPolyLineListener(PolyLineListener polyLineListener) {
        this.polyLineListener = polyLineListener;
    }

    public void getDirection(LatLng source, LatLng destination, PolyLineListener polyLineListener) {
        this.polyLineListener = polyLineListener;
        getPolyline(getDirectionsUrl(source, destination, new ArrayList<LatLng>()), polyLineListener);
    }

    public void getDirection(LatLng source, LatLng destination, ArrayList<LatLng> wayPoints, PolyLineListener polyLineListener) {
        this.polyLineListener = polyLineListener;
        getPolyline(getDirectionsUrl(source, destination, wayPoints), polyLineListener);
    }

    public void drawDirection(LatLng source, LatLng destination, ArrayList<LatLng> wayPoints, final GoogleMap googleMap, final OnDrawDirectionPath onDrawDirectionPath) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(source);
        builder.include(destination);
        LatLngBounds bounds = builder.build();
        if (googleMap != null)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 18));
        getPolyline(getDirectionsUrl(source, destination, wayPoints), new PolyLineListener() {
            @Override
            public void onPolyLineSting(String polyline) {
                onDrawDirectionPath.onDrawDirection(true);
            }

            @Override
            public void onPolyLinePoints(List<LatLng> points) {
                if (points.size() > 1) {
                    if (googleMap != null) {
                        MapAnimator.getInstance().animateRoute(googleMap, points);
                    }
                    if (onDrawDirectionPath != null)
                        onDrawDirectionPath.onDrawDirection(true);
                } else {
                    if (onDrawDirectionPath != null)
                        onDrawDirectionPath.onDrawDirection(true);
                }
            }
        });
    }


    public interface OnDrawDirectionPath {
        void onDrawDirection(boolean isSuccses);
    }

    public interface ILocationChangeListener {
        public void onLocationChanges(Location location);
    }


    public void connectApi() {

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        else buildGoogleApiClient();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (!needLocationUpdate)
            removeUpdate();
        locationChangeListener.onLocationChanges(location);
    }

    public GoogleApiClient getApiClient() {
        if (mGoogleApiClient == null)
            buildGoogleApiClient();
        return mGoogleApiClient;
    }

    private synchronized void setPolyLineArray(JSONObject poly) {
        List<LatLng> allPoints = new ArrayList<>();
        try {
            JSONArray routes = poly.getJSONArray("routes");
            for (int i = 0; i < routes.length(); i++) {
                JSONArray legs = ((JSONObject) routes.get(i)).getJSONArray("legs");
                for (int j = 0; j < legs.length(); j++) {
                    JSONArray steps = ((JSONObject) legs.get(j)).getJSONArray("steps");
                    for (int k = 0; k < steps.length(); k++) {
                        String polyline = (String) ((JSONObject) ((JSONObject) steps.get(k)).get("polyline")).get("points");
                        allPoints.addAll(decodePoly(polyline));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (polyLineListener != null)
            polyLineListener.onPolyLinePoints(allPoints);
    }

    /**
     * Method to decode polyline points
     * Courtesy : http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
     */
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));

            poly.add(p);
        }

        return poly;
    }

    public synchronized void setPolyLine(JSONObject polyLineObject) {
        String polyLine = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            JSONArray jRoutes = polyLineObject.getJSONArray("routes");
            for (int i = 0; i < jRoutes.length(); i++) {
                JSONObject poly = ((JSONObject) jRoutes.get(i)).getJSONObject("overview_polyline");
                stringBuilder.append(poly.getString("points"));
            }
            polyLine = stringBuilder.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (polyLineListener != null) {
            polyLineListener.onPolyLineSting(polyLine);
        }
        //this.polyLine = polyLine;
    }

    public void getPolyline(String url, PolyLineListener polyLineListener) {
        this.polyLineListener = polyLineListener;
        // String url = "http://gng.vuln.in:7070/otp/verify?mobile=" + phoneNumber + "&otp=" + otpNumber;
        Logs.wtf(this, "Url", url);
        JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        setPolyLine(response);
                        setPolyLineArray(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Logs.wtf(this, " Volley", error.toString() + "\n" + error.getMessage());
                        //Toast.makeText(Step1Activity.this, error.toString(), Toast.LENGTH_LONG).show();
                    }
                });


        q.add(stringRequest);
    }

    public interface OnReverseGeoCodeResult {
        void onReverseGeoCodeResult(LatLng result);
    }

    public interface PolyLineListener {
        void onPolyLineSting(String polyline);

        void onPolyLinePoints(List<LatLng> points);
    }

    public interface AddOnMarkerListener {
        void onMarkerAdded(Marker marker);
    }

    public void addMarker(final String url, final String title, final double lat, final double lng, final GoogleMap map, final AddOnMarkerListener addOnMarkerListener) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                InputStream bmp = null;
                try {
                    bmp = new URL(url).openConnection().getInputStream();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                final Bitmap finalBmp = BitmapFactory.decodeStream(bmp);
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        MarkerOptions marker = new MarkerOptions()
                                .position(new LatLng(lat, lng))
                                .icon(BitmapDescriptorFactory.fromBitmap(finalBmp))
                                .title(title);
                        Marker dm = map.addMarker(marker);
                        dm.showInfoWindow();
                        if (addOnMarkerListener != null)
                            addOnMarkerListener.onMarkerAdded(dm);

                    }
                });
            }
        });
        thread.start();

    }

    public void addMarker(final int resId, final String title, final double lat, final double lng, final GoogleMap map, final AddOnMarkerListener addOnMarkerListener) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                final Bitmap finalBmp = BitmapFactory.decodeResource(context.getResources(), resId);
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        MarkerOptions marker = new MarkerOptions()
                                .position(new LatLng(lat, lng))
                                .icon(BitmapDescriptorFactory.fromBitmap(finalBmp))
                                .title(title);
                        Marker dm = map.addMarker(marker);
                        dm.showInfoWindow();
                        if (addOnMarkerListener != null)
                            addOnMarkerListener.onMarkerAdded(dm);

                    }
                });
            }
        });
        thread.start();

    }

}
