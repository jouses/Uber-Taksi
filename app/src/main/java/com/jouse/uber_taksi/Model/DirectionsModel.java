package com.jouse.uber_taksi.Model;

import com.google.android.gms.maps.model.LatLng;

public class DirectionsModel {
    public LatLng startLatLng;
    public LatLng endLatLng;

    public DirectionsModel(LatLng startLatLng,LatLng endLatLng){
        this.startLatLng = startLatLng;
        this.endLatLng = endLatLng;
    }
}
