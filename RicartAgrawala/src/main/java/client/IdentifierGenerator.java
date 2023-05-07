package client;

public class IdentifierGenerator {

	private final String IP_ADDRESS;
	private static int nextIdentifier = 0;
	/**
	 * @param iP_ADDRESS
	 */
	public IdentifierGenerator(String iP_ADDRESS) {
		this.IP_ADDRESS = iP_ADDRESS;
	}
	
	public IdentifierGenerator(String iP_ADDRESS, int firstUID) {
		this.IP_ADDRESS = iP_ADDRESS;
		IdentifierGenerator.nextIdentifier = firstUID;
	}

	public ClientIdentifier nextIdentifier() {
		ClientIdentifier newIdentifier = new ClientIdentifier(IP_ADDRESS, nextIdentifier);
		nextIdentifier += 1;
		return newIdentifier;
	}
	
}
