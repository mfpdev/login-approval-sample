package com.github.mfpdev;


import java.io.Serializable;

public class WebClientData {
    private String location;
    private String date;
    private String agent;

    public WebClientData () {
    }

    public WebClientData(String location, String date, String agent) {
        this.location = location;
        this.date = date;
        this.agent = agent;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }


    public String getLocation() {
        return location;
    }

    public String getDate() {
        return date;
    }

    public String getAgent() {
        return agent;
    }
}
