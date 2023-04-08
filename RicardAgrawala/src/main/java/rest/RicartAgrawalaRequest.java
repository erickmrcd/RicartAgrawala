package rest;


/**
 * This class contains the representation of a client's request (timestamp, clientID). 
 * 
 * This request will sent to the rest of the client requesting access to enter
 * the critical section. 
 * 
 * Every client has a queue of requests of type 'RicartAgrawalaRequest' to handle
 * requests from other clients
 * 
 * @author Luis Blazquez Minambres
 * @author Samuel Gomez Sanchez
 *
 */
public class RicartAgrawalaRequest implements Comparable<RicartAgrawalaRequest> {

	@Override
	public int compareTo(RicartAgrawalaRequest o) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/**
	 * Logging handler
	 */
	
}
