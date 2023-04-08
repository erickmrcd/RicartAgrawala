package clientData;

public enum CriticalSectionState {
	
	/**
	 * Free state
	 */
	FREE("Free"),
	
	/**
	 * Busy state
	 */
	BUSY("Busy"),
	
	/**
	 * Requesting state
	 */
	REQUESTING("Requesting");
	
	private String value;
	
	/**
	 * Sets the state of the critical section
	 * 
	 * @param value     actual state of the critical section
	 */
	private CriticalSectionState(String value) {
		this.value = value;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}
}
