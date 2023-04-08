/**
 * 
 */
package main;

import java.util.concurrent.Semaphore;

import client.Client;
import clientData.CriticalSectionState;

/**
 * @author erick
 *
 */
public class Main {

	private static final int NUM_PROCESOS = 2;
	private static int[] clientsId = null;
	private static int numTotalClients = 0;
	private static Semaphore generalLock = new Semaphore(1);
	private static Semaphore permissionsLock = new Semaphore(1);
	private static CriticalSectionState state;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		state = CriticalSectionState.FREE;
		Client[] clients = null;
		for (int id = 0; id < NUM_PROCESOS; id++) {
			clients[id] = new Client(id);
		}
		startExecution(clients);

	}

	/**
	 * 
	 */
	public void pedirEntrarEnSeccionCritica() {
		state = CriticalSectionState.REQUESTING;
	}

	public void SeccionCritica() {
		state = CriticalSectionState.BUSY;
	}

	public void salirSeccionCritica() {
		state = CriticalSectionState.FREE;
	}
	
	private static void startExecution(Client[] clients) {

		for (Client client: clients) {
			if (null == client) {
				System.out.println(String.format("One client did not start correctly"));
				return ;
			}
			client.start();
		}
	}
}
