package com.jouse.uber_taksi.Data;

public class JsonData {
    public boolean uber;
    public String marka;
    public Double latitude;
    public Double longitude;
    public int mesafe;

    public JsonData(boolean uber, String marka, Double latitude, Double longitude, int mesafe){
        this.uber = uber;
        this.marka = marka;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mesafe = mesafe;
    }

    public JsonData(){

    }
}
