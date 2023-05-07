package rest;

import java.text.ParseException;

import client.ClientIdentifier;
import clientData.LamportTime;

public class RestRequest implements Comparable<RestRequest>{

	private LamportTime timestamp;
	private ClientIdentifier clientId;
	

	public RestRequest(LamportTime timestamp, ClientIdentifier clientId) {
		this.timestamp = timestamp;
		this.clientId = clientId;
	}

	public LamportTime getTimestamp() {
		return timestamp;
	}


	public ClientIdentifier getClientId() {
		return clientId;
	}


	public static int compare(RestRequest r1,RestRequest r2) {
		
		int comparison = LamportTime.compare(r1.getTimestamp(), r2.getTimestamp());
		if (comparison != 0) {
			return comparison;
		} else {
			return ClientIdentifier.compare(r1.getClientId(), r2.getClientId());
		}
	}

	@Override
	public int compareTo(RestRequest o) {
		return RestRequest.compare(this, o);
	}


	@Override
	public String toString() {
		return timestamp.toString() +"#"+ clientId.toUniqueIdentifier();
	}


	public static RestRequest parse(String req) throws ParseException {
		String[] parts = req.split("#");
		if (parts.length != 2)
			throw new ParseException(
					String.format(
							"Bad format in string '%s'. Must be obtained from request.toString().",
							req
					),
					0
			);
		return new RestRequest(
				new LamportTime(Long.parseLong(parts[0])), 
				ClientIdentifier.fromUniqueIdentifier(parts[1])
		);
	}
	
	

}
