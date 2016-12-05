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

import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderResult;
import com.google.code.geocoder.model.LatLng;
import com.ibm.mfp.adapter.api.ConfigurationAPI;
import com.ibm.mfp.adapter.api.OAuthSecurity;
import com.ibm.mfp.server.registration.external.model.AuthenticatedUser;
import com.ibm.mfp.server.registration.external.model.ClientData;
import com.ibm.mfp.server.security.external.resource.AdapterSecurityContext;
import com.ibm.mfp.server.security.external.resource.ClientSearchCriteria;
import eu.bitwalker.useragentutils.UserAgent;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.github.mfpdev.Constants.*;

@Api(value = "Login Approvals Adapter Resource")
@Path("/")
public class LoginApprovalsAdapterResource {
	public static final String REVOKE_EVENT = "revoke";
	public static final String APPROVE_EVENT = "approve";
	static Logger logger = Logger.getLogger(LoginApprovalsAdapterResource.class.getName());
	// Inject the MFP configuration API:
	@Context
	ConfigurationAPI configApi;

	@Context
	AdapterSecurityContext securityContext;

	@ApiOperation(value = "Get approved web instances", notes = "Return all the approved web instances")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "A JSON object containing all the approved app instances.") })
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/approvals")
	@OAuthSecurity(scope = "appInstanceApprover")
	public Map<String, Map<String,String>> getApprovedWebClients() {
		setAsApprover();

		ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria().byAttribute(APPROVED_KEY, APPROVED).byUser(USER_LOGIN_SC_NAME, securityContext.getAuthenticatedUser().getId());
		List<ClientData> clientsData = securityContext.findClientRegistrationData(clientSearchCriteria);

		Map<String, Map<String,String>> allClients = new HashMap<>();
		for (ClientData clientData : clientsData) {

			WebClientData webClientData = clientData.getPublicAttributes().get("webClientData", WebClientData.class);

			Map<String, String> clientDataMap = new HashMap<>();
			clientDataMap.put(ADDRESS_KEY, webClientData.getAddress());
			clientDataMap.put(DATE_KEY, webClientData.getDate());
			clientDataMap.put(PLATFORM_KEY, webClientData.getPlatform());
			clientDataMap.put(OS_KEY, webClientData.getOs());

			allClients.put(webClientData.getClientId(), clientDataMap);
		}

		return allClients;
	}

	@ApiOperation(value = "Approve web client", notes = "approve or deny a new web client login")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Return true after send the refresh event to the client") })
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/approve")
	@OAuthSecurity(scope = "appInstanceApprover")
	public boolean approve(@QueryParam("clientId") String clientId, @QueryParam("approve") Boolean approve, @QueryParam("longitude") Double longitude, @QueryParam("latitude") Double latitude) {
		ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria().byAttribute(WEB_CLIENT_UUID, clientId);
		List<ClientData> clientsData = securityContext.findClientRegistrationData(clientSearchCriteria);

		if (clientsData.size() == 1) {
			ClientData clientData = clientsData.get(0);
			String event = REVOKE_EVENT;

			if (approve) {
				clientData.getPublicAttributes().put(APPROVED_KEY, APPROVED);
				event = APPROVE_EVENT;
			} else {
				clientData.getPublicAttributes().delete(USER_LOGIN_DONE);
				//clientData.getPublicAttributes().delete(WEB_CLIENT_UUID);
				clientData.getPublicAttributes().delete(APPROVED_KEY);
			}
			securityContext.storeClientRegistrationData(clientData);
			return HttpSenderUtils.sendRefreshEvent(configApi.getPropertyValue(NODE_SERVER_URL), clientId, event);
		}
		return false;
	}

	@Path("/webClientData")
	@POST
	@Produces("application/json")
	@ApiOperation(value = "Get web client data",
			notes = "Get web client data and send back client id",
			httpMethod = "POST"
	)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Return the authenticated user",
					response = Boolean.class)
	})
	public Map<String, String> getWebClientData(@QueryParam("date") String dateString, @QueryParam("latitude") String latitude, @QueryParam("longitude") String longitude) {
		double lat = Double.valueOf(latitude);
		double lon = Double.valueOf(longitude);

		final ClientData clientRegistrationData = securityContext.getClientRegistrationData();
		String platform = clientRegistrationData.getRegistration().getDevice().getPlatform();

		UserAgent userAgent = UserAgent.parseUserAgentString(platform);

		String platformName = userAgent.getBrowser().getName() + " " + userAgent.getBrowserVersion().getVersion();
		String os = userAgent.getOperatingSystem().getName();
		String clientId = clientRegistrationData.getClientId();

		WebClientData webClientData = clientRegistrationData.getPublicAttributes().get(WEB_CLIENT_DATA, WebClientData.class);
		if (webClientData == null) {
			String webClientID = this.securityContext.getClientRegistrationData().getClientId();
			webClientData = new WebClientData(clientId, dateString, os, platformName, lat, lon, getLocationAddress(lat, lon));
			clientRegistrationData.getPublicAttributes().put(WEB_CLIENT_DATA, webClientData);
			clientRegistrationData.getPublicAttributes().put(WEB_CLIENT_UUID, webClientID);
			securityContext.storeClientRegistrationData(clientRegistrationData);
		}

		Map<String, String> result = new HashMap<>();
		result.put("clientId", clientRegistrationData.getClientId());
		return result;
	}

	@Path("/user")
	@GET
	@OAuthSecurity (scope = "approvedWebUserLogin")
	@Produces("application/json")
	@ApiOperation(value = "Get authenticated user - the protected resource used for this sample",
			notes = "Sample resource which protected with webLogin security check",
			httpMethod = "GET",
			response = Void.class
	)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Return the authenticated user",
					response = Boolean.class)
	})
	public AuthenticatedUser getUser() {
		return securityContext.getAuthenticatedUser();
	}

	/**
	 * Get address name from coordinates
	 * @param latitude
	 * @param longitude
	 * @return
	 */
	private String getLocationAddress (double latitude, double longitude) {
		final Geocoder geocoder = new Geocoder();
		GeocoderRequest geocoderRequest = new GeocoderRequestBuilder().setLocation(new LatLng(BigDecimal.valueOf(latitude),BigDecimal.valueOf(longitude))).getGeocoderRequest();
		GeocodeResponse geocoderResponse = geocoder.geocode(geocoderRequest);
		List<GeocoderResult> results = geocoderResponse.getResults();
		if (results.size() > 0) {
			return results.get(0).getFormattedAddress();
		}
		return latitude + ":" + latitude;
	}

	/**
	 * Set client as aprrover
	 */
	private void setAsApprover() {
		ClientData clientData = securityContext.getClientRegistrationData();
		clientData.getPublicAttributes().put(APPROVER_KEY, securityContext.getAuthenticatedUser().getId());
		securityContext.storeClientRegistrationData(clientData);
	}
}
