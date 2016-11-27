package com.github.mfpdev;

import com.ibm.mfp.security.checks.base.UserAuthenticationSecurityCheck;
import com.ibm.mfp.server.registration.external.model.AuthenticatedUser;
import com.ibm.mfp.server.registration.external.model.ClientData;
import com.ibm.mfp.server.security.external.checks.AuthorizationResponse;
import com.ibm.mfp.server.security.external.checks.IntrospectionResponse;
import com.ibm.mfp.server.security.external.checks.SecurityCheckReference;
import com.ibm.mfp.server.security.external.resource.ClientSearchCriteria;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static com.github.mfpdev.Constants.*;

public class WebUserLoginSecurityCheck extends UserAuthenticationSecurityCheck {
    static Logger logger = Logger.getLogger(WebUserLoginSecurityCheck.class.getName());

    private transient boolean denied = false;
    private transient boolean isPushSent = false;

    @SecurityCheckReference
    private transient UserLoginSecurityCheck userLoginSecurityCheck;

    @Override
    public WebUserLoginSecurityCheckConfiguration createConfiguration(Properties properties) {
        return new WebUserLoginSecurityCheckConfiguration (properties);
    }

    @Override
    public void authorize(Set<String> scope, Map<String, Object> credentials, HttpServletRequest request, AuthorizationResponse response) {
        if (userLoginSecurityCheck.isDone()) {
            super.authorize(scope, credentials, request, response);
        }
    }

    @Override
    protected AuthenticatedUser createUser() {
        return userLoginSecurityCheck.getUser();
    }

    protected boolean validateCredentials(Map<String, Object> credentials) {
        boolean approved = isApprovedWebClient();
        if (!approved) {
            denied = true;
            userLoginSecurityCheck.setExpired();
            userLoginSecurityCheck.setDone(false);
        }
        return approved;
    }

    protected Map<String, Object> createChallenge() {
        Map <String, Object> challenge = new HashMap<>();
        if (!isApprovedWebClient() && !denied) {
            challenge.put(WAITING_FOR_APPROVAL_KEY, true);

            ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria().byAttribute(APPROVER_KEY, userLoginSecurityCheck.getUser().getId());
            List<ClientData> clientsData = registrationContext.findClientRegistrationData(clientSearchCriteria);


            if (clientsData.size() > 0) {
                //Sending the approval push notification
                String appIdentifier = clientsData.get(0).getRegistration().getApplication().getId();
                String deviceId = clientsData.get(0).getRegistration().getDevice().getId();
                String userId = clientsData.get(0).getUsers().get(userLoginSecurityCheck.getSecurityCheckName()).getId();
                WebClientData webClientData = this.registrationContext.getRegisteredProtectedAttributes().get(WEB_CLIENT_DATA, WebClientData.class);

                try {
                    if(!isPushSent) {
                        String token = HttpSenderUtils.getOAuthTokenForPush(getMFServerURL(), getConfidentialClientCredentials(), appIdentifier);
                        HttpSenderUtils.sendApprovalPushNotification(getMFServerURL(), webClientData, appIdentifier, deviceId, userId, token);
                        isPushSent = true;
                    }
                } catch (IOException e) {
                    logger.info("Cannot send login approval push notification " + e.getMessage());
                }

            }

        }
        return challenge;
    }

    @Override
    public void introspect(Set<String> scope, IntrospectionResponse response) {
        if (registrationContext.getRegisteredPublicAttributes().get(APPROVED_KEY) == null) {
            userLoginSecurityCheck.setExpired();
            setState(STATE_EXPIRED);
        } else {
            super.introspect(scope, response);
        }
    }

    private boolean isApprovedWebClient() {
        String approved = registrationContext.getRegisteredPublicAttributes().get(APPROVED_KEY);
        return approved != null && approved.equals(APPROVED);
    }

    private String getMFServerURL() {
        return ((WebUserLoginSecurityCheckConfiguration)this.config).getPushServerURL();
    }

    private String getConfidentialClientCredentials() {
        return ((WebUserLoginSecurityCheckConfiguration)this.config).getConfidentialClientCredentials();
    }

}
