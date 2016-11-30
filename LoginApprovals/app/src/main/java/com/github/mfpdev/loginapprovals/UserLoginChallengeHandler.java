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

import com.worklight.common.Logger;
import com.worklight.wlclient.api.WLClient;
import com.worklight.wlclient.api.challengehandler.SecurityCheckChallengeHandler;

import org.json.JSONException;
import org.json.JSONObject;

class UserLoginChallengeHandler extends SecurityCheckChallengeHandler {

    static final String USER_LOGIN_SECURITY_CHECK = "UserLogin";
    private static final Logger logger = Logger.getInstance(LoginActivity.class.getName());
    private  LoginActivity loginActivity;

    void setLoginActivity(LoginActivity loginActivity) {
        this.loginActivity = loginActivity;
    }

    UserLoginChallengeHandler() {
        super(USER_LOGIN_SECURITY_CHECK);
    }

    @Override
    public void handleChallenge(JSONObject jsonObject) {
        Intent intent = new Intent(WLClient.getInstance().getContext(), LoginActivity.class);
        if (loginActivity == null) {
            WLClient.getInstance().getContext().startActivity(intent);
        } else {
            loginActivity.showMessage (loginActivity.getString(R.string.wrong_credentials));
        }
    }

    @Override
    public void handleSuccess(JSONObject identity) {
        logger.log(identity.toString());
        closeLoginActivity();
    }

    private void closeLoginActivity() {
        if (loginActivity != null) {
            loginActivity.finish();
        }
    }

    @Override
    public void handleFailure(JSONObject errorJson) {
        logger.error(errorJson.toString());
        String error = "";
        try {
            error = errorJson.getString("failure");
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
        }
        loginActivity.showMessage (error.isEmpty() ? errorJson.toString() : error);
    }
}
