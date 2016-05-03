package com.pghadge.map.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class MileMarkerItem implements ClusterItem {
    public final String title;
    public final LatLng mPosition;

    public MileMarkerItem(LatLng position, String title) {
        this.title = title;
        mPosition = position;
    }
    @Override
    public LatLng getPosition() {
        return mPosition;
    }
    public String getTitle() {
        return title;
    }
}
