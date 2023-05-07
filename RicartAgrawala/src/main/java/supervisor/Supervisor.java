/**
 * 
 */
package supervisor;

import java.util.List;
import java.util.Map;
import java.io.File;
import java.util.*;
import java.util.ArrayList;
import java.util.Collections;

import javax.ws.rs.core.MediaType;

import client.ClientIdentifier;
import logging.Logs;
import utils.RestHandler;
import utils.ConcurrencyException;
import utils.MarzulloTuple;
import utils.RestParameter;

/**
 * @author erick
 * @author Daniel
 */
public class Supervisor {

	private static RestHandler restHandler;
	private static List<ClientIdentifier> clients;
	private static List<String> logFilenames = null;
	private static String ipNodo1;
	private static String ipNodo2;
	private static String ipNodo3;
	private static int numClientes = 0;
	private static String supervisor;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		if (args.length < 1) {
			System.out.println("No se han añadido los correspondientes argumentos (minimo 3)");
			System.out.println("argumento 1: IP LOCAL que es la misma de donde se lanzara el primer cliente");
			System.out.println("argumento 2: IP segundo equipo");
			System.out.println("argumento 3: IP Tercer equipo");
			System.exit(0);
		}
			

		if (args.length == 1) {
			setIpNodo1(args[0]);
			numClientes = args.length * 2;
		}

		if (args.length == 2) {
			setIpNodo1(args[0]);
			setIpNodo2(args[1]);
			numClientes = args.length * 2;
		}

		if (args.length == 3) {
			setIpNodo1(args[0]);
			setIpNodo2(args[1]);
			setIpNodo3(args[2]);
			numClientes = args.length * 2;
		}

		setSupervisor("http://" + getIpNodo1() + ":8080/RicartAgrawala");
		Map<ClientIdentifier, long[]> bestEstimations = new HashMap<>();
		Map<ClientIdentifier, List<long[]>> estimations = new HashMap<>();
		clients = new ArrayList<>();
		System.out.println(numClientes);
		for (int i = 0; i < numClientes; i++) {
			if (numClientes == 2) {
				clients.add(new ClientIdentifier(getIpNodo1(), i));
			} else if (numClientes == 4) {
				if (i < 2) {
					clients.add(new ClientIdentifier(getIpNodo1(), i));
				} else {
					clients.add(new ClientIdentifier(getIpNodo2(), i));
				}
			} else {
				if (i < 2) {
					clients.add(new ClientIdentifier(getIpNodo1(), i));
				} else if (i >= 2 && i < 4) {
					clients.add(new ClientIdentifier(getIpNodo2(), i));
				} else {
					clients.add(new ClientIdentifier(getIpNodo3(), i));
				}
			}
		}

