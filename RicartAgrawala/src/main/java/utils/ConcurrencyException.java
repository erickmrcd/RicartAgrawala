/**
 * 
 */
package utils;

/**
 * @author erick
 * @author Daniel
 *
 */
public class ConcurrencyException extends Exception {

	private static final long serialVersionUID = 1L;
	public ConcurrencyException(String message) {
		super(message);
	}
	public ConcurrencyException() {
		super();
	}

}
