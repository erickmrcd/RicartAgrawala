package rest;

import java.text.ParseException;

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


	public static Request parse(String req) throws ParseException {
		// TODO Auto-generated method stub
		String[] parts = req.split("#");
		if (parts.length != 2)
			throw new ParseException(
					String.format(
							"Bad format in string '%s'. Must be obtained from request.toString().",
							req
					),
					0
			);
		return new Request(
				new LamportTime(Long.parseLong(parts[0])), 
				ClientUID.fromUniqueFilename(parts[1])
		);
	}
	
	

}
