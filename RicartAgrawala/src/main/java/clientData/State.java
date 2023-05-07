package clientData;

public enum State {
	
	LIBRE("Free"),
	OCUPADA("Busy"),
	REQUERIDA("Requesting");
	
	private String value;
	private State(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
}
