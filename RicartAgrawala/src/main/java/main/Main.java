package main;

import java.util.logging.Logger;
import javax.ws.rs.core.Response;

import client.ClientRicart;
import client.ClientUIDGen;
import utils.RestHandler;
import utils.Utils;
import utils.RESTParameter;

public class Main {
	private static final int NUM_PROCESOS = 3;

	private static final String RESET_ENDPOINT = "/rest/reset";
	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	private static final String DEFAULT_WEB_SERVICE_URI_FORMAT = "http://192.168.1.136:8080/RicartAgrawala";
	private static ClientRicart[] clients = new ClientRicart[NUM_PROCESOS];
	private static String webServiceURI = "";
	private static ClientUIDGen guidGenerator = null;
	private static RestHandler restHandler = null;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		guidGenerator = new ClientUIDGen("192.168.1.136");
		webServiceURI = DEFAULT_WEB_SERVICE_URI_FORMAT;
		restHandler = new RestHandler(webServiceURI);
		int value;
		value = setupServer();
		if (Utils.FAILURE_VALUE == value) {
			LOGGER.info(String.format("Server setup failed"));
			System.exit(0);
		}
		value = resetServer();
		if (Utils.FAILURE_VALUE == value) {
			LOGGER.info(String.format("Server restart failed"));
			System.exit(0);
		}
		LOGGER.info(String.format("Server was restarted"));

		for (int id = 0; id < NUM_PROCESOS; id++) {
			clients[id] = new ClientRicart(guidGenerator.nextGUID());
			
		}
		startExecution(clients);
	}
	
	private static int resetServer() {
		Response response = restHandler.callWebServiceResponse(RESET_ENDPOINT);
		if (Response.Status.OK.getStatusCode() != response.getStatus()){
			return Utils.FAILURE_VALUE;
		} 
		return Utils.SUCCESS_VALUE;
	}
	
	private static int setupServer() {
		// Send number of local clients and global clients				
		Response response = restHandler.callWebServiceResponse("/rest/setup_num",
																				  new RESTParameter[] {
																							new RESTParameter("numLocal", String.valueOf(clients.length)),
																							new RESTParameter("numTotal", String.valueOf(clients.length))
																				  });
		// Check response
		if (Response.Status.OK.getStatusCode() != response.getStatus()){
			LOGGER.warning(String.format("ERROR. Response status HTTP %d", response.getStatus()));
			LOGGER.warning(response.getEntity().toString());
			return Utils.FAILURE_VALUE;
		} 
		/*
		String ip, numClients;
		if (null != remoteNodes){
			for (String node : remoteNodes){
				ip = node.split(";")[0];
				numClients = node.split(";")[1];
				restHandler.callWebServiceResponse(SETUP_REMOTE_CLIENTS_ENDPOINT,
															  new RESTParameter[] {
																	  new RESTParameter("ip", ip),
																	  new RESTParameter("numClients", numClients)
															  });
				
				// Check response
				if (Response.Status.OK.getStatusCode() != response.getStatus()){
					return Utils.FAILURE_VALUE;
				} 
			}		
		}
		*/
		return Utils.SUCCESS_VALUE;
	}
	private static void startExecution(ClientRicart[] clients) {

		for (ClientRicart client : clients) {
			if (null == client) {
				System.out.println(String.format("One client did not start correctly"));
				return;
			}
			client.start();
		}
	}
}
