package com.github.mfpdev.loginapprovals;
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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.worklight.common.Logger;
import com.worklight.wlclient.api.WLClient;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private static final Logger logger = Logger.getInstance(LoginActivity.class.getName());

    private UserLoginChallengeHandler userLoginChallengeHandler;
    private EditText userNameEditText;
    private EditText passwordEditText;
    private TextView errorMessageTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.userNameEditText = (EditText)findViewById(R.id.username);
        this.passwordEditText = (EditText)findViewById(R.id.password);
        this.errorMessageTextView = (TextView)findViewById(R.id.errorMessage);

        this.userLoginChallengeHandler = (UserLoginChallengeHandler) WLClient.getInstance().getSecurityCheckChallengeHandler(UserLoginChallengeHandler.USER_LOGIN_SECURITY_CHECK);
        this.userLoginChallengeHandler.setLoginActivity(this);
    }

    public void loginClicked (View view) {
        String username = userNameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (!username.trim().isEmpty() && !password.trim().isEmpty()) {
            try {
            JSONObject credentials = new JSONObject();
                credentials.put("username", username);
                credentials.put("password", password);
                this.userLoginChallengeHandler.submitChallengeAnswer(credentials);
            } catch (JSONException e) {
                logger.error(e.getMessage(),e);
            }
        }
    }

    public void showMessage(final String error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                errorMessageTextView.setText(error);
            }
        });
    }

}
