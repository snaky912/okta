package com.serverless;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class ProprietaryAuthentication implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

	private static final Logger LOG = LogManager.getLogger(ProprietaryAuthentication.class);

	@Override
	public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
		
		/*
		 * The request body should contain these attributes in JSON format:
		 * {
		 * 	"firstname" : "John",
		 * 	"lastname"  : "Doe",
		 *  "email"     : "john.doe@mallinator.com",
		 *  "mobile"    : "4152290000",
		 *  "username"  : "john.doe@mallinator.com",
		 *  "password"  : "12345"
		 *  }
		 */
		
		// Here I always return a successful authentication response
		// but reference the HTTPS POST call below to implement the call
		// to an authentication mechanism like LDAP or web service.
		
		LOG.info("Received: {}", input);
		// create the unique value as a token value
		String uuid = UUID.randomUUID().toString();
		uuid = uuid.replace("-", "");
		Value value = new Value();
		value.setCredential("VERIFIED");
		value.setToken(uuid);
		Command command = new Command();
		command.setType("com.anycompany.action.authentication");
		command.setValue(value);
		Root root = new Root();
		List<Command> commandList = new ArrayList<Command>();
		commandList.add(command);
		root.setCommands(commandList);

		// In order to update the user, first retrieve the userid from login
		JSONObject requestObj = new JSONObject(input.get("body").toString());
		LOG.info("Request body: {}", requestObj);
		String username = requestObj.getString("username");
		String output, userOutput = "";

		try {
			URL url = new URL ("https://walgreensidppoc-admin.okta.com/api/v1/users/" + username);
			HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
			con.setRequestProperty("Content-Type", "application/json; utf-8");
			con.setRequestProperty("Accept", "application/json");
			con.setRequestProperty("Authorization", "SSWS <API key>");

			if (con.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ con.getResponseCode() + con.getResponseMessage());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(
				(con.getInputStream())));

			while ((output = br.readLine()) != null) {
				LOG.info("Response: " + output);
				userOutput = output;
			}

			con.disconnect();
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// After getting the user object, get the userid for update
		JSONObject userObj = new JSONObject(userOutput);
		String userid = userObj.getString("id");

		try {
			URL url = new URL ("https://walgreensidppoc-admin.okta.com/api/v1/users/" + userid);
			HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Accept", "application/json");
			con.setRequestProperty("Authorization", "SSWS <API key>");
			con.setDoOutput(true);
			
			/*
			 * Sample request
			 * {
				  "profile": {
				  "customToken": "67194a36f47d421c8efbcf18a0c7b29e",
			   }
			 */
			JSONObject jsonObj = new JSONObject();
			HashMap<String, String> hmap = new HashMap<String, String>();
			
			
			hmap = new HashMap<String, String>();
			hmap.put("walgreens_tk", uuid);
			jsonObj.put("profile", hmap);
			LOG.info("Update request: {}" + jsonObj.toString());
						
			try(OutputStream os = con.getOutputStream()) {
			    byte[] inputBa = jsonObj.toString().getBytes("utf-8");
			    os.write(inputBa, 0, inputBa.length);			
			}
			
			try(BufferedReader br = new BufferedReader(
					new InputStreamReader(con.getInputStream(), "utf-8"))) {
					    StringBuilder response = new StringBuilder();
					    String responseLine = null;
					    while ((responseLine = br.readLine()) != null) {
					        response.append(responseLine.trim());
					    }
					    LOG.info("Response from /users endpoint: {}" + response.toString());
					}
			con.disconnect();
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Need more headers to return in the response to handle CORS
		
		Map<String, String> m = new HashMap<String, String>();
		m.put("Content-Type", "application/json");
		m.put("Access-Control-Allow-Origin", "*");
		m.put("Access-Control-Allow-Methods", "*");
		m.put("Access-Control-Allow-Headers", "*");
		
        
		return ApiGatewayResponse.builder()
		      				.setStatusCode(200)
		      				.setObjectBody(root)
		      				.setHeaders(m)
		      				.build();
	}
}
