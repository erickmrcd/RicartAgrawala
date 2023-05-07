/**
 * 
 */
package logging;

/**
 * @author Erick Mercado
 * @author Daniel 
 *
 */
public class TimeStamps implements Comparable<TimeStamps> {
	
	private static final String BOUNDS_SEPARATOR = ",";
	private long lowerBound;
	private long upperBound;

	public TimeStamps(long lowerBound, long upperBound) {
		if (upperBound < lowerBound) {
			this.lowerBound = upperBound;
			this.upperBound = lowerBound;
		} else {
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}
	}
	

	public TimeStamps(String lowerBound, String upperBound) {
		this(Long.parseLong(lowerBound), Long.parseLong(upperBound));
	}
	

	public TimeStamps(String timestamp) {
		String[] bounds = timestamp.split(TimeStamps.BOUNDS_SEPARATOR);
		long[] lbounds = new long[] {Long.parseLong(bounds[0]), Long.parseLong(bounds[1])};
		if (this.upperBound < this.lowerBound)
			throw new IllegalArgumentException("lowerBound must be less than or equal to upperBound");
		this.lowerBound = lbounds[0];
		this.upperBound = lbounds[1];
	}

	/**
	 * @return the lower bound
	 */
	public long getLowerBound() {
		return lowerBound;
	}

	/**
	 * @return the upper bound
	 */
	public long getUpperBound() {
		return upperBound;
	}
	
	public static int compare(TimeStamps t1, TimeStamps t2) {
		if (t1.getLowerBound() < t2.getLowerBound()) {
			if (t1.getUpperBound() < t2.getLowerBound()) {
				return -1;
			} else if (t1.getUpperBound() > t2.getLowerBound()) {
				return -1;
			}
		} else if (t1.getLowerBound() > t2.getLowerBound()) {
			if (t1.getLowerBound() < t2.getUpperBound()) {
				return 1;
			} else if (t1.getLowerBound() > t2.getUpperBound()) {
				return 1;
			}
		}
		
		return 0; // Equal
	}
	
	@Override
	public int compareTo(TimeStamps other) {
		return TimeStamps.compare(this, other);
	}

	@Override
	public String toString() {
		return String.format("%d" + TimeStamps.BOUNDS_SEPARATOR + "%d", this.lowerBound, this.upperBound);
	}
}
