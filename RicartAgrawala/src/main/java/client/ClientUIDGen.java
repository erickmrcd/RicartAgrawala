package client;

public class ClientUIDGen {

	private final String IP_ADDRESS;
	private static int nextUID = 0;
	/**
	 * @param iP_ADDRESS
	 */
	public ClientUIDGen(String iP_ADDRESS) {
		this.IP_ADDRESS = iP_ADDRESS;
	}
	
	public ClientUIDGen(String iP_ADDRESS, int firstUID) {
		this.IP_ADDRESS = iP_ADDRESS;
		ClientUIDGen.nextUID = firstUID;
	}

	public ClientUID nextUID() {
		ClientUID newUID = new ClientUID(IP_ADDRESS, nextUID);
		nextUID += 1;
		return newUID;
	}
	
}
