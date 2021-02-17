package com.serverless;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;


public class CheckPasswordHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

	private static final Logger LOG = LogManager.getLogger(CheckPasswordHandler.class);

	@Override
	public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
		LOG.info("Received : {}", input);

/*				String output = "{" +
		String output = "\"commands\":[" +
		"{" +
		"\"type\":\"com.okta.action.update\"," +
		"\"value\":{" +
		"\"credential\":\"UNVERIFIED\"," +
		"}" +
		"}" +
		"]";
		"}";
		ObjectMapper om = new ObjectMapper();
			root = om.readValue(output, Root.class);
			
		
 */
		// First retrieve the credential from the input				
		JSONObject request = new JSONObject(input.get("body").toString());
		LOG.info("Request object : {}", request);
		JSONObject credential = request.getJSONObject("data").
											  getJSONObject("context").
											  getJSONObject("credential");
		
		// We need to compose the DN out of the username element in the request, hard-coding the parent DN
		// for development purposes, in real implementation we might want to retrieve it from a properties
		// file.
		// IMPORTANT: for production grade implementation, make sure you accept an authorization header
		// and perform an authorizer like call to ensure the call is indeed coming from the inline hook.
		String userDN = "mail=" + credential.getString("username") + ",ou=people,dc=example,dc=com";
		String password = credential.getString("password");
		LOG.info("userDN : {}", userDN);
		LOG.info("password : {}", password);
		
		try {
			LDAPConnection connection = new LDAPConnection();
			connection.connect("pds.karmenlei.net", 1389);
			LOG.info("Connected to my directory");
			connection.bind(userDN, password);
			LOG.info("Bind request has been called");
			if (connection.isConnected()) {
				Value value = new Value();
				value.setCredential("VERIFIED");
				Command command = new Command();
				command.setType("com.okta.action.update");
				command.setValue(value);
				Root root = new Root();
				List<Command> commandList = new ArrayList<Command>();
				commandList.add(command);
				root.setCommands(commandList);
				connection.close();
				return ApiGatewayResponse.builder()
				      				.setStatusCode(200)
				      				.setObjectBody(root)
				      				.setHeaders(Collections.singletonMap("Content-Type", "application/json"))
				      				.build();
			} else {
				Value value = new Value();
				value.setCredential("UNVERIFIED");
				Command command = new Command();
				command.setType("com.okta.action.update");
				command.setValue(value);
				Root root = new Root();
				List<Command> commandList = new ArrayList<Command>();
				commandList.add(command);
				root.setCommands(commandList);
				connection.close();
				return ApiGatewayResponse.builder()
				      				.setStatusCode(200)
				      				.setObjectBody(root)
				      				.setHeaders(Collections.singletonMap("Content-Type", "application/json"))
				      				.build();

			}
		} catch (LDAPException e) {
			// TODO Auto-generated catch block
			LOG.error("CheckPasswordHandler error in handleRequest()", e.getDiagnosticMessage());
			Value value = new Value();
			value.setCredential("UNVERIFIED");
			Command command = new Command();
			command.setType("com.okta.action.update");
			command.setValue(value);
			Root root = new Root();
			List<Command> commandList = new ArrayList<Command>();
			commandList.add(command);
			root.setCommands(commandList);
			return ApiGatewayResponse.builder()
			      				.setStatusCode(200)
			      				.setObjectBody(root)
			      				.setHeaders(Collections.singletonMap("Content-Type", "application/json"))
			      				.build();
		}
		
	}
	/*
	public static void main(String[] args) {
		
		CheckPasswordHandler cph = new CheckPasswordHandler();
		
		cph.handleRequest(null, null);

	}
	*/
}
