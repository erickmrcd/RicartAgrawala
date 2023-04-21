/**
 * 
 */
package utils;

/**
 * @author erick
 * @author Daniel
 *
 */
public class SynchronizationException extends Exception {

	/**
	 * Identification of serial exception
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor of the class
	 * 
	 * @param message message to show
	 */
	public SynchronizationException(String message) {
		super(message);
	}

	/**
	 * Contructor of the class
	 */
	public SynchronizationException() {
		super();
	}

}
