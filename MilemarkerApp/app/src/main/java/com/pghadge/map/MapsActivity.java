package com.pghadge.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.google.maps.android.clustering.ClusterManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pghadge.map.model.MileMarkerItem;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private TextView textNearestMarker;
    private TextView textDistance_in_Miles;

    private ClusterManager<MileMarkerItem> mClusterManager;
    private Marker routePositionMarker;

    static List<MileMarkerItem> allMileMarkerList = new ArrayList<MileMarkerItem>();
    static MileMarkerItem nearestMarkerItem;
    static LatLng myCurrent_LatLng;
    static float distanceinMile;

    private int mInterval = 2000; // every 2 seconds
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setTitle("Mile Markers");
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
        textNearestMarker = (TextView) findViewById(R.id.text_closest_marker);
        textDistance_in_Miles = (TextView) findViewById(R.id.text_distance_in_mile);
        mHandler = new Handler();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();
        drawMycurrentLocation();
        drawMileMarker();
        updateNextNearestMarker();
    }

    public void drawMycurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location arg0) {
                // TODO Auto-generated method stub
                myCurrent_LatLng = new LatLng(arg0.getLatitude(), arg0.getLongitude());
                mMap.addMarker(new MarkerOptions().position(myCurrent_LatLng).title("It's Me!"));
//                moveCameraToLocation(myCurrent_LatLng); // always resets camera to mylocation
            }
        });
    }

    public void drawMileMarker() {

        mClusterManager = new ClusterManager<MileMarkerItem>(this, mMap);
        mMap.setOnCameraChangeListener(mClusterManager);
        try {
            //draw Marker Oprit
            List<LatLng> itemsOpritLatLng = getRouteLatLngs(R.xml.oprit);
            List<MileMarkerItem> itemsOprit = new ArrayList<>();
            for (int i = 0; i < itemsOpritLatLng.size(); i++) {
                itemsOprit.add(new MileMarkerItem(itemsOpritLatLng.get(i), "oprit Marker " + i));
            }

            //draw Marker A2ReTest
            List<LatLng> itemsA2ReTestLatLng = getRouteLatLngs(R.xml.a2retest);
            List<MileMarkerItem> itemsA2ReTest = new ArrayList<>();
            for (int i = 0; i < itemsA2ReTestLatLng.size(); i++) {
                itemsA2ReTest.add(new MileMarkerItem(itemsA2ReTestLatLng.get(i), " a2retest Marker " + i));
            }

            // add Markers into Cluster
            mClusterManager.addItems(itemsOprit);
            mClusterManager.addItems(itemsA2ReTest);

            // get all markers in one list
            allMileMarkerList.addAll(itemsOprit);
            allMileMarkerList.addAll(itemsA2ReTest);

            initialCameraPosition(itemsA2ReTestLatLng);
        } catch (Exception e) {
            Toast.makeText(this, "Problem in rendering Mile markers..", Toast.LENGTH_LONG);
        }
    }

    private void initialCameraPosition(List<LatLng> routeLatLngs) {
        routePositionMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(routeLatLngs.get(0).latitude, routeLatLngs.get(0).longitude))
                .title("Marker - 0")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)));
        moveCameraToLocation(routePositionMarker.getPosition());
    }

    private void moveCameraToLocation(LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(10).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null);
    }

    private List<LatLng> getRouteLatLngs(int resXml) {
        List<LatLng> latLngs = null;
        XmlPullParser xmlPullParser = getResources().getXml(resXml);
        try {
            latLngs = loadGpxData(xmlPullParser);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return latLngs;
    }

    /**
     * loadGpxData executes the parsing of XML file, selects the Lat and Lng
     */
    private List<LatLng> loadGpxData(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<LatLng> latLngs = new ArrayList<>();
        parser.next();

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            if (parser.getName().equals("wpt")) {
                latLngs.add(new LatLng(
                        Double.valueOf(parser.getAttributeValue(null, "lat")),
                        Double.valueOf(parser.getAttributeValue(null, "lon"))));
            }
        }
        return latLngs;
    }

    private MileMarkerItem findNextNearestMarker(LatLng current, List<MileMarkerItem> mileMarkerItemList) {
        if (mileMarkerItemList.size() == 0 || current == null) {
            return null;
        }
        float markerDistance[] = new float[mileMarkerItemList.size()];

        Location locationCurrent = new Location("GPS");
        locationCurrent.setLatitude(current.latitude);
        locationCurrent.setLatitude(current.longitude);

        Location lTemp = new Location("GPS");
        for (int i = 0; i < mileMarkerItemList.size(); i++) {
            MileMarkerItem mileMarker = mileMarkerItemList.get(i);
            lTemp.setLatitude(mileMarker.getPosition().latitude);
            lTemp.setLatitude(mileMarker.getPosition().longitude);
            markerDistance[i] = locationCurrent.distanceTo(lTemp);
        }

        float min = markerDistance[0];
        for (int i = 1; i < markerDistance.length; i++) {
            min = Math.min(min, markerDistance[i]);
        }

        for (int i = 0; i < markerDistance.length; i++) {
            if (markerDistance[i] == min) {
                distanceinMile = Math.round(markerDistance[i] / 1609); // meter to mile
                return mileMarkerItemList.get(i);
            }
        }
        return null;
    }

    void updateNextNearestMarker() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                nearestMarkerItem = findNextNearestMarker(myCurrent_LatLng, allMileMarkerList);
                if (nearestMarkerItem != null) {
                    textNearestMarker.setText(nearestMarkerItem.getTitle());
                    textDistance_in_Miles.setText(distanceinMile +" Mile(s)");

                } else {
                    textNearestMarker.setText("-"); // still calculating..
                    textDistance_in_Miles.setText("-");
                }
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };
}
