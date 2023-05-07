/**
 * 
 */
package utils;

/**
 * @author Erick José Mercado Hernández
 * @author Daniel
 *
 */
public class MarzulloTuple implements Comparable<MarzulloTuple> {
	private long offset;
	private int type;
	
	public MarzulloTuple(long offset, int type) {
		this.offset = offset;
		this.type = type;
	}
	
	public MarzulloTuple() {
		this.offset = 0;
		this.type = 0;
	}

	public static MarzulloTuple[] getTuples(long offset, long delay) {
		MarzulloTuple[] tuples = new MarzulloTuple[2];
		tuples[0] = new MarzulloTuple(offset - delay/2, -1);
		tuples[1] = new MarzulloTuple(offset + delay/2,  1);
		return tuples;
	}
	
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

	public long getOffset() {
		return offset;
	}
	public int getType() {
		return type;
	}
	
}
