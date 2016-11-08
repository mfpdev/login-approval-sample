package com.github.mfpdev;

import com.ibm.mfp.security.checks.base.UserAuthenticationSecurityCheckConfig;

import java.util.Properties;

/**
 * Created by ishaib on 08/11/2016.
 */
public class WebUserLoginSecurityCheckConfiguration extends UserAuthenticationSecurityCheckConfig{
    private String pushServerURL;
    private String confidentialClientCredentials;

    public WebUserLoginSecurityCheckConfiguration(Properties properties) {
        super(properties);
        pushServerURL = properties.getProperty("push_server_url", "http://localhost:9080");
        confidentialClientCredentials = properties.getProperty("confidential_client_credentials");
    }

    public String getPushServerURL() {
        return pushServerURL;
    }

    public String getConfidentialClientCredentials() {
        return confidentialClientCredentials;
    }
}
