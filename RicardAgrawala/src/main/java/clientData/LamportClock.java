package clientData;

public class LamportClock {
	
	private LamportTime time;

	/**
	 * @param time This clock's initial time
	 */
	public LamportClock(LamportTime time) {
		this.time = time;
	}
	
	/**
	 * @param time This clock's initial time
	 */
	public LamportClock(long time) {
		this.time = new LamportTime(time);
	}
	
	/**
	 * 
	 */
	public LamportClock() {
		this(0);
	}

	/**
	 * @return  This clock's time
	 */
	public LamportTime getTime() {
		return time;
	}

	/**
	 * @param time  This clock's time
	 */
	public void setTime(LamportTime time) {
		this.time = time;
	}
	
	
	/**
	 * @param time This clock's time
	 */
	public void setTime(long time) {
		this.setTime(new LamportTime(time));
	}
	
	/**
	 * @param time This clock's time
	 */
	public void setTime(int time) {
		this.setTime((long) time); 
	}
	
	/**
	 * @param i Number of units to add to the clock's time
	 */
	public void increase(long i) {
		this.time.add(i);
	}
	
	/**
	 * @param i  Number of units to add to the clock's time
	 */
	public void increase(int i) {
		this.increase((long) i);
	}
	
	
	/**
	 * Increases the value in one unit
	 */
	public void increase() {
		this.increase(1);
	}
}
