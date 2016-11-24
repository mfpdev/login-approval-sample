package com.github.mfpdev;

import com.ibm.json.java.JSONObject;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.github.mfpdev.Constants.*;
import static com.github.mfpdev.Constants.CLIENT_ID_KEY;

/**
 * Created by ishaib on 22/11/2016.
 */
public class HttpSenderUtils {

    private static CloseableHttpClient httpclient = HttpClients.createDefault();

    static Logger logger = Logger.getLogger(LoginApprovalsAdapterResource.class.getName());

     static boolean sendRefreshEvent(String nodeServerURL, String uuid, String event) {
         boolean status = false;
         nodeServerURL = nodeServerURL + "/refresh/" + uuid + "/" + event;
         HttpGet httpGet = new HttpGet(nodeServerURL);
         CloseableHttpResponse response = null;
         try {
             response = httpclient.execute(httpGet);
             status = response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
         } catch (IOException e) {
             logger.info("Cannot send refresh event to " + nodeServerURL);
         } finally {
             if (response != null) {
                 HttpClientUtils.closeQuietly(response);
             }
         }
         return status;
     }

     static String getOAuthTokenForPush (String serverURL, String credentials, String appId) throws IOException {
        String token = null;
        String url = serverURL + "/mfp/api/az/v1/token";
        HttpPost httpPost = new HttpPost(url);

        Header authorizationHeader = new BasicHeader("Authorization", "Basic " + Base64.encode(credentials.getBytes()));
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

    static int sendApprovalPushNotification (String serverURL, WebClientData webClientData, String appIdentifier, String deviceId, String userId, String accessToken) throws IOException {
        String url = serverURL + "/imfpush/v1/apps/" + appIdentifier + "/messages";
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
