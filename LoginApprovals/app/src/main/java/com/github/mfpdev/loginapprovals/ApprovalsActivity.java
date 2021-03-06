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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushException;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushNotificationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPSimplePushNotification;
import com.worklight.common.Logger;
import com.worklight.wlclient.api.WLClient;
import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLResourceRequest;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.WLResponseListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;

public class ApprovalsActivity extends AppCompatActivity implements MFPPushNotificationListener {

    private static final Logger logger = Logger.getInstance(LoginActivity.class.getName());
    public static final String DATE_EXTRA_KEY = "date";
    public static final String LOCATION_EXTRA_KEY = "location";
    public static final String PLATFORM_EXTRA_KEY = "platform";
    public static final String OS_EXTRA_KEY = "os";
    public static final String CLIENTID_EXTRA_KEY = "clientId";

    public static final int APPROVALS_ACTIVITY_CODE = 198;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private ListView approvalsListView;

    private boolean isInApprovalProcess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // init the MobileFirst Android SDK
        setContentView(R.layout.activity_main);

        initWLSDK();
        MFPPush.getInstance().listen(this);
        approvalsListView = (ListView) findViewById(R.id.approvals_list_view);
        getApprovedClients();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.approvals, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                getApprovedClients();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //if (requestCode == APPROVALS_ACTIVITY_CODE && resultCode == RESULT_OK) {
            getApprovedClients();
            isInApprovalProcess = false;
        //}
    }

    @Override
    public void onReceive(MFPSimplePushNotification mfpSimplePushNotification) {
        if (!isInApprovalProcess) {
            try {
                JSONObject payload = new JSONObject(mfpSimplePushNotification.getPayload());
                Intent intent = new Intent(WLClient.getInstance().getContext(), ApprovalActivity.class);
                intent.putExtra(DATE_EXTRA_KEY, (String) payload.get("date"));
                intent.putExtra(LOCATION_EXTRA_KEY, (String) payload.get("address"));
                intent.putExtra(PLATFORM_EXTRA_KEY, (String) payload.get("platform"));
                intent.putExtra(OS_EXTRA_KEY, (String) payload.get("os"));
                intent.putExtra(CLIENTID_EXTRA_KEY, (String) payload.get("clientId"));
                ApprovalsActivity.this.startActivityForResult(intent, APPROVALS_ACTIVITY_CODE);
                isInApprovalProcess = true;
            } catch (JSONException e) {
                logger.error("Failed to parse payload " + e.getMessage());
            }
        }
    }

    private ArrayList<ApprovedDevice> getApprovedDevicesList (JSONObject devicesJson) throws JSONException {
        ArrayList<ApprovedDevice> approvedDevices = new ArrayList<>();
        if (devicesJson.length() > 0) {
            int i = 0;
            Iterator<String> devicesIterator = devicesJson.keys();
            while (devicesIterator.hasNext()) {
                String id = devicesIterator.next();
                JSONObject device = (JSONObject) devicesJson.get(id);
                approvedDevices.add(new ApprovedDevice(id, device.getString("address"), device.getString("date"), device.getString("platform"), device.getString("os")));
                i++;
            }
        }
        return approvedDevices;
    }

    protected void getApprovedClients() {
        URI adapterPath = URI.create("/adapters/LoginApprovalsAdapter/approvals");
        WLResourceRequest resourceRequest = new WLResourceRequest(adapterPath, WLResourceRequest.GET);

        resourceRequest.send(new WLResponseListener() {
            @Override
            public void onSuccess(WLResponse wlResponse) {
                try {
                    JSONObject devices = new JSONObject(wlResponse.getResponseJSON().toString());
                    ArrayList<ApprovedDevice> approvedDevices = getApprovedDevicesList (devices);
                    if (approvedDevices.size() > 0) {
                        final ApprovedDeviceAdapter adapter = new ApprovedDeviceAdapter(ApprovalsActivity.this, approvedDevices);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                approvalsListView.setAdapter(adapter);
                            }
                        });
                    }
                } catch (JSONException e) {
                    logger.error ("Cannot populate list of approved devices " + e.getMessage());
                }
            }

            @Override
            public void onFailure(WLFailResponse wlFailResponse) {
                logger.debug("ApprovalsActivity" + wlFailResponse.getErrorMsg());
            }
        });

    }

    private void initWLSDK() {
        WLClient.getInstance().registerChallengeHandler(new UserLoginChallengeHandler());
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

}
