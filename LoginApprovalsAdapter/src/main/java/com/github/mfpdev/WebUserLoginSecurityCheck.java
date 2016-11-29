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

    @SecurityCheckReference
    private transient UserLoginSecurityCheck userLoginSecurityCheck;

    @Override
    public WebUserLoginSecurityCheckConfiguration createConfiguration(Properties properties) {
        return new WebUserLoginSecurityCheckConfiguration (properties);
    }

    @Override
    public void authorize(Set<String> scope, Map<String, Object> credentials, HttpServletRequest request, AuthorizationResponse response) {
        if (!userLoginSecurityCheck.isSuccess()) {
            userLoginSecurityCheck.setDone(false);
            registrationContext.getRegisteredPublicAttributes().delete(APPROVED_KEY);
        } else if (userLoginSecurityCheck.isDone()) {
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
                WebClientData webClientData = this.registrationContext.getRegisteredPublicAttributes().get(WEB_CLIENT_DATA, WebClientData.class);

                try {
                    String token = HttpSenderUtils.getOAuthTokenForPush(getMFServerURL(), getConfidentialClientCredentials(), appIdentifier);
                    HttpSenderUtils.sendApprovalPushNotification(getMFServerURL(), webClientData, appIdentifier, deviceId, userId, token);
                } catch (IOException e) {
                    logger.info("Cannot send login approval push notification " + e.getMessage());
                }

            }

        }
        return challenge;
    }

    @Override
    public void introspect(Set<String> scope, IntrospectionResponse response) {
        if (!isApprovedWebClient()) {
            userLoginSecurityCheck.setExpired();
            setState(STATE_EXPIRED);
        } else {
            super.introspect(scope, response);
        }
    }

    private void setApproved () {

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
