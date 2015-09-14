package com.example.android.sunshine.app.data;

/**
 * Created by pcarrillo on 14/09/2015.
 */
public class Weather {

    private int weatherId;
    private double cityLatitude;
    private double cityLongitude;
    private String high;
    private String low;

    public Weather(int weatherId, double cityLatitude, double cityLongitude, String high, String low) {
        this.weatherId = weatherId;
        this.cityLatitude = cityLatitude;
        this.cityLongitude = cityLongitude;
        this.high = high;
        this.low = low;
    }

    public double getCityLatitude() {
        return cityLatitude;
    }

    public void setCityLatitude(double cityLatitude) {
        this.cityLatitude = cityLatitude;
    }

    public double getCityLongitude() {
        return cityLongitude;
    }

    public void setCityLongitude(double cityLongitude) {
        this.cityLongitude = cityLongitude;
    }

    public String getHigh() {
        return high;
    }

    public void setHigh(String high) {
        this.high = high;
    }

    public String getLow() {
        return low;
    }

    public void setLow(String low) {
        this.low = low;
    }

    public int getWeatherId() {
        return weatherId;
    }

    public void setWeatherId(int weatherId) {
        this.weatherId = weatherId;
    }
}