		restHandler = new RestHandler(getSupervisor());
		// System.out.println(getSupervisor());
		restHandler.callWebService("/supervisor/setup",
				new RestParameter("num_clients", String.valueOf(clients.size())));
		try {
			runEstimations(clients, estimations);
		} catch (ConcurrencyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		String s = restHandler.callWebService(MediaType.TEXT_PLAIN, "/supervisor/collect_logs");
		logFilenames = Arrays.asList(s.split(";"));
		System.out.println("Collected log files.");

		try {
			runEstimations(clients, estimations);
		} catch (ConcurrencyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		// marzullo
		for (ClientIdentifier currentUID : clients) {
			bestEstimations.put(currentUID, marzullo(estimations.get(currentUID)));
		}

		for (String filename : logFilenames) {
			long[] offsetBounds = bestEstimations.get(ClientIdentifier.fromUniqueIdentifier(filename));
			Logs.normalizeLog(Server.UPLOAD_LOCATION + File.separator + filename, offsetBounds);
		}

		Logs.mergeLogs(logFilenames, "clients.log");

		System.out.println(String.format("Log files check result: '%s'",
				Logs.checkLog("clients.log") ? "Successful execution" : "Critical section violation"));

		Scanner scanner = new Scanner(System.in);
		System.out.print("Execution of Supervisor finished. Press any key to close...");
		scanner.nextLine();
		scanner.close();
	}

	private static void runEstimations(List<ClientIdentifier> clients, Map<ClientIdentifier, List<long[]>> estimations)
			throws ConcurrencyException {
		for (ClientIdentifier currentID : clients) {
			// Add IP to Map
			if (!estimations.keySet().contains(currentID)) {
				estimations.put(currentID, new ArrayList<long[]>());
			}

			// Estimate offset values
			List<long[]> currentEstimation = estimateOffset(currentID);
			if (null == currentEstimation) {

				throw new ConcurrencyException();
			}

			// Stores estimations for the current client
			for (long[] pair : currentEstimation) {
				estimations.get(currentID).add(pair);
			}
		}
	}

	private static List<long[]> estimateOffset(ClientIdentifier currentID) {
		// TODO Auto-generated method stub
		List<long[]> offsetDelayPairs = new ArrayList<>();
		long[] timeStamps = new long[4]; // Times t0, t1, t2 , t3

		// Set WEB Uri
		RestHandler webUtils = new RestHandler("http://" + currentID.getIpAddress() + ":8080/RicartAgrawala");
		// System.out.println("http://"+currentID.getIpAddress()+":8080/RicartAgrawala");
		// Clear the last server's data
		offsetDelayPairs.clear();
		for (int i = 0; i < 10; ++i) {
			// Delivery time, 't0'
			timeStamps[0] = System.currentTimeMillis();

			// Receive server's receipt and delivery times, 't1' and 't2'
			long[] serverTimes = ntpSync(webUtils, currentID, (i == 10 - 1) ? true : false);
			if (null == serverTimes) {
				return null;
			}

			// Receipt time, 't3'
			timeStamps[3] = System.currentTimeMillis();

			// Add 't1' and 't2' to array
			timeStamps[1] = serverTimes[0];
			timeStamps[2] = serverTimes[1];
			
			// Calculate offset and delay
			offsetDelayPairs.add(new long[] { calculateOffset(timeStamps),
					calculateDelay(timeStamps) });
		}

		return offsetDelayPairs;
	}

	private static long calculateDelay(long[] timeStamps) {
		return calculateDelay(timeStamps[0], timeStamps[1], timeStamps[2], timeStamps[3]);
	}

	public static long calculateOffset(long[] timestamps) {
		return calculateOffset(timestamps[0], timestamps[1], timestamps[2], timestamps[3]);
	}
	
	public static long calculateDelay(long t0, long t1, long t2, long t3) {
		long t = t1 - t0;
		long t_ = t3 - t2;
		long d = t + t_;
		return d;
	}

	public static long calculateOffset(long t0, long t1, long t2, long t3) {
		// Calculate offset estimation
		long o_ = (t1 - t0 + t2 - t3) / 2;
		return o_;
	}

	private static long[] ntpSync(RestHandler webUtils, ClientIdentifier clientID, boolean lastSync) {
		String delimiter = "#";
		long[] requestPair = new long[10];
		// Send request
		String timesString = webUtils.callWebService(MediaType.TEXT_PLAIN, "/rest/synchronize",
				new RestParameter[] { new RestParameter("id", clientID.toUniqueIdentifier()),
						new RestParameter("finished", String.valueOf(lastSync)) });

		// Split the times separated by #
		String[] timePair = timesString.split(delimiter);

		try {
			if (timePair.length != 2)
				throw new NumberFormatException();
			requestPair[0] = Long.parseLong(timePair[0]);
			requestPair[1] = Long.parseLong(timePair[1]);
		} catch (NumberFormatException e) {
			return null;
		}

		return requestPair; // <offset,pair>
	}

	private static long[] marzullo(List<long[]> timePairs) {

		int maxNumberOfIntervals = 0, currentNumberOfIntervals = 0;
		long bestStart = -1, bestEnd = -1;

		List<MarzulloTuple> tuples = new ArrayList<>();
		for (long[] pair : timePairs) {
			MarzulloTuple[] startEndTuples = MarzulloTuple.getTuples(pair);
			tuples.add(startEndTuples[0]);
			tuples.add(startEndTuples[1]);
		}

		Collections.sort(tuples);

		for (int i = 0; i < tuples.size(); ++i) {
			currentNumberOfIntervals = currentNumberOfIntervals - tuples.get(i).getType();
			if (currentNumberOfIntervals > maxNumberOfIntervals) {
				maxNumberOfIntervals = currentNumberOfIntervals;
				bestStart = tuples.get(i).getOffset();
				bestEnd = tuples.get(i + 1).getOffset();
			}
		}

		return new long[] { bestStart, bestEnd };
	}

	public static String getIpNodo1() {
		return ipNodo1;
	}

	public static void setIpNodo1(String ipNodol) {
		Supervisor.ipNodo1 = ipNodol;
	}

	public static String getIpNodo2() {
		return ipNodo2;
	}

	public static void setIpNodo2(String ipNodo2) {
		Supervisor.ipNodo2 = ipNodo2;
	}

	public static String getIpNodo3() {
		return ipNodo3;
	}

	public static void setIpNodo3(String ipNodo3) {
		Supervisor.ipNodo3 = ipNodo3;
	}

	public static String getSupervisor() {
		return supervisor;
	}

	public static void setSupervisor(String supervisor) {
		Supervisor.supervisor = supervisor;
	}

}
