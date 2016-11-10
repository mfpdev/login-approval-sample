package com.github.mfpdev;

import com.ibm.json.java.JSONObject;
import com.ibm.mfp.security.checks.base.UserAuthenticationSecurityCheck;
import com.ibm.mfp.server.registration.external.model.AuthenticatedUser;
import com.ibm.mfp.server.registration.external.model.ClientData;
import com.ibm.mfp.server.security.external.checks.AuthorizationResponse;
import com.ibm.mfp.server.security.external.checks.SecurityCheckReference;
import com.ibm.mfp.server.security.external.resource.ClientSearchCriteria;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

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

    private transient CloseableHttpClient httpclient = HttpClients.createDefault();

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
            userLoginSecurityCheck.logout();
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
                    String token = getOAuthTokenForPush (appIdentifier);
                    sendApprovalPushNotification (webClientData, appIdentifier, deviceId, userId, token);
                } catch (IOException e) {
                    logger.info("Cannot send login approval push notification " + e.getMessage());
                }

            }

        }
        return challenge;
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

    private String getOAuthTokenForPush (String appId) throws IOException {
        String token = null;
        String url = getMFServerURL() + "/mfp/api/az/v1/token";
        HttpPost httpPost = new HttpPost(url);

        Header authorizationHeader = new BasicHeader("Authorization", "Basic " + Base64.encode(getConfidentialClientCredentials().getBytes()));
        httpPost.setHeader(authorizationHeader);

        List<NameValuePair> params = new ArrayList<>(2);
        params.add(new BasicNameValuePair("grant_type", "client_credentials"));
        params.add(new BasicNameValuePair("scope", "messages.write and push.application." + appId));
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        CloseableHttpResponse response = httpclient.execute(httpPost);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            JSONObject tokenInfo = JSONObject.parse(response.getEntity().getContent());
            token = (String) tokenInfo.get("access_token");
        }
        HttpClientUtils.closeQuietly(response);
        return token;
    }

    private int sendApprovalPushNotification (WebClientData webClientData, String appIdentifier, String deviceId, String userId, String accessToken) throws IOException {
        String url = getMFServerURL() + "/imfpush/v1/apps/" + appIdentifier + "/messages";
        HttpPost httpPost = new HttpPost(url);

        JSONObject payloadGCM = new JSONObject();
        payloadGCM.put(PLATFORM_KEY, webClientData.getPlatform());
        payloadGCM.put(OS_KEY, webClientData.getOs());
        payloadGCM.put(ADDRESS_KEY, webClientData.getAddress());
        payloadGCM.put(DATE_KEY, webClientData.getDate());
        payloadGCM.put(CLIENT_ID_KEY, webClientData.getClientId());

        String payload = "{\n" +
                "  \"message\": {\n" +
                "    \"alert\": \"Did you just login near " + webClientData.getAddress() + "?\"\n" +
                "  },\n" +
                "  \"settings\": {\n" +
                "   \"gcm\": {\n" +
                "       \"payload\":"  + payloadGCM.toString() + "\n" +
                "   },\n" +
                "  },\n" +
                "  \"notificationType\":1,\n" +
                "   \"target\" : {\n" +
                "     \"platforms\" : [\"G\"],\n" +
                "     \"deviceIds\" : [\""+ deviceId+ "\"],\n" +
                "     \"userIds\" : [\""+ userId+ "\"]\n" +
                "   }\n" +
                "}";
        HttpEntity entity = new StringEntity(payload);
        Header contentTypeHeader = new BasicHeader("Content-Type", "application/json");
        Header authorizationHeader = new BasicHeader("Authorization", "Bearer " + accessToken);
        httpPost.setHeader(authorizationHeader);
        httpPost.setHeader(contentTypeHeader);
        httpPost.setEntity(entity);
        CloseableHttpResponse response = httpclient.execute(httpPost);
        int status = response.getStatusLine().getStatusCode();
        HttpClientUtils.closeQuietly(response);
        return status;
    }
}
