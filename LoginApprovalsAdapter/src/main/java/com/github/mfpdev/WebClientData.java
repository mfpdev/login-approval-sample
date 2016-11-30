/**
 *    © Copyright 2016 IBM Corp.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.mfpdev;


class WebClientData {
    private String clientId;
    private String address;
    private double latitude;
    private double longitude;
    private String date;
    private String platform;
    private String os;

    public WebClientData () {
    }

    WebClientData(String clientId, String date, String os, String platform, double latitude, double longitude, String locationDescription) {
        this.clientId = clientId;
        this.date = date;
        this.os = os;
        this.platform = platform;
        this.longitude = longitude;
        this.latitude = latitude;
        this.address = locationDescription;
    }

    String getDate() {
        return date;
    }

    void setDate(String date) {
        this.date = date;
    }

    double getLongitude() {
        return longitude;
    }

    void setLongitude(long longitude) {
        this.longitude = longitude;
    }

    double getLatitude() {
        return latitude;
    }

    void setLatitude(long latitude) {
        this.latitude = latitude;
    }

    String getAddress() {
        return address;
    }

    void setAddress(String address) {
        this.address = address;
    }

    String getPlatform() {
        return platform;
    }

    void setPlatform(String platform) {
        this.platform = platform;
    }

    String getOs() {
        return os;
    }

    void setOs(String os) {
        this.os = os;
    }

    String getClientId() {
        return clientId;
    }

    void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
