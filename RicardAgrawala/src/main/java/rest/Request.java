package rest;

import java.util.Queue;

import client.ClientUID;
import clientData.LamportTime;

public class Request implements Comparable<Request>{

	private LamportTime timestamp;
	private ClientUID clientId;
	
	
	/**
	 * @param timestamp
	 * @param clientId
	 */
	public Request(LamportTime timestamp, ClientUID clientId) {
		this.timestamp = timestamp;
		this.clientId = clientId;
	}


	/**
	 * @return the timestamp
	 */
	public LamportTime getTimestamp() {
		return timestamp;
	}


	/**
	 * @return the clientId
	 */
	public ClientUID getClientId() {
		return clientId;
	}


	public static int compare(Request r1,Request r2) {
		
		int comparison = LamportTime.compare(r1.getTimestamp(), r2.getTimestamp());
		if (comparison != 0) {
			return comparison;
		} else {
			return ClientUID.compare(r1.getClientId(), r2.getClientId());
		}
	}

	@Override
	public int compareTo(Request o) {
		// TODO Auto-generated method stub
		return Request.compare(this, o);
	}


	@Override
	public String toString() {
		return "Request [timestamp=" + timestamp + ", clientId=" + clientId + "]";
	}
	
	

}
