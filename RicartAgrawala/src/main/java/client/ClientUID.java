package client;

import java.text.ParseException;

public class ClientUID implements Comparable<ClientUID>{
	
	private String ipAddress;
	private int clientID;
	
	
	public ClientUID(String ipAddress, int id) {
		this.ipAddress = ipAddress;
		this.clientID = id;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}
	
	public int getClientID() {
		return clientID;
	}
	
	@Override
	public String toString() {
		return "[" + ipAddress + "(" + clientID + ")]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + clientID;
		result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClientUID other = (ClientUID) obj;
		if (clientID != other.clientID)
			return false;
		if (ipAddress == null) {
			if (other.ipAddress != null)
				return false;
		} else if (!ipAddress.equals(other.ipAddress))
			return false;
		return true;
	}
	
	public static int compare(ClientUID id1, ClientUID id2) {
		int comparison = id1.getIpAddress().compareTo(id2.getIpAddress());
		if (comparison != 0)
			return comparison;
		else
			return Integer.compare(id1.getClientID(), id2.getClientID());
	}

	@Override
	public int compareTo(ClientUID other) {
		return ClientUID.compare(this, other);
	}
	
	public String toUniqueFilename(String fileExtension) {
		if (fileExtension != null)
			return String.format("%s_%d.%s", this.ipAddress, this.clientID, fileExtension);
		else
			return String.format("%s_%d", this.ipAddress, this.clientID);
	}
	
	public String toUniqueFilename() {
		return this.toUniqueFilename(null);
	}

	public static ClientUID fromUniqueFilename(String id) throws ParseException{
		String[] parts = id.split("_");
		if (parts.length < 2) {
			throw new IllegalArgumentException(String.format("Filename '%s' has a bad structure", id));
		} else if (parts.length > 2) {
			int offset = 0;
			for (String part : parts)
				offset += part.length();
			throw new ParseException(String.format("Unknown format of filename '%s'", id), offset);
		}
		String[] rightParts = parts[1].split("\\.");  // Support for file extensions
		return new ClientUID(parts[0], Integer.parseInt(rightParts[0]));	
	}

}
