package client;

import java.util.Random;
import java.util.concurrent.Semaphore;

public class Client extends Thread{
	
	private static final int DEFAULT_NUMBER_ITERATIONS = 100;                      
	private static final int OPERATIONS_MIN_TIME = 300;  
	private static final int OPERATIONS_MAX_TIME = 500;                  
	private static final int CRITICAL_SECTION_MIN_TIME = 100;          
	private static final int CRITICAL_SECTION_MAX_TIME = 300; 
	
	private Random random; 
	private int id;
	
	




	/**
	 * @param id
	 */
	public Client(int id) {
		super();
		this.id = id;
		this.random = new Random();
		
		
		
	}






	public void run() {
		
	}
}
