package main;

import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import client.RicarClient;
import client.IdentifierGenerator;
import utils.RestHandler;
import utils.ResponseCodes;
import utils.RestParameter;

public class Main {
	private static final int NUM_PROCESOS = 2;

	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	private static RicarClient[] clients = new RicarClient[NUM_PROCESOS];
	private static String webServiceURI = "";
	private static String supervisorURI = "";
	private static IdentifierGenerator guidGenerator = null;
	private static RestHandler restHandler = null;
	private static int numTotalClients = NUM_PROCESOS;
	private static String[] remoteNodes = null;

	// private static String[] remoteNodes = null;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length < 3) {
			System.out.println("No se han añadido los correspondientes argumentos (minimo 3)");
			System.out.println("argumento 1: IP LOCAL");
			System.out.println("argumento 2: IP Supervisor");
			System.out.println("argumento 3: Identificador de procesos separados por ; je(0;1;...)");
			System.out.println("argumento 4: IPs remotas y sus correspondientes identificadores (192.168.1.123;0;1#192.168.1.321;2;3)");
			System.out.println("argumento 5: Numero de la maquina (0, 1 o 2)");
			System.exit(0);
		}
		
		webServiceURI = "http://" + args[0] + ":8080/RicartAgrawala";
		setSupervisorURI("http://" + args[1] + ":8080/RicartAgrawala");
		restHandler = new RestHandler(webServiceURI);
		String[] numNodo = args[2].split(";");
		if (args.length >= 4) {
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
		if(args.length == 5) {
			guidGenerator = new IdentifierGenerator(args[0],Integer.parseInt(args[4])*2);
		}else {
			guidGenerator = new IdentifierGenerator(args[0]);
		}
		int value;
		value = setupServer();
		if (ResponseCodes.FAILURE_VALUE == value) {
			LOGGER.info(String.format("Server setup failed"));
			System.exit(0);
		}
		value = resetServer();
		if (ResponseCodes.FAILURE_VALUE == value) {
			LOGGER.info(String.format("Server restart failed"));
			System.exit(0);
		}
		LOGGER.info(String.format("Server was restarted"));
		int i = 0;
		for (String s : numNodo) {
			clients[i] = new RicarClient(guidGenerator.nextIdentifier(), Integer.parseInt(s), webServiceURI,
					getSupervisorURI());
			clients[i].setServerURI(webServiceURI);
			clients[i].setSupervisorURI(getSupervisorURI());
			i++;
		}

		startExecution(clients);

	}

	private static int resetServer() {
		Response response = restHandler.callWebServiceResponse("/rest/reset");
		if (Response.Status.OK.getStatusCode() != response.getStatus()) {
			return ResponseCodes.FAILURE_VALUE;
		}
		return ResponseCodes.SUCCESS_VALUE;
	}

	private static int setupServer() {
		// Send number of local clients and global clients
		Response response = restHandler.callWebServiceResponse("/rest/setup_local",
				new RestParameter[] { new RestParameter("numLocal", String.valueOf(clients.length)),
						new RestParameter("numTotal", String.valueOf(getNumTotalClients())) });
		// Check response
		if (Response.Status.OK.getStatusCode() != response.getStatus()) {
			LOGGER.warning(String.format("ERROR. Response status HTTP %d", response.getStatus()));
			LOGGER.warning(response.getEntity().toString());
			return ResponseCodes.FAILURE_VALUE;
		}

		String ip;
		String[] numNodo = null;
		if (null != remoteNodes) {
			for (String node : remoteNodes) {
				numNodo = node.split(";");
				ip = numNodo[0];
				restHandler.callWebServiceResponse("/rest/setup_remote", new RestParameter[] {
						new RestParameter("ip", ip),
						new RestParameter("cliente1", numNodo[1]),
						new RestParameter("cliente2", numNodo[2])});
				// Check response
				if (Response.Status.OK.getStatusCode() != response.getStatus()) {
					LOGGER.warning(
							String.format("[Process %s] ERROR. Response status HTTP %d", ip, response.getStatus()));
					LOGGER.warning(String.valueOf(response.getEntity()));
					return ResponseCodes.FAILURE_VALUE;
				}

				LOGGER.info(String.valueOf(response.getEntity()));
			}
		}else {
			
		}

		return ResponseCodes.SUCCESS_VALUE;
	}

	private static void startExecution(RicarClient[] clients) {

		for (RicarClient client : clients) {
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
