package clientData;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import rest.RestRequest;

public class RicartClientData {
	private static Semaphore generalLock = new Semaphore(1);
	private static Semaphore permissionsLock = new Semaphore(1);

	private LamportTime requestAccessTimestamp;
	private LamportClock lamportClock;
	private Queue<RestRequest> requestQueue;
	private Semaphore waitSynchronizeSemaphore;
	private int permissions;
	private State state;
	private CountDownLatch waitForResponsesStructure;

	/**
	 * 
	 */
	public RicartClientData() {
		this.requestAccessTimestamp = new LamportTime(0);
		this.lamportClock = new LamportClock(new LamportTime(0));
		this.requestQueue = new LinkedList<>();
		this.permissions = 0;
		this.state = State.LIBRE;
		this.waitSynchronizeSemaphore = new Semaphore(0);
		this.waitForResponsesStructure = new CountDownLatch(1);
	}

	public void writeLock() {
		try {
			RicartClientData.generalLock.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void writeUnlock() {
		RicartClientData.generalLock.release();
	}

	public int readLock() {
		return RicartClientData.generalLock.availablePermits();
	}

	/**
	 * @return the requestAccessTimestamp
	 */
	public LamportTime getRequestAccessTimestamp() {
		return this.requestAccessTimestamp;
	}

	/**
	 * @param requestAccessTimestamp the requestAccessTimestamp to set
	 */
	public void setRequestAccessTimestamp(LamportTime requestAccessTimestamp) {
		this.requestAccessTimestamp = requestAccessTimestamp;
	}

	/**
	 * @param lamportClock the lamportClock to set
	 */
	public void setLamportClockThread(LamportClock lamportClock) {
		this.lamportClock = lamportClock;
	}

	/**
	 * @return the lamportClock
	 */
	public LamportClock getLamportClockThread() {
		return this.lamportClock;
	}

	/**
	 * @return the state
	 */
	public State getStateOfThread() {
		return this.state;
	}

	/**
	 * @param state the state to set
	 */
	public void setStateOfThread(State state) {
		this.state = state;
	}

	/**
	 * @return the waitForResponsesStructure
	 */
	public CountDownLatch getWaitForResponsesStructure() {
		return this.waitForResponsesStructure;
	}

	/**
	 * @param waitForResponsesStructure the waitForResponsesStructure to set
	 */
	public void resetWaitForResponsesStructure() {
		this.waitForResponsesStructure = new CountDownLatch(1);
	}

	public void increaseLamportClockThread() {
		this.lamportClock.increase();
	}

	public void writeLockPermissions() {
		// this.permissionsLock.writeLock().lock();
		try {
			RicartClientData.permissionsLock.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void writeUnlockPermissions() {
		RicartClientData.permissionsLock.release();
	}

	public void addPermissionThread() {
		this.permissions += 1;
	}

	public int getPermissionsThread() {
		return this.permissions;
	}

	public void setPermissionsThread(int permissions) {
		this.permissions = permissions;
	}

	/**
	 * @return the waitSynchronizeSemaphore
	 */
	public Semaphore getWaitSynchronizeSemaphore() {
		if (null == this.waitSynchronizeSemaphore) {
			this.waitSynchronizeSemaphore = new Semaphore(0);
		}
		return this.waitSynchronizeSemaphore;
	}

	/**
	 * @param waitSynchronizeSemaphore the waitSynchronizeSemaphore to set
	 */
	public void resetWaitSynchronizeSemaphore() {
		this.waitSynchronizeSemaphore = new Semaphore(0);
	}

	public boolean addToQueueThreadUnsafe(RestRequest request) {
		return this.requestQueue.add(request);
	}

	public RestRequest nextFromQueueThreadUnsafe() {
		return this.requestQueue.remove();
	}

	public List<RestRequest> removeAllFromQueueThreadUnsafe() {
		List<RestRequest> l = new ArrayList<>(this.requestQueue);
		this.requestQueue = new LinkedList<>();
		return l;
	}

	public String printQueue() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n+----+-------------+\n");
		sb.append("| ID |   ADDRESS   |\n");
		sb.append("+----+-------------+\n");

		for (RestRequest r : requestQueue) {
			sb.append(String.format("| %d  | %s |\n", r.getClientId().getClientID(), r.getClientId().getIpAddress()));
		}

		sb.append("+----+-------------+\n");
		return sb.toString();
	}

}
