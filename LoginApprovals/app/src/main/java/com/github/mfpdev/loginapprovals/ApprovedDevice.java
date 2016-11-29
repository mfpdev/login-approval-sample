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
