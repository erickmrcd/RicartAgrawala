package client;

import java.net.URI;
import java.util.Random;
import java.util.concurrent.Semaphore;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

public class ClientRicart extends Thread {

	private static final int DEFAULT_NUMBER_ITERATIONS = 100;
	private static final int OPERATIONS_MIN_TIME = 300;
	private static final int OPERATIONS_MAX_TIME = 500;
	private static final int CRITICAL_SECTION_MIN_TIME = 100;
	private static final int CRITICAL_SECTION_MAX_TIME = 300;
	public Semaphore s1;

	private Random random;

	private ClientUID clientID;
	
	
	/**
	 * @param clientID
	 */
	public ClientRicart(ClientUID clientID) {
		this.clientID = clientID;
		this.random = new Random();
	}
	
	
	public ClientRicart(ClientUID clientUID, Semaphore permissionsLock) {
		this.clientID = clientUID;
		this.s1 = permissionsLock;
		this.random = new Random();
	}


	/**
	 * @return the clientID
	 */
	public int getClientID() {
		return clientID.getClientID();
	}

	




	public void run() {
		Client client = ClientBuilder.newClient();
		URI uri = UriBuilder.fromUri("http://localhost:8080/RicardAgrawala").build();
		WebTarget target = client.target(uri);
		waitOthers(target);
		for (int i = 0; i < DEFAULT_NUMBER_ITERATIONS; i++) {

			
			// Simulating operations in between 0.3 and 0.5 seconds
			
			
			simulateOperations();
			
			// Try to enter Critical Section
			enterCriticalSection(target);

			// Stay in critical section
			criticalSection();


			// Exit critical section and grant access to other clients
			exitCriticalSection();

		}
	}
	



	private synchronized void waitOthers(WebTarget target) {
		// TODO Auto-generated method stub
		String inicio = target.path("rest").path("rest").path("wait").request(MediaType.TEXT_PLAIN).get(String.class);
		System.out.println(inicio);
	}


	private void simulateOperations() {
		// TODO Auto-generated method stub
		System.out.printf("%d Hago cosas\n",getClientID());
		try {
			Thread.sleep(Math.abs(random.nextInt()%(ClientRicart.OPERATIONS_MAX_TIME - 
					ClientRicart.OPERATIONS_MIN_TIME + 1) + ClientRicart.OPERATIONS_MIN_TIME));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private synchronized void enterCriticalSection(WebTarget target) {
		// TODO Auto-generated method stub
		String inicio = target.path("rest").path("send_request").request(MediaType.TEXT_PLAIN).get(String.class);
		System.out.println(inicio);
	}

	

	private void criticalSection() {
		System.out.printf("%d Entre en la seccion critica\n",getClientID());
		try {
			//s1.acquire();
			Thread.sleep(Math.abs(random.nextInt()%(ClientRicart.CRITICAL_SECTION_MAX_TIME - 
					ClientRicart.CRITICAL_SECTION_MIN_TIME + 1) + ClientRicart.CRITICAL_SECTION_MIN_TIME));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	
	
	private void exitCriticalSection() {
		System.out.printf("%d sali seccion critica\n",getClientID());
		//s1.release();
	}
}
