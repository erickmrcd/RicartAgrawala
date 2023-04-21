/**
 * 
 */
package main;

import java.net.URI;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import client.ClientRicart;
import client.ClientUID;
import client.ClientUIDGen;
import clientData.CriticalSectionState;
import utils.RestHandler;
import utils.Utils;
import utils.RESTParameter;

/**
 * @author erick
 *
 */
public class Main {

	private static final int NUM_PROCESOS = 3;
	private static final String RESET_ENDPOINT = "/rest/reset";
	private static final Logger LOGGER = Logger.getLogger(ClientRicart.class.getName());
	private static final String SETUP_NUM_CLIENTS_ENDPOINT = "/rest/setup_num";
	private static final String SETUP_REMOTE_CLIENTS_ENDPOINT = "/rest/setup_remote";
	private static final String DEFAULT_WEB_SERVICE_URI_FORMAT = "http://192.168.1.136:8080/RicardAgrawala";

	private static String[] clientsIDs = null;
	private static String[] remoteNodes = null;
	private static int clientsNumberIterations = -1;
	private static int numTotalClients = 0;
	private static String localIpAddress = "";
	private static String webServiceURI = "";
	private static String supervisorWebServiceURI = "";
	private static String supervisorIpAddress = "";
	private static ClientUIDGen guidGenerator = null;
	private static RestHandler restHandler = null;


	public static void main(String[] args) {
		// TODO Auto-generated method stub
				
		ClientRicart[] clients = new ClientRicart[NUM_PROCESOS];
		guidGenerator = new ClientUIDGen("localhost");
		webServiceURI = DEFAULT_WEB_SERVICE_URI_FORMAT;
		restHandler = new RestHandler(webServiceURI);
		int value;
		value = setupServer();
		if (Utils.FAILURE_VALUE == value){
			LOGGER.info(String.format("Server setup failed"));
			System.exit(0);
		}
		value = resetServer();
		if (Utils.FAILURE_VALUE == value){
			LOGGER.info(String.format("Server restart failed"));
			System.exit(0);
		}
		LOGGER.info(String.format("Server was restarted"));
		
		for (int id = 0; id < NUM_PROCESOS; id++) {
			clients[id] = new ClientRicart(new ClientUID("192.168.1.136", id));
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
																							new RESTParameter("numLocal", String.valueOf(3)),
																							new RESTParameter("numTotal", String.valueOf(3))
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
