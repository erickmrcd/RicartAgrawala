/**
 * 
 */
package main;

import java.net.URI;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import client.ClientRicart;
import client.ClientUID;
import clientData.CriticalSectionState;

/**
 * @author erick
 *
 */
public class Main {

	private static final int NUM_PROCESOS = 2;
	private static ClientRicart[] clients;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Client client = ClientBuilder.newClient();
		URI uri = UriBuilder.fromUri("http://localhost:8080/RicardAgrawala").build();
		WebTarget target = client.target(uri);
		llamarReinicio(target);
		clients = new ClientRicart[NUM_PROCESOS];
		for (int id = 0; id < NUM_PROCESOS; id++) {
			clients[id] = new ClientRicart(new ClientUID("123", id));
		}
		startExecution(clients);
		
	}

	private synchronized static void llamarReinicio(WebTarget target) {
		String reinicio = target.path("rest").path("rest").path("reset").request(MediaType.TEXT_PLAIN).get(String.class);
		System.out.println(reinicio);

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
