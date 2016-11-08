package com.github.mfpdev.loginapprovals;

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
