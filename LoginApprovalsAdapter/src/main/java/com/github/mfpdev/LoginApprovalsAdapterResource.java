/*
 *    Licensed Materials - Property of IBM
 *    5725-I43 (C) Copyright IBM Corp. 2015, 2016. All Rights Reserved.
 *    US Government Users Restricted Rights - Use, duplication or
 *    disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
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
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
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

	private static int EARTH_RADIUS = 6371; // Radius of the earth in km

	@ApiOperation(value = "Get approved web instances", notes = "Return all the approved app instances")
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
		for (ClientData data : clientsData) {
			WebClientData webClientData = data.getProtectedAttributes().get("webClientData", WebClientData.class);
			Map<String, String> clientData = new HashMap<>();
			clientData.put(ADDRESS_KEY, webClientData.getAddress());
			clientData.put(DATE_KEY, webClientData.getDate());
			clientData.put(PLATFORM_KEY, webClientData.getPlatform());
			clientData.put(OS_KEY, webClientData.getOs());


			allClients.put(webClientData.getClientId(), clientData);
		}

		return allClients;
	}

	@ApiOperation(value = "Approve web client", notes = "approve web client login")
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
				clientData.getProtectedAttributes().delete(WEB_CLIENT_UUID);
				clientData.getPublicAttributes().delete(APPROVED_KEY);
			}
			securityContext.storeClientRegistrationData(clientData);
			return HttpSenderUtils.sendRefreshEvent(configApi.getPropertyValue(WEB_URL_FOR_NOTIFY), clientId, event);
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

		String platform = securityContext.getClientRegistrationData().getRegistration().getDevice().getPlatform();

		UserAgent userAgent = UserAgent.parseUserAgentString(platform);

		String platformName = userAgent.getBrowser().getName() + " " + userAgent.getBrowserVersion().getVersion();
		String os = userAgent.getOperatingSystem().getName();
		String clientId = securityContext.getClientRegistrationData().getClientId();

		WebClientData webClientData = securityContext.getClientRegistrationData().getProtectedAttributes().get(WEB_CLIENT_DATA, WebClientData.class);
		if (webClientData == null) {
			webClientData = new WebClientData(clientId, dateString, os, platformName, lat, lon, getLocationAddress(lat, lon));
			securityContext.getClientRegistrationData().getProtectedAttributes().put(WEB_CLIENT_DATA, webClientData);
			securityContext.getClientRegistrationData().getPublicAttributes().put(WEB_CLIENT_UUID, this.securityContext.getClientRegistrationData().getClientId());
			securityContext.storeClientRegistrationData(securityContext.getClientRegistrationData());
		}

		Map<String, String> result = new HashMap<>();
		result.put("clientId", securityContext.getClientRegistrationData().getClientId());
		return result;
	}


	@Path("/user")
	@GET
	@OAuthSecurity (scope = "approvedWebUserLogin")
	@Produces("application/json")
	@ApiOperation(value = "Get authenticated user resource",
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

	private void setAsApprover() {
		ClientData clientData = securityContext.getClientRegistrationData();
		clientData.getPublicAttributes().put(APPROVER_KEY, securityContext.getAuthenticatedUser().getId());
		securityContext.storeClientRegistrationData(clientData);
	}
}
