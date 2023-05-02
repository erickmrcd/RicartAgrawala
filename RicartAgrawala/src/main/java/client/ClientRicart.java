package client;

import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;

import logging.Logging;
import utils.RESTParameter;
import utils.RestHandler;
import utils.Utils;

public class ClientRicart extends Thread {

	private static final int DEFAULT_NUMBER_ITERATIONS = 100;
	private static final int OPERATIONS_MIN_TIME = 300;
	private static final int OPERATIONS_MAX_TIME = 500;
	private static final int CRITICAL_SECTION_MIN_TIME = 100;
	private static final int CRITICAL_SECTION_MAX_TIME = 300;
	
	private static final Logger LOGGER = Logger.getLogger(ClientRicart.class.getName());
	public Semaphore s1;

	private Random random;

	private ClientUID clientID;
	private String serverURI;
	private String supervisorURI;
	private RestHandler restHandler;
	private RestHandler supervisorHandler;
	private int numeroNodo;
	private StringBuffer stringLog = new StringBuffer();
	
	/**
	 * 
	 * @param clientID
	 * @param numeroNodo
	 * @param serverURI
	 * @param supervisorURI
	 */
	public ClientRicart(ClientUID clientID, int numeroNodo, String serverURI,String supervisorURI) {
		this.clientID = clientID;
		this.random = new Random();
		this.restHandler = new RestHandler(serverURI);
		this.supervisorHandler = new RestHandler(supervisorURI);
		this.numeroNodo = numeroNodo;
		
	}


	/**
	 * @return the clientID
	 */
	public int getClientID() {
		return clientID.getClientID();
	}

	public void run() {
		int value = -1;
		if (null == this.restHandler) {
			this.restHandler = new RestHandler(this.getServerURI());
		}
		value = registerClient();
		if (Utils.FAILURE_VALUE == value){
			LOGGER.warning(String.format("Client '%d' registery failed", this.getClientID()));
			System.exit(0);
		}
		value = waitSynchronize();
		if (Utils.FAILURE_VALUE == value){
			LOGGER.warning(String.format("Client '%d' synchronization failed", this.getClientID()));
			System.exit(0);
		}
		value = clientsStart();
		if (Utils.FAILURE_VALUE == value){
			LOGGER.warning(String.format("Client '%d' start failed", this.getClientID()));
			System.exit(0);
		}
		for (int i = 0; i < DEFAULT_NUMBER_ITERATIONS; i++) {
			simulateOperations();
			value = enterCriticalSection();
			if (Utils.FAILURE_VALUE == value){
				LOGGER.warning(String.format("Client '%d' entrance to the CS failed", this.getClientID()));
				System.exit(0);
			}
			writeLog("Enter");
			criticalSection();
			writeLog("exit");
			value = exitCriticalSection();
			if (Utils.FAILURE_VALUE == value){
				LOGGER.warning(String.format("Client '%d' entrance from the CS failed", this.getClientID()));
				System.exit(0);
			}

		}
		
		Logging.doLog(this.clientID, stringLog.toString());
		// Send client�s log to supervisor
		value = sendLog(clientID.toUniqueFilename("log"));
		if (Utils.FAILURE_VALUE == value){
			LOGGER.warning(String.format("Error ocurred while sending logs in Client %d", this.getClientID()));
			System.exit(0);
		}
		waitSynchronize();
	}
	
	private int clientsStart() {
		Response response = restHandler.callWebServiceResponse("/rest/start");

		// Check response
		if (Response.Status.OK.getStatusCode() != response.getStatus()) {
			LOGGER.warning(String.format("[Process %d] ERROR. Response status HTTP %d", clientID.getClientID(), response.getStatus()));
			LOGGER.warning(String.valueOf(response.getEntity()));
			return Utils.FAILURE_VALUE;
		}
		LOGGER.info(String.valueOf(response.getEntity()));
		return Utils.SUCCESS_VALUE;
	}
	
