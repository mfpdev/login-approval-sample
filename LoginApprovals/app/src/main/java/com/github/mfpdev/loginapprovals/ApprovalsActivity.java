package com.github.mfpdev.loginapprovals;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushException;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushResponseListener;
import com.worklight.common.Logger;
import com.worklight.wlclient.api.WLClient;
import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLResourceRequest;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.WLResponseListener;

import org.json.JSONObject;

import java.net.URI;

public class ApprovalsActivity extends AppCompatActivity {

    private static final Logger logger = Logger.getInstance(LoginActivity.class.getName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // init the MobileFirst Android SDK
        initWLSDK ();
        setContentView(R.layout.activity_main);
    }

    private void initWLSDK() {
        WLClient.createInstance(this);
        WLClient.getInstance().registerChallengeHandler(new UserLoginChallengeHandler());
        MFPPush.getInstance().initialize(this);
        Boolean isSupported = MFPPush.getInstance().isPushSupported();

        if (isSupported ) {
            initPushSDK();
        } else {
            logger.warn("Push is not supported");
        }
    }

    private void initPushSDK() {
        MFPPush.getInstance().registerDevice(null, new MFPPushResponseListener<String>() {
            @Override
            public void onSuccess(String s) {
                logger.debug("Device registered to push service successfully");
            }

            @Override
            public void onFailure(MFPPushException e) {
                logger.error("Device registered to push service failed");
            }
        });
    }


    public void onButtonClicked (View view) {
        getAppInstances ();
    }
    protected void getAppInstances () {
        URI adapterPath = URI.create("/adapters/LoginApprovalsAdapter/approvals");
        WLResourceRequest resourceRequest = new WLResourceRequest(adapterPath, WLResourceRequest.GET);

        resourceRequest.send(new WLResponseListener() {
            @Override
            public void onSuccess(WLResponse wlResponse) {
                Log.i("ApprovalsActivity", wlResponse.getResponseJSON().toString());
            }

            @Override
            public void onFailure(WLFailResponse wlFailResponse) {
                Log.i("ApprovalsActivity", wlFailResponse.getErrorMsg());
            }
        });

    }
}
