/*
 *    Licensed Materials - Property of IBM
 *    5725-I43 (C) Copyright IBM Corp. 2015, 2016. All Rights Reserved.
 *    US Government Users Restricted Rights - Use, duplication or
 *    disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

package com.github.mfpdev;

import com.ibm.mfp.server.registration.external.model.AuthenticatedUser;
import com.ibm.mfp.server.registration.external.model.ClientData;
import com.ibm.mfp.server.security.external.checks.RegistrationContext;
import com.ibm.mfp.server.security.external.resource.AdapterSecurityContext;
import com.ibm.mfp.server.security.external.resource.ClientSearchCriteria;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.mfp.adapter.api.ConfigurationAPI;
import com.ibm.mfp.adapter.api.OAuthSecurity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import static com.github.mfpdev.Constants.*;

@Api(value = "Login Approvals Adapter Resource")
@Path("/")
public class LoginApprovalsAdapterResource {
	static Logger logger = Logger.getLogger(LoginApprovalsAdapterResource.class.getName());

	// Inject the MFP configuration API:
	@Context
	ConfigurationAPI configApi;

	@Context
	AdapterSecurityContext securityContext;


	private CloseableHttpClient httpclient = HttpClients.createDefault();

	@ApiOperation(value = "Get approved web instances", notes = "Return all the approved app instances")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "A JSON object containing all the approved app instances.") })
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/approvals")
	@OAuthSecurity(scope = "appInstanceApprover")
	public Map<String, String> getApprovedWebClients() {
		setAsApprover();

		ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria().byAttribute(APPROVED_KEY, APPROVED);
		List<ClientData> clientsData = securityContext.findClientRegistrationData(clientSearchCriteria);

		Map<String, String> result = new HashMap<>();

		result.put("approvals", "test");
		return result;
	}

	private void setAsApprover() {
		ClientData clientData = securityContext.getClientRegistrationData();
		clientData.getPublicAttributes().put(APPROVER_KEY, securityContext.getAuthenticatedUser().getId());

		securityContext.storeClientRegistrationData(clientData);
	}


	@ApiOperation(value = "Approve web client", notes = "approve web client login")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Return true after send the refresh event to the client") })
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/approve")
	@OAuthSecurity(scope = "appInstanceApprover")
	public boolean approve(@QueryParam("uuid") String uuid) {
		ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria().byAttribute(WEB_CLIENT_UUID, uuid);
		List<ClientData> clientsData = securityContext.findClientRegistrationData(clientSearchCriteria);
		if (clientsData.size() == 1) {
			clientsData.get(0).getPublicAttributes().put(APPROVED_KEY, APPROVED);
			securityContext.storeClientRegistrationData(clientsData.get(0));
			return sendRefreshEvent(uuid);
		}

		return false;
	}


	private boolean sendRefreshEvent(String uuid) {
		String url = configApi.getPropertyValue(WEB_URL_FOR_NOTIFY);
		url = url + "/refresh/" + uuid;
		HttpGet httpGet = new HttpGet(url);
		try {
			CloseableHttpResponse response = httpclient.execute(httpGet);
			return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
		} catch (IOException e) {
			logger.info("Cannot send refresh event to " + url);
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
	public Map<String, String> getWebClientData(@QueryParam("date") String dateString, @QueryParam("location") String location, @QueryParam("agent") String agent ) {
		WebClientData webClientData = new WebClientData(location, dateString, agent);
		securityContext.getClientRegistrationData().getProtectedAttributes().put(WEB_CLIENT_DATA, webClientData);
		securityContext.storeClientRegistrationData(securityContext.getClientRegistrationData());

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
		//sendRefreshEvent(securityContext.getClientRegistrationData().getClientId());
		return securityContext.getAuthenticatedUser();
	}
}
