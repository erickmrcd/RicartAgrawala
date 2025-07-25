package utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.media.multipart.*;
import org.glassfish.jersey.media.multipart.file.*;

public class RestHandler {
	private final Client multipartClient;   
	private Client defaultClient;
	
	private WebTarget defaultBaseWebTarget;
	private WebTarget multipartBaseWebTarget;

	/**
	 * @param defaultClient
	 * @param defaultBaseWebTarget
	 */
	public RestHandler(String webTarget) {
		this.defaultClient = ClientBuilder.newClient();
		this.defaultBaseWebTarget = defaultClient.target(
				UriBuilder.fromUri(webTarget).build()
				);
		this.multipartClient = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
		this.multipartBaseWebTarget = multipartClient.target(
				UriBuilder.fromUri(webTarget).build()
				);
	}

	/**
	 * @return the defaultBaseWebTarget
	 */
	public WebTarget getDefaultBaseWebTarget() {
		return defaultBaseWebTarget;
	}

	public void callWebService(String uri, RestParameter[] parameters) {
		callWebServiceReturnVoid(uri, parameters);
	}

	public void callWebService(String uri) {
		callWebServiceReturnVoid(uri, null);
	}

	public void callWebService(String uri, RestParameter parameter) {
		callWebServiceReturnVoid(uri, new RestParameter[] { parameter });
	}

	public Response callWebServiceResponse(String uri, RestParameter[] parameters) {
		return callWebServiceReturnResponse(uri, parameters);
	}

	public Response callWebServiceResponse(String uri) {
		return callWebServiceReturnResponse(uri, null);
	}

	public Response callWebServiceResponse(String uri, RestParameter parameter) {
		return callWebServiceReturnResponse(uri, new RestParameter[] { parameter });
	}

	private void callWebServiceReturnVoid(String uri, RestParameter[] parameters) {
		// TODO Auto-generated method stub
		WebTarget currentTarget = defaultBaseWebTarget;

		// Process uri
		String[] uriParts = RestHandler.getUriParts(uri);

		for (String e : uriParts) {
			currentTarget = currentTarget.path(e);
		}

		if (parameters != null) {
			for (RestParameter parameter : parameters) {
				currentTarget = currentTarget.queryParam(parameter.getName(), parameter.getValue());
			}
		}
		currentTarget.request().get();
	}

	private Response callWebServiceReturnResponse(String uri, RestParameter[] parameters) {
		// TODO Auto-generated method stub
		WebTarget currentTarget = defaultBaseWebTarget;

		// Process uri
		String[] uriParts = getUriParts(uri);

		for (String e : uriParts) {
			currentTarget = currentTarget.path(e);
		}

		if (parameters != null) {
			for (RestParameter parameter : parameters) {
				currentTarget = currentTarget.queryParam(parameter.getName(), parameter.getValue());
				// System.out.println(parameter.getValue());
			}
		}

		Response response = currentTarget.request().get();

		return response;
	}

	private static String[] getUriParts(String uri) {
		List<String> uriParts = new ArrayList<>();
		boolean flag = true;
		for (String e : uri.split("/")) {
			if (flag) { // It should start with /, so it will produce an empty item
				flag = false;
				continue;
			}
			uriParts.add(e);
		}

		String[] asArray = new String[uriParts.size()];
		return uriParts.toArray(asArray);
	}

	public <T> T callWebService(String textPlain, String string, RestParameter[] restParameters) {
		return callWebServiceReturnGeneric(textPlain, string, restParameters);
	}

	public <T> T callWebService(String textPlain, String string) {
		return callWebServiceReturnGeneric(textPlain, string, null);
	}

	public <T> T callWebService(String textPlain, String string, RestParameter parameter) {
		return callWebServiceReturnGeneric(textPlain, string, new RestParameter[] { parameter });
	}

	@SuppressWarnings("unchecked")
	private <T> T callWebServiceReturnGeneric(String textPlain, String string, RestParameter[] restParameters) {
		WebTarget currentTarget = defaultBaseWebTarget;

		// Process uri
		String[] uriParts = RestHandler.getUriParts(string);

		for (String e : uriParts) {
			currentTarget = currentTarget.path(e);
		}

		if (restParameters != null) {
			for (RestParameter parameter : restParameters) {
				currentTarget = currentTarget.queryParam(parameter.getName(), parameter.getValue());
			}
		}

		if (MediaType.TEXT_PLAIN == textPlain) {
			return (T) currentTarget.request(textPlain).get(String.class);
		} else {
			return (T) currentTarget.request(textPlain).get(String.class);
		}
	}

	public Response postFile(String uri, String file) {

		FileDataBodyPart filePart = new FileDataBodyPart("file", new File(file));
		FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
		FormDataMultiPart multipart = (FormDataMultiPart)
		formDataMultiPart.field("foo", "bar").bodyPart(filePart);
		WebTarget currentTarget = multipartBaseWebTarget.path(uri);
		Response response = currentTarget.request().post(Entity.entity(multipart, multipart.getMediaType()));

		 try { formDataMultiPart.close(); multipart.close(); } catch (IOException e) {
		  e.printStackTrace(); }
		 
		// Use response object to verify upload success
		return response;
	}
}
