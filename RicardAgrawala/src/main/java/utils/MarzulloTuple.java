/**
 * 
 */
package utils;

/**
 * @author Erick
 * @author Daniel
 *
 */
public class MarzulloTuple implements Comparable<MarzulloTuple> {
	
	/**
	 * Offset of the tuple
	 */
	private long offset;
	
	/**
	 * Type of the tuple
	 */
	private int type;
	
	/**
	 * Constructor of the class
	 * 
	 * @param offset  offset of the tuple
	 * @param type    type of the tuple
	 */
	public MarzulloTuple(long offset, int type) {
		this.offset = offset;
		this.type = type;
	}
	
	/**
	 * Constructor of the class
	 */
	public MarzulloTuple() {
		this.offset = 0;
		this.type = 0;
	}

	/**
	 * Get best tuples applying Marzullo algorithm
	 * 
	 * @param offset    offset of the tuple
	 * @param delay     delay of the tuple
	 * @return          get best tuples <offset,delay>
	 */
	public static MarzulloTuple[] getTuples(long offset, long delay) {
		MarzulloTuple[] tuples = new MarzulloTuple[2];
		tuples[0] = new MarzulloTuple(offset - delay/2, -1);
		tuples[1] = new MarzulloTuple(offset + delay/2,  1);
		return tuples;
	}
	
	/**
	 * Get best tuples applying Marzullo algorithm
	 * 
	 * @param offsetDelay  list of tuples <offset,delay>
	 * @return             best tuples <offset,delay>
	 */
	public static MarzulloTuple[] getTuples(long[] offsetDelay) {
		return getTuples(offsetDelay[0], offsetDelay[1]);
	}

	@Override
	public int compareTo(MarzulloTuple tuple) {
		if (tuple.offset > this.offset)
			return -1;
		else if (tuple.offset < this.offset)
			return 1;
		else
			return this.type;
	}

	/**
	 * @return the offset
	 */
	public long getOffset() {
		return offset;
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}
	
}
