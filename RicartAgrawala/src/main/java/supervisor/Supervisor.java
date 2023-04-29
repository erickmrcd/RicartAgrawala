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

import client.ClientUID;
import logging.Logging;
import utils.RestHandler;
import utils.SynchronizationException;
import utils.SynchronizationUtils;
import utils.MarzulloTuple;
import utils.RESTParameter;

/**
 * @author erick
 * @author Daniel
 */
public class Supervisor {

	private static RestHandler restHandler;
	private static List<ClientUID> clients;
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
		if (args.length < 1)
			System.exit(0);

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
		Map<ClientUID, long[]> bestEstimations = new HashMap<>();
		Map<ClientUID, List<long[]>> estimations = new HashMap<>();
		clients = new ArrayList<>();
		System.out.println(numClientes);
		for (int i = 0; i < numClientes; i++) {
			if (numClientes == 2) {
				clients.add(new ClientUID(getIpNodo1(), i));
			} else if (numClientes == 4) {
				if (i < 2) {
					clients.add(new ClientUID(getIpNodo1(), i));
				} else {
					clients.add(new ClientUID(getIpNodo2(), i));
				}
			} else {
				if (i < 2) {
					clients.add(new ClientUID(getIpNodo1(), i));
				} else if (i >= 2 && i < 4) {
					clients.add(new ClientUID(getIpNodo2(), i));
				} else {
					clients.add(new ClientUID(getIpNodo3(), i));
				}
			}
		}

		restHandler = new RestHandler(getSupervisor());
		// System.out.println(getSupervisor());
		restHandler.callWebService("/supervisor/setup",
				new RESTParameter("num_clients", String.valueOf(clients.size())));
		try {
			runEstimations(clients, estimations);
		} catch (SynchronizationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		String s = restHandler.callWebService(MediaType.TEXT_PLAIN, "/supervisor/collect_logs");
		logFilenames = Arrays.asList(s.split(";"));
		System.out.println("Collected log files.");

		try {
			runEstimations(clients, estimations);
		} catch (SynchronizationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		// marzullo
		for (ClientUID currentUID : clients) {
			bestEstimations.put(currentUID, marzullo(estimations.get(currentUID)));
		}

		for (String filename : logFilenames) {
			long[] offsetBounds = bestEstimations.get(ClientUID.fromUniqueFilename(filename));
			Logging.normalizeLog(Server.UPLOAD_LOCATION + File.separator + filename, offsetBounds);
		}

		Logging.mergeLogs(logFilenames, "clients.log");

		System.out.println(String.format("Log files check result: '%s'",
				Logging.checkLog("clients.log") ? "Successful execution" : "Critical section violation"));

		Scanner scanner = new Scanner(System.in);
		System.out.print("Execution of Supervisor finished. Press any key to close...");
		scanner.nextLine();
		scanner.close();
	}

	private static void runEstimations(List<ClientUID> clients, Map<ClientUID, List<long[]>> estimations)
			throws SynchronizationException {
		for (ClientUID currentID : clients) {
			// Add IP to Map
			if (!estimations.keySet().contains(currentID)) {
				estimations.put(currentID, new ArrayList<long[]>());
			}

			// Estimate offset values
			List<long[]> currentEstimation = estimateOffset(currentID);
			if (null == currentEstimation) {

				throw new SynchronizationException();
			}

			// Stores estimations for the current client
			for (long[] pair : currentEstimation) {
				estimations.get(currentID).add(pair);
			}
		}
	}

	private static List<long[]> estimateOffset(ClientUID currentID) {
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
			offsetDelayPairs.add(new long[] { SynchronizationUtils.calculateOffset(timeStamps),
					SynchronizationUtils.calculateDelay(timeStamps) });
		}

		return offsetDelayPairs;
	}

	private static long[] ntpSync(RestHandler webUtils, ClientUID clientID, boolean lastSync) {
		String delimiter = "#";
		long[] requestPair = new long[10];
		// Send request
		String timesString = webUtils.callWebService(MediaType.TEXT_PLAIN, "/rest/synchronize",
				new RESTParameter[] { new RESTParameter("id", clientID.toUniqueFilename()),
						new RESTParameter("finished", String.valueOf(lastSync)) });

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
