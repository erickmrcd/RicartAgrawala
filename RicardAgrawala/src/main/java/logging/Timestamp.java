/**
 * 
 */
package logging;

/**
 * A log timestamp
 * 
 * This representation is different from logical 'LamportTime' timestamp representation
 * as this one is exclusively used for timestamps set in logs
 *
 * @author Luis Blazquez Miñambres
 * @author Samuel Gómez Sánchez
 *
 */
public class Timestamp implements Comparable<Timestamp> {
	
	private static final String BOUNDS_SEPARATOR = ",";
	
	/**
	 * Lower value of the timestamp limit
	 */
	private long lowerBound;
	
	/**
	 * Upper value of the timestamp limit
	 */
	private long upperBound;

	/**
	 * Constructor of the class
	 * 
	 * @param lowerBound      lower value of the timestamp limit
	 * @param upperBound      upper value of the timestamp limit
	 */
	public Timestamp(long lowerBound, long upperBound) {
		if (upperBound < lowerBound) {
			this.lowerBound = upperBound;
			this.upperBound = lowerBound;
		} else {
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}
	}
	
	/**
	 * Constructor of the class
	 * 
	 * @param lowerBound      lower value of the timestamp limit
	 * @param upperBound      upper value of the timestamp limit
	 */
	public Timestamp(String lowerBound, String upperBound) {
		this(Long.parseLong(lowerBound), Long.parseLong(upperBound));
	}
	
	/**
	 * Constructor of the class
	 * 
	 * @param timestamp      timestamp limit
	 */
	public Timestamp(String timestamp) {
		String[] bounds = timestamp.split(Timestamp.BOUNDS_SEPARATOR);
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

	@Override
	public int compareTo(Timestamp other) {
		return Timestamp.compare(this, other);
	}
	
	/**
	 * Compare two timestamp limit values
	 * 
	 * @param t1    timestamp limit to compare
	 * @param t2    timestamp limit to compare
	 * @return      the result of comparison . -1 if t2 is greater, 0 if equals and 1 if t1 is greater
	 */
	public static int compare(Timestamp t1, Timestamp t2) {
		if (t1.getLowerBound() < t2.getLowerBound()) {
			/* 
			 *              CASE 1
			 * ================================= 
			 *                T2
			 *             <------->
			 *      T1
			 *  <------->
			 * ------------------------------------------------> time
			 */
			if (t1.getUpperBound() < t2.getLowerBound()) {
				return -1;
			/* 
			 *              CASE 2
			 * ================================= 
			 *         T2
			 *      <------->
			 *      T1
			 *  <------->
			 * ------------------------------------------------> time
			 */
			} else if (t1.getUpperBound() > t2.getLowerBound()) {
				return -1;
			}
		} else if (t1.getLowerBound() > t2.getLowerBound()) {
			/* 
			 *              CASE 3
			 * ================================= 
			 *         T2
			 *      <------->
			 *              T1
			 *           <------->
			 * ------------------------------------------------> time
			 */
			if (t1.getLowerBound() < t2.getUpperBound()) {
				return 1;
			/* 
			 *              CASE 4
			 * ================================= 
			 *         T2
			 *      <------->
			 *                      T1
			 *                   <------->
			 * ------------------------------------------------> time
			 */
			} else if (t1.getLowerBound() > t2.getUpperBound()) {
				return 1;
			}
		}
		
		return 0; // Equal
	}

	@Override
	public String toString() {
		return String.format("%d" + Timestamp.BOUNDS_SEPARATOR + "%d", this.lowerBound, this.upperBound);
	}
}