	private int waitSynchronize() {
		System.out.printf("%d me quedo a la espera de que inicie el supervisor\n", clientID.getClientID());
		Response response = restHandler.callWebServiceResponse("/rest/wait_synchronize",
													new RESTParameter("id", clientID.toUniqueFilename()));
		
		// Check response
		if (Response.Status.OK.getStatusCode() != response.getStatus()){
			LOGGER.warning(String.format("[Process %d] ERROR. Response status HTTP %d", clientID.getClientID(), response.getStatus()));
			LOGGER.warning(String.valueOf(response.getEntity()));
			return Utils.FAILURE_VALUE;
		} 
		LOGGER.info(String.valueOf(response.getEntity()));
		return Utils.SUCCESS_VALUE;
	}

	private int sendLog(String logfile) {
		System.out.printf("%d envio los logs al supervisor\n", clientID.getClientID());
		Response response = supervisorHandler.postFile("/supervisor/inform", logfile);
		
		// Check response
		if (Response.Status.OK.getStatusCode() != response.getStatus()){
			LOGGER.warning(String.format("[Process %d] ERROR. Response status HTTP %d", clientID.getClientID(), response.getStatus()));
			LOGGER.warning(String.valueOf(response.getEntity()));
			return Utils.FAILURE_VALUE;
		} 
		
		LOGGER.info(String.valueOf(response.getEntity()));
		return Utils.SUCCESS_VALUE;

	}

	private void simulateOperations() {
		// TODO Auto-generated method stub
		System.out.printf("%d Simulo operaciones\n", getClientID());
		
		try {
			Thread.sleep(Math
					.abs(random.nextInt() % (ClientRicart.OPERATIONS_MAX_TIME - ClientRicart.OPERATIONS_MIN_TIME + 1)
							+ ClientRicart.OPERATIONS_MIN_TIME));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.printf("%d Done\n", getClientID());
	}

	private int enterCriticalSection() {
		System.out.printf("%d Quiero entrar en la seccion critica\n", getClientID());
		Response response = restHandler.callWebServiceResponse("/rest/send_request",
				new RESTParameter("id", clientID.toUniqueFilename()));
		
		// Check response
		if (Response.Status.OK.getStatusCode() != response.getStatus()) {
			LOGGER.warning(String.format("[Process %d] ERROR. Response status HTTP %d", clientID.getClientID(), response.getStatus()));
			LOGGER.warning(String.valueOf(response.getEntity()));
			return Utils.FAILURE_VALUE;
		}
		LOGGER.info(String.valueOf(response.getEntity()));
		return Utils.SUCCESS_VALUE;

	}

	private void criticalSection() {
		System.out.printf("%d Entre en la seccion critica\n", getClientID());
		try {
			Thread.sleep(Math.abs(random.nextInt() % (CRITICAL_SECTION_MAX_TIME - CRITICAL_SECTION_MIN_TIME + 1)
					+ CRITICAL_SECTION_MIN_TIME));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private int exitCriticalSection() {
		System.out.printf("%d sali seccion critica\n", getClientID());
		Response response = restHandler.callWebServiceResponse("/rest/exit",
				new RESTParameter("id", clientID.toUniqueFilename()));
		if (Response.Status.OK.getStatusCode() != response.getStatus()) {
			return Utils.FAILURE_VALUE;
		}
		return Utils.SUCCESS_VALUE;
	}
	
	private int registerClient() {	
		// Send request to server
		Response response = restHandler.callWebServiceResponse("/rest/registrar",
				                                                new RESTParameter("id", clientID.toUniqueFilename()));
		
		// Check response
		if (Response.Status.OK.getStatusCode() != response.getStatus()){
			LOGGER.warning(String.format("[Process %d] ERROR. Response status HTTP %d", clientID.getClientID(), response.getStatus()));
			LOGGER.warning(String.valueOf(response.getEntity()));
			return Utils.FAILURE_VALUE;
		} 
		
		return Utils.SUCCESS_VALUE;
	}
	
	private void writeLog(String action) {
		stringLog.append(Logging.logMessage(this.getNumeroNodo(), action));
	}

	public String getServerURI() {
		return serverURI;
	}

	public void setServerURI(String serverURI) {
		this.serverURI = serverURI;
	}

	public String getSupervisorURI() {
		return supervisorURI;
	}

	public void setSupervisorURI(String supervisorURI) {
		this.supervisorURI = supervisorURI;
	}


	public int getNumeroNodo() {
		return numeroNodo;
	}


	public void setNumeroNodo(int numeroNodo) {
		this.numeroNodo = numeroNodo;
	}
}
