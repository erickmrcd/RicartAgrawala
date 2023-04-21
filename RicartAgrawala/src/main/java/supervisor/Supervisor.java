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
 *
 */
public class Supervisor {

	private static final String SYNCHRONIZATION_ENDPOINT = null;
	private static RestHandler restHandler;
	private static  List<ClientUID> clients;
	private static List<String> logFilenames = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		Map<ClientUID, long[]> bestEstimations = new HashMap<>();
		Map<ClientUID, List<long[]>> estimations = new HashMap<>();
		clients = new ArrayList<>();
		clients.add(new ClientUID("192.168.1.136",0));
		clients.add(new ClientUID("192.168.1.136",1));
		clients.add(new ClientUID("192.168.1.136",2));
		restHandler = new RestHandler(String.format("http://192.168.1.136:8080/RicartAgrawala", "localhost"));
		restHandler.callWebService(
				"/supervisor/setup",
				new RESTParameter("num_clients", String.valueOf(clients.size()))
		);
		try {
			runEstimations(clients,estimations);
		} catch (SynchronizationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		for (ClientUID currentUID : clients) {
			bestEstimations.put(currentUID, marzullo(estimations.get(currentUID)));
		}
		
		for (String filename : logFilenames) {
			long[] offsetBounds = bestEstimations.get(ClientUID.fromUniqueFilename(filename));
			Logging.normalizeLog(Server.UPLOAD_LOCATION + File.separator + filename,
					             offsetBounds);
		}
		
		try {
			runEstimations(clients,estimations);
		} catch (SynchronizationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		for (ClientUID currentUID : clients) {
			bestEstimations.put(currentUID, marzullo(estimations.get(currentUID)));
		}
		
		for (String filename : logFilenames) {
			long[] offsetBounds = bestEstimations.get(ClientUID.fromUniqueFilename(filename));
			Logging.normalizeLog(Server.UPLOAD_LOCATION + File.separator + filename,
					             offsetBounds);
		}
		
		Logging.mergeLogs(logFilenames, "Client.log");
	}
		

	private static void runEstimations(List<ClientUID> clients, Map<ClientUID, List<long[]>> estimations)
			throws SynchronizationException
	{
		for (ClientUID currentID : clients){
			// Add IP to Map
			if (! estimations.keySet().contains(currentID)) {
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
		RestHandler webUtils = new RestHandler(String.format("http://192.168.1.136:8080/RicardAgrawala", currentID.getIpAddress()));
			
		// Clear the last server's data
		offsetDelayPairs.clear();
		for (int i = 0; i < 10; ++i) {
			// Delivery time, 't0'
			timeStamps[0] = System.currentTimeMillis();
			
			// Receive server's receipt and delivery times, 't1' and 't2'
			long[] serverTimes = ntpSync(
					webUtils,
					currentID,
					(i == 10 - 1) ? true : false
			);
			if (null == serverTimes) {
				return null;
			}
			
			// Receipt time, 't3'
			timeStamps[3] = System.currentTimeMillis();
			
			// Add 't1' and 't2' to array
			timeStamps[1] = serverTimes[0];
			timeStamps[2] = serverTimes[1];
			
			// Calculate offset and delay
			offsetDelayPairs.add(new long[] {
					SynchronizationUtils.calculateOffset(timeStamps),
					SynchronizationUtils.calculateDelay(timeStamps)
			});
		}
		
		return offsetDelayPairs;
	}


	private static long[] ntpSync(RestHandler webUtils, ClientUID clientID, boolean lastSync) {
		String delimiter = "#";
		long[] requestPair = new long[10];
		
		// Send request
		String timesString = webUtils.callWebService(
				MediaType.TEXT_PLAIN,
				"/rest/synchronize",
				new RESTParameter[] {
						new RESTParameter("id", clientID.toUniqueFilename()),
						new RESTParameter("finished", String.valueOf(lastSync))
				}
		);
		
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
		
		for (int i = 0; i < tuples.size() ; ++i) {
			currentNumberOfIntervals = currentNumberOfIntervals - tuples.get(i).getType();
			if (currentNumberOfIntervals > maxNumberOfIntervals) {
				maxNumberOfIntervals = currentNumberOfIntervals;
				bestStart = tuples.get(i).getOffset();
				bestEnd = tuples.get(i + 1).getOffset();
			}
		}
				
		return new long[] {bestStart, bestEnd};
	}
	
	
}
