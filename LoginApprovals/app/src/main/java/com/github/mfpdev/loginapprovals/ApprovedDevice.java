package com.github.mfpdev.loginapprovals;

/**
 * Created by ishaib on 11/11/2016.
 */
public class ApprovedDevice {
    private String id;
    private String address;
    private String date;
    private String platform;
    private String os;

    public ApprovedDevice(String id, String location, String date, String platform, String os) {
        this.id = id;
        this.address = location;
        this.date = date;
        this.platform = platform;
        this.os = os;
    }

    public String getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public String getDate() {
        return date;
    }

    public String getPlatform() {
        return platform;
    }

    public String getOs() {
        return os;
    }
}
