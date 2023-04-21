package clientData;

public class LamportTime implements Comparable<LamportTime> {
	
	/**
	 * Value of timestamp
	 */
	private long value; 

	/**
	 * Constructor of the class
	 * 
	 * @param value     value of the timestamp
	 */
	public LamportTime(long value) {
		this.value = value;
	}
	
	/**
	 * @param value      new value of the timestamp
	 */
	public LamportTime(int value) {
		this.value = (long) value;
	}

	/**
	 * @return the value
	 */
	public long getValue() {
		return value;
	}
	
	/**
	 * Adds new value to the current one
	 * 
	 * @param value      new value to add
	 */
	public void add(long value) {
		this.value += value;
	}
	
	/**
	 * @return the value
	 */
	public long toLong() {
		return this.value;
	}
	
	/**
	 * Compares two timestamps
	 * 
	 * @param t1    timestamp to compare
	 * @param t2    timestamp to compare
	 * @return      result of comparison. -1 if t2 is greater, 0 if equals and 1 if t1 is greater
	 */
	public static int compare(LamportTime t1, LamportTime t2) {
		return Long.compare(t1.getValue(), t2.getValue());
	}

	@Override
	public int compareTo(LamportTime other) {
		return LamportTime.compare(this, other);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (value ^ (value >>> 32));
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
		LamportTime other = (LamportTime) obj;
		if (value != other.value)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(this.value);
	}
	
	/**
	 * Gets the maximum value of the two given
	 * 
	 * @param t1    timestamp to compare
	 * @param t2    timestamp to compare
	 * @return      result of comparison. -1 if t2 is greater, 0 if equals and 1 if t1 is greater
	 */
	public static LamportTime max(LamportTime t1, LamportTime t2) {
		return (t1.getValue() >= t2.getValue()) ? t1 : t2;  
	}
}
