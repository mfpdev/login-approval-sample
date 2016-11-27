package com.github.mfpdev.loginapprovals;

import android.content.Intent;

import com.worklight.common.Logger;
import com.worklight.wlclient.api.WLClient;
import com.worklight.wlclient.api.challengehandler.SecurityCheckChallengeHandler;

import org.json.JSONException;
import org.json.JSONObject;

public class UserLoginChallengeHandler extends SecurityCheckChallengeHandler {

    public static final String USER_LOGIN_SECURITY_CHECK = "UserLogin";
    private static final Logger logger = Logger.getInstance(LoginActivity.class.getName());
    private  LoginActivity loginActivity;

    public void setLoginActivity(LoginActivity loginActivity) {
        this.loginActivity = loginActivity;
    }

    public UserLoginChallengeHandler() {
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
