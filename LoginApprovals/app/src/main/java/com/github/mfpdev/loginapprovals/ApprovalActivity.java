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
import android.view.View;
import android.widget.TextView;

import com.worklight.common.Logger;
import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLResourceRequest;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.WLResponseListener;

import java.net.URI;

public class ApprovalActivity extends AppCompatActivity {
    private static final Logger logger = Logger.getInstance(LoginActivity.class.getName());

    private String clientId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approval);

        //Get the clientID
        this.clientId = (String) getIntent().getExtras().get(ApprovalsActivity.CLIENTID_EXTRA_KEY);
        setLoginApprovalLabels();
    }

    public void onClickYes (View view) {
        approveWebUser(true);
    }

    public void onClickNo (View view) {
        approveWebUser(false);
    }

    private void approveWebUser(boolean approve) {
        URI adapterPath = URI.create("/adapters/LoginApprovalsAdapter/approve");
        WLResourceRequest resourceRequest = new WLResourceRequest(adapterPath, WLResourceRequest.POST);
        resourceRequest.setQueryParameter("clientId", clientId);
        resourceRequest.setQueryParameter("approve", Boolean.toString(approve));

        resourceRequest.send(new WLResponseListener() {
            @Override
            public void onSuccess(WLResponse wlResponse) {
                setResult(RESULT_OK);
                ApprovalActivity.this.finish();
            }

            @Override
            public void onFailure(WLFailResponse wlFailResponse) {
                setResult(RESULT_CANCELED);
                ApprovalActivity.this.finish();
                logger.error(wlFailResponse.getErrorMsg());
            }
        });
    }

    private void setLoginApprovalLabels() {
        Intent intent = getIntent();
        String date = (String) intent.getExtras().get(ApprovalsActivity.DATE_EXTRA_KEY);
        String location = (String) intent.getExtras().get(ApprovalsActivity.LOCATION_EXTRA_KEY);
        String platform = (String) intent.getExtras().get(ApprovalsActivity.PLATFORM_EXTRA_KEY);
        String os = (String) intent.getExtras().get(ApprovalsActivity.OS_EXTRA_KEY);


        TextView dateText = (TextView) findViewById(R.id.dateLabel);
        TextView locationText = (TextView) findViewById(R.id.locationLabel);
        TextView platformText = (TextView) findViewById(R.id.platformLabel);

        location = "Near " + location;
        String platformAndOS = platform + " for " + os;

        dateText.setText(date);
        locationText.setText(location);
        platformText.setText(platformAndOS);
    }
}
