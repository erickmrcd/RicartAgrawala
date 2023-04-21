package clientData;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import rest.Request;

public class ClientData {
	private static Semaphore generalLock = new Semaphore(1);
	private static Semaphore permissionsLock = new Semaphore(1);
	
	private LamportTime requestAccessTimestamp;
	private LamportClock lamportClock;
	private Queue<Request> requestQueue;
	private Semaphore waitSynchronizeSemaphore;
	private int permissions;
	private CriticalSectionState state; 
	private CountDownLatch waitForResponsesStructure;
	
	/**
	 * 
	 */
	public ClientData() {
		this.requestAccessTimestamp = new LamportTime(0);
		this.lamportClock = new LamportClock(new LamportTime(0));
		this.requestQueue = new LinkedList<>();
		this.waitSynchronizeSemaphore = new Semaphore(0);
		this.permissions = 0;
		this.state = CriticalSectionState.FREE;
		this.waitForResponsesStructure = new CountDownLatch(1);
	}
	
	public void writeLock() {
		try {
			ClientData.generalLock.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeUnlock() {
		ClientData.generalLock.release();
	}
	
	public int readLock() {
		return ClientData.generalLock.availablePermits();
	}

	/**
	 * @return the requestAccessTimestamp
	 */
	public LamportTime getRequestAccessTimestamp() {
		return requestAccessTimestamp;
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
	public void setLamportClockThreadUnsafe(LamportClock lamportClock) {
		this.lamportClock = lamportClock;
	}
	
	/**
	 * @return the lamportClock
	 */
	public LamportClock getLamportClockThreadUnsafe() {
		return lamportClock;
	}

	/**
	 * @return the state
	 */
	public CriticalSectionState getStateThreadUnsafe() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setStateThreadUnsafe(CriticalSectionState state) {
		this.state = state;
	}

	/**
	 * @return the waitForResponsesStructure
	 */
	public CountDownLatch getWaitForResponsesStructure() {
		return waitForResponsesStructure;
	}

	/**
	 * @param waitForResponsesStructure the waitForResponsesStructure to set
	 */
	public void resetWaitForResponsesStructure() {
		this.waitForResponsesStructure = new CountDownLatch(1);
	}
	
	public void increaseLamportClockThreadUnsafe() {
		this.lamportClock.increase();
	}
	
	public void writeLockPermissions() {
		//this.permissionsLock.writeLock().lock();
		try {
			ClientData.permissionsLock.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void writeUnlockPermissions() {
		ClientData.permissionsLock.release();
	}
	
	public void addPermissionThreadUnsafe() {
		this.permissions += 1;
	}
	
	public int getPermissionsThreadUnsafe() {
		return this.permissions;
	}
	
	public void setPermissionsThreadUnsafe(int permissions) {
		this.permissions = permissions;
	}

	/**
	 * @return the waitSynchronizeSemaphore
	 */
	public Semaphore getWaitSynchronizeSemaphore() {
		return waitSynchronizeSemaphore;
	}

	/**
	 * @param waitSynchronizeSemaphore the waitSynchronizeSemaphore to set
	 */
	public void resetWaitSynchronizeSemaphore() {
		this.waitSynchronizeSemaphore = new Semaphore(0);
	}
	
	public boolean addToQueueThreadUnsafe(Request request) {
		return this.requestQueue.add(request);
	}
	
	public Request nextFromQueueThreadUnsafe() {
		return this.requestQueue.remove();
	}
	
	public List<Request> removeAllFromQueueThreadUnsafe(){
		 List<Request> l = new ArrayList<>(this.requestQueue);
		 this.requestQueue = new LinkedList<>();
		 return l;
	}
	
	
	
	
}
