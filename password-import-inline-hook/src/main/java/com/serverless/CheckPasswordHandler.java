package com.serverless;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;


public class CheckPasswordHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

	private static final Logger LOG = LogManager.getLogger(CheckPasswordHandler.class);

	@Override
	public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
		LOG.info("received: {}", input);

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
					Value value = new Value();
					value.setCredential("VERIFIED");
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
	/*
	public static void main(String[] args) {
		
		CheckPasswordHandler cph = new CheckPasswordHandler();
		
		cph.handleRequest(null, null);

	}
	*/
}
