package com.github.mfpdev;


public class WebClientData {
    private String address;
    private double latitude;
    private double longitude;
    private String date;
    private String platform;
    private String os;

    public WebClientData () {
    }

    public WebClientData(String date, String os, String platform, double latitude, double longitude, String locationDescription) {
        this.date = date;
        this.os = os;
        this.platform = platform;
        this.longitude = longitude;
        this.latitude = latitude;
        this.address = locationDescription;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(long longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(long latitude) {
        this.latitude = latitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }
}
