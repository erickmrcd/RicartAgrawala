package utils;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

public class RestHandler {

	private Client defaultClient;
	private WebTarget defaultBaseWebTarget;

	/**
	 * @param defaultClient
	 * @param defaultBaseWebTarget
	 */
	public RestHandler(String webTarget) {
		this.defaultClient = ClientBuilder.newClient();
		this.defaultBaseWebTarget = defaultClient.target((UriBuilder.fromUri(webTarget)).build());
	}

	/**
	 * @return the defaultBaseWebTarget
	 */
	public WebTarget getDefaultBaseWebTarget() {
		return defaultBaseWebTarget;
	}

	public void callService(String uri, RESTParameter[] parameters) {
		callWebServiceReturnVoid(uri, parameters);
	}


	public void callService(String uri) {
		callWebServiceReturnVoid(uri, null);
	}

	public void callService(String uri, RESTParameter parameter) {
		callWebServiceReturnVoid(uri, new RESTParameter[] {parameter});
	}
	
	private void callWebServiceReturnVoid(String uri, RESTParameter[] parameters) {
		// TODO Auto-generated method stub
WebTarget currentTarget = defaultBaseWebTarget;
		
		// Process uri
		String[] uriParts = RestHandler.getUriParts(uri);
		
		for (String e : uriParts) {
			currentTarget = currentTarget.path(e);
		}
		
		if (parameters != null)
		{
			for (RESTParameter parameter : parameters) {
				currentTarget = currentTarget.queryParam(parameter.getName(), parameter.getValue());
			}
		}
		currentTarget.request().get();
	}

	private static String[] getUriParts(String uri) {
		List<String> uriParts = new ArrayList<>(); 
		boolean flag = true;
		for (String e : uri.split("/")) {
			if (flag) {  // It should start with /, so it will produce an empty item
				flag = false;
				continue;
			}
			uriParts.add(e);
		}
		
		String[] asArray = new String[uriParts.size()];
		return uriParts.toArray(asArray);
	}

}
