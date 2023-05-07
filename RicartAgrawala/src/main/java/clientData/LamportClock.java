package clientData;

/**
 * 
 * @author Erick
 * @author Daniel
 * 
 */
public class LamportClock {
	
	private LamportTime time;

	public LamportClock(LamportTime time) {
		this.time = time;
	}

	public LamportClock(long time) {
		this.time = new LamportTime(time);
	}
	public LamportClock() {
		this(0);
	}

	public LamportTime getTime() {
		return time;
	}

	public void setTime(LamportTime time) {
		this.time = time;
	}
	
	public void setTime(long time) {
		this.setTime(new LamportTime(time));
	}

	public void setTime(int time) {
		this.setTime((long) time); 
	}

	public void increase(long i) {
		this.time.add(i);
	}

	public void increase(int i) {
		this.increase((long) i);
	}

	public void increase() {
		this.increase(1);
	}
}
