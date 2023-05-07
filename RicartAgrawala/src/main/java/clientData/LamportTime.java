package clientData;

public class LamportTime implements Comparable<LamportTime> {

	private long value; 

	public LamportTime(long value) {
		this.value = value;
	}

	public LamportTime(int value) {
		this.value = (long) value;
	}

	public long getValue() {
		return value;
	}

	public void add(long value) {
		this.value += value;
	}

	public long toLong() {
		return this.value;
	}

	
	
	public static LamportTime max(LamportTime t1, LamportTime t2) {
		return (t1.getValue() >= t2.getValue()) ? t1 : t2;  
	}

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
	
	
}
