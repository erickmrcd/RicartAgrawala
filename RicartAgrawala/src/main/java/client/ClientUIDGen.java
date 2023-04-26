package client;

public class ClientUIDGen {

	private final String IP_ADDRESS;
	private static int nextGUID = 0;
	/**
	 * @param iP_ADDRESS
	 */
	public ClientUIDGen(String iP_ADDRESS) {
		this.IP_ADDRESS = iP_ADDRESS;
	}
	
	public ClientUID nextGUID() {
		ClientUID newUID = new ClientUID(IP_ADDRESS, nextGUID);
		nextGUID += 1;
		return newUID;
	}
	
}
