package com.github.mfpdev.loginapprovals;

import android.app.Application;

import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.worklight.wlclient.api.WLClient;

/**
 * Created by ishaib on 23/11/2016.
 */

public class ApprovalsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the MobileFirst SDK. This needs to happen just once.
        WLClient.createInstance(this);

        // Initialize MobileFirst Push SDK. This needs to happen just once.
        MFPPush.getInstance().initialize(this);


    }
}
