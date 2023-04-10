/**
 * 
 */
package utils;

/**
 * @author erick
 * @author Daniel
 *
 */
public class SynchronizationUtils {

	public static long calculateDelay(long t0, long t1, long t2, long t3) {

		// Time from client to server
		long t = t1 - t0;

		// Time from server to client
		long t_ = t3 - t2;

		// Calculate delay
		long d = t + t_;

		return d;
	}

	public static long calculateOffset(long t0, long t1, long t2, long t3) {
		// Calculate offset estimation
		long o_ = (t1 - t0 + t2 - t3) / 2;

		return o_;
	}

	public static long calculateDelay(long[] timestamps) {
		return calculateDelay(timestamps[0], timestamps[1], timestamps[2], timestamps[3]);
	}

	public static long calculateOffset(long[] timestamps) {
		return calculateOffset(timestamps[0], timestamps[1], timestamps[2], timestamps[3]);
	}
}
