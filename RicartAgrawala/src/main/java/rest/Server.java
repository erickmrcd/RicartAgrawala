package rest;

import client.ClientIdentifier;
import clientData.RicartClientData;
import clientData.State;
import clientData.LamportClock;
import clientData.LamportTime;
import utils.RestParameter;
import utils.RestHandler;

import java.util.List;
import java.util.Map;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Logger;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.ws.rs.Produces;

@Path("/rest")
@Singleton
public class Server {
	private static int numLocalClients = -1;
	private static int numTotalClients = -1;
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private static Map<ClientIdentifier, RicartClientData> localClients;
	private static Map<String, List<ClientIdentifier>> remoteClients = new HashMap<>();
	private static CyclicBarrier startClientBarrier = null;
	

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("/reset")
	public Response resetServer() {
		localClients = new HashMap<ClientIdentifier, RicartClientData>();
		localClients.clear();
		startClientBarrier = new CyclicBarrier(numLocalClients);

		return Response.status(Response.Status.OK).entity(String.format("SUCCESS: Server restarted")).build();
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("/setup_local")
	public Response setupLocal(@QueryParam(value = "numLocal") String localNumClients,
			@QueryParam(value = "numTotal") String totalNumClients) {
		numLocalClients = Integer.parseInt(localNumClients);
		numTotalClients = Integer.parseInt(totalNumClients);
		
		return Response.status(Response.Status.OK).entity(String.format("SUCCESS: New configuration set in server"))
				.build();
	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("/setup_remote")
	public Response setupRemote(@QueryParam(value="ip")String ip,
			                           @QueryParam(value="cliente1")String c1,
			                           @QueryParam(value="cliente2")String c2) {
		
		List<ClientIdentifier> c = new ArrayList<>();
		
			c.add(new ClientIdentifier(ip, Integer.parseInt(c2)));
			c.add(new ClientIdentifier(ip, Integer.parseInt(c1)));

		
		remoteClients.put(ip, c);
		
		return Response.status(Response.Status.OK)
				.entity(String.format("SUCCESS: Registered clients from node %s", ip)).build();
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("/start")
	public Response iniciar() {
		try {
			startClientBarrier.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.status(Response.Status.OK).entity(String.format("Success")).build();
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("/registrar")
	public Response registrar(@QueryParam(value = "id") String id) {
		ClientIdentifier uid = parameterToIdentifier(id);

		// Avoid registering duplicates
		if (localClients.containsKey(uid)) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("ERROR: process with given ID (%s) already registered in server", id))
					.build();
		}

		// Add client to map registry
		localClients.put(uid, new RicartClientData());

		return Response.status(Response.Status.OK)
				.entity(String.format("SUCCESS: Registered process %s with GUID %d", id, uid.getClientID())).build();
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("/wait_synchronize")
	public Response waitSynchronize(@QueryParam(value = "id") String id) {

		// Get requesting client information
		RicartClientData clientData = localClients.get(parameterToIdentifier(id));

		if (null == clientData) {
			return Response.status(Response.Status.NOT_FOUND)
					.entity(String.format("ERROR: process with given ID (%s) does not exist", id)).build();
		}

		// Wait for supervisors release
		try {
			clientData.getWaitSynchronizeSemaphore().acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return Response.status(Response.Status.OK).entity(String.format("SUCCESS: NTP Synchronization finished"))
				.build();
	}

	@GET
	@Path("/send_request")
	public Response sendRequest(@QueryParam(value = "id") String id) {
		System.out.println("\n\n");
		LOGGER.info(String.format("[Client '%s'] requests access to critical section", id));
		ClientIdentifier uid = parameterToIdentifier(id);
		RicartClientData clientData = localClients.get(uid);
		if (null == clientData) {
			LOGGER.info(String.format("[Client '%s'] No existe", uid.getClientID()));
			return Response.status(Response.Status.BAD_GATEWAY)
					.entity(String.format("ERROR: the client with client id (%d) does not exist", uid.getClientID()))
					.build();
		}
		RestRequest request = updateStateAndTimestamp(uid, clientData);
		LOGGER.info(String.format("[Client '%s'] Sending request to rest of clients [timestamp#uniqueFilename]: %s", id, request.toString()));
		multicastRequest(uid, request);
		LOGGER.info(String.format("[Client '%s'] Waiting for responses from other clients", id));
		waitResponses(id, clientData);
		LOGGER.info(String.format("[Client '%s'] Entering critical section", id));
		enterCriticalSection(clientData)
		;
		return Response.status(Response.Status.OK).entity(String.format("SUCCESS: the client '%s' is ready to enter the critical section ", id)).build();

	}

	@GET
	@Path("/receive_request")
	public void receiveRequest(@QueryParam(value = "id") String id, @QueryParam(value = "request") String req) {
		ClientIdentifier uid = parameterToIdentifier(id);
		RicartClientData clientData = localClients.get(uid);
		RestRequest request = null;
		clientData.writeLock();
		
		try {
			request = RestRequest.parse(req);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		updateLamportClock(clientData, request);
		State state = clientData.getStateOfThread();
		RestRequest requestToCompare = new RestRequest(clientData.getRequestAccessTimestamp(), uid);

		if (state == State.OCUPADA
				|| state == State.REQUERIDA && RestRequest.compare(requestToCompare, request) <= 0) {
			clientData.addToQueueThreadUnsafe(request);
			clientData.writeUnlock();
		} else {
			clientData.writeUnlock();
			LOGGER.info(String.format("Client '%s' ANSWERS. Queue NOT updated", id));
			answerClient(request.getClientId());
			LOGGER.info(clientData.printQueue());
		}
	}

	@GET
	@Path("/receive_grant")
	public void receiveGrant(@QueryParam(value = "id") String id) {
		ClientIdentifier uid = parameterToIdentifier(id);
		RicartClientData clientData = localClients.get(uid);

		clientData.writeLockPermissions(); 
		clientData.addPermissionThread();
		if (clientData.getPermissionsThread() == numTotalClients - 1) {
			clientData.setPermissionsThread(0);
			clientData.getWaitForResponsesStructure().countDown();
		}

		clientData.writeUnlockPermissions(); // Between Lock and unlock is thread safe. Atomic operation
	}

	@GET
	@Path("/exit")
	public Response exitCriticalSection(@QueryParam(value = "id") String id) {

		// Get requesting client information
		ClientIdentifier uid = parameterToIdentifier(id);
		RicartClientData clientData = localClients.get(uid);
		List<RestRequest> queuedRequests = getAllRequestsFromQueue(clientData);

		clientData.writeUnlock(); 
		answerRequestsInQueue(queuedRequests);

		return Response.status(Response.Status.OK)
				.entity(String.format("SUCCESS: the client '%s' exited the critical section", id)).build();
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("/synchronize")
	public String synchronize(@QueryParam(value = "id") String id, @QueryParam(value = "finished") boolean finished) {
		long receipt_time = System.currentTimeMillis();
		String response = String.format("%d#%d", receipt_time, System.currentTimeMillis());
		
		// Reached 10th time in loop (release client from wait_synchronize state)
		if (finished) {
			RicartClientData clientData = localClients.get(parameterToIdentifier(id));
			clientData.getWaitSynchronizeSemaphore().release();
		}

		return response;
	}

	
	private void answerRequestsInQueue(List<RestRequest> queuedRequests) {
		for (RestRequest request : queuedRequests) {
			ClientIdentifier destinationUID = request.getClientId();
			answerClient(destinationUID);
		}
	}

	

	private void answerClient(ClientIdentifier clientId) {
		RestHandler connectionHandler = new RestHandler(
				String.format("http://%s:8080/RicartAgrawala", clientId.getIpAddress()));
		connectionHandler.callWebService(MediaType.TEXT_PLAIN, "/rest/receive_grant",
				new RestParameter("id", clientId.toUniqueIdentifier()));
	}
	
	private List<RestRequest> getAllRequestsFromQueue(RicartClientData clientData) {
		clientData.setStateOfThread(State.LIBRE);
		return clientData.removeAllFromQueueThreadUnsafe();
	}
	
	private void updateLamportClock(RicartClientData clientData, RestRequest request) {
		LamportTime maxTimestamp = LamportTime.max(clientData.getLamportClockThread().getTime(),
				request.getTimestamp());
		clientData.setLamportClockThread(new LamportClock(new LamportTime(maxTimestamp.getValue() + 1)));
	}

	private void enterCriticalSection(RicartClientData clientData) {
		clientData.writeLock();
			clientData.setStateOfThread(State.OCUPADA);
			clientData.increaseLamportClockThread();
		clientData.writeUnlock();
	}

	private void waitResponses(String id, RicartClientData clientData) {
		try {
			clientData.getWaitForResponsesStructure().await();
			clientData.resetWaitForResponsesStructure();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void multicastRequest(ClientIdentifier uid, RestRequest request) {
		// TODO Auto-generated method stub
		RestHandler connection;

		for (String ipAddress : remoteClients.keySet()) {
			connection =  new RestHandler(String.format("http://%s:8080/RicartAgrawala", ipAddress));
			for (ClientIdentifier client : remoteClients.get(ipAddress)) {
				connection.callWebService(MediaType.TEXT_PLAIN,
						"/rest/receive_request",
						new RestParameter[] {
								new RestParameter("id", client
										.toUniqueIdentifier()),
								new RestParameter("request", request.toString())
						}
				);
			}
		}
		
		connection = new RestHandler(String.format("http://"+uid.getIpAddress()+":8080/RicartAgrawala", "localhost"));
		for (ClientIdentifier localClient : localClients.keySet()) {
			if (localClient.equals(uid)) {
				continue;
			}
			connection.callWebService(MediaType.TEXT_PLAIN, "/rest/receive_request",
					new RestParameter[] { new RestParameter("id", localClient.toUniqueIdentifier()),
							new RestParameter("request", request.toString()) });
		}

	}

	private RestRequest updateStateAndTimestamp(ClientIdentifier uid, RicartClientData clientData) {
		// TODO Auto-generated method stub
		clientData.writeLock();
		
			clientData.setStateOfThread(State.REQUERIDA);
			LamportTime time = clientData.getLamportClockThread().getTime();
			clientData.setRequestAccessTimestamp(time);
			
		clientData.writeUnlock();
		
		return new RestRequest(time, uid);
	}

	private ClientIdentifier parameterToIdentifier(String id) {
		// TODO Auto-generated method stub
		ClientIdentifier uid = null;
		try {
			uid = ClientIdentifier.fromUniqueIdentifier(id);
		} catch (java.text.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return uid;
	}

}
