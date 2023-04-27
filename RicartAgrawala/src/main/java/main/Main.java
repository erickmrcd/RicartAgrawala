package main;

import java.util.Scanner;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import client.ClientRicart;
import client.ClientUIDGen;
import utils.RestHandler;
import utils.Utils;
import utils.RESTParameter;

public class Main {
	private static final int NUM_PROCESOS = 2;

	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	// private static final String DEFAULT_WEB_SERVICE_URI_FORMAT =
	// "http://192.168.1.136:8080/RicartAgrawala";
	private static ClientRicart[] clients = new ClientRicart[NUM_PROCESOS];
	private static String webServiceURI = "";
	private static String supervisorURI = "";
	private static ClientUIDGen guidGenerator = null;
	private static RestHandler restHandler = null;
	private static int numTotalClients = NUM_PROCESOS;
	private static String[] remoteNodes = null;

	// private static String[] remoteNodes = null;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length < 3)
			System.exit(0);

		guidGenerator = new ClientUIDGen(args[0]);
		webServiceURI = "http://" + args[0] + ":8080/RicartAgrawala";
		setSupervisorURI("http://" + args[1] + ":8081/RicartAgrawala");
		restHandler = new RestHandler(webServiceURI);
		String[] numNodo = args[2].split(";");
		if (args.length == 4) {
			if (args[3].contains("#"))
				remoteNodes = args[3].split("#");
			else {
				remoteNodes = new String[1];
				remoteNodes[0] = args[3];
			}
			if (remoteNodes == null) {
				setNumTotalClients(NUM_PROCESOS);
			} else {
				setNumTotalClients(getNumTotalClients() + remoteNodes.length * 2);
			}
		}
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
		int i = 0;
		for (String s : numNodo) {
			clients[i] = new ClientRicart(guidGenerator.nextGUID(), Integer.parseInt(s), webServiceURI,
					getSupervisorURI());
			clients[i].setServerURI(webServiceURI);
			clients[i].setSupervisorURI(getSupervisorURI());
			i++;
		}

		startExecution(clients);
		Scanner scanner = new Scanner(System.in);
		System.out.print("Execution of main finished. Press any key to close...");
		scanner.nextLine();
		scanner.close();
	}

	private static int resetServer() {
		Response response = restHandler.callWebServiceResponse("/rest/reset");
		if (Response.Status.OK.getStatusCode() != response.getStatus()) {
			return Utils.FAILURE_VALUE;
		}
		return Utils.SUCCESS_VALUE;
	}

	private static int setupServer() {
		// Send number of local clients and global clients
		Response response = restHandler.callWebServiceResponse("/rest/setup_num",
				new RESTParameter[] { new RESTParameter("numLocal", String.valueOf(clients.length)),
						new RESTParameter("numTotal", String.valueOf(getNumTotalClients())) });
		// Check response
		if (Response.Status.OK.getStatusCode() != response.getStatus()) {
			LOGGER.warning(String.format("ERROR. Response status HTTP %d", response.getStatus()));
			LOGGER.warning(response.getEntity().toString());
			return Utils.FAILURE_VALUE;
		}

		String ip;
		String[] numNodo = null;
		if (null != remoteNodes) {
			for (String node : remoteNodes) {
				numNodo = node.split(";");
				ip = numNodo[0];
				restHandler.callWebServiceResponse("/rest/setup_remote", new RESTParameter[] {
						new RESTParameter("ip", ip),
						new RESTParameter("cliente1", numNodo[1]),
						new RESTParameter("cliente2", numNodo[2])});
				// Check response
				if (Response.Status.OK.getStatusCode() != response.getStatus()) {
					LOGGER.warning(
							String.format("[Process %s] ERROR. Response status HTTP %d", ip, response.getStatus()));
					LOGGER.warning(String.valueOf(response.getEntity()));
					return Utils.FAILURE_VALUE;
				}

				LOGGER.info(String.valueOf(response.getEntity()));
			}
		}else {
			
		}

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

	public static String getSupervisorURI() {
		return supervisorURI;
	}

	public static void setSupervisorURI(String supervisorURI) {
		Main.supervisorURI = supervisorURI;
	}

	public static int getNumTotalClients() {
		return numTotalClients;
	}

	public static void setNumTotalClients(int numTotalClients) {
		Main.numTotalClients = numTotalClients;
	}
}
