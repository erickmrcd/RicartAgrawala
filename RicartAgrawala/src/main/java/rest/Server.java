package rest;

import client.ClientUID;
import clientData.ClientData;
import clientData.CriticalSectionState;
import clientData.LamportClock;
import clientData.LamportTime;
import utils.RESTParameter;
import utils.RestHandler;

import java.util.List;
import java.util.Map;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

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
	private static int numTotalClients = numLocalClients;
	
	private static Map<ClientUID, ClientData> localClients;

	private static CyclicBarrier startClientBarrier = null;

	

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("reset")
	public Response resetServer() {
		localClients = new HashMap<ClientUID, ClientData>();
		localClients.clear();
		startClientBarrier = new CyclicBarrier(numLocalClients);
		
		return Response.status(Response.Status.OK)
				.entity(String.format("SUCCESS: Server restarted")).build();
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("/setup_num")
	public Response setupNumClients(@QueryParam(value="numLocal")String localNumClients,
			                        @QueryParam(value="numTotal")String totalNumClients) {
		numLocalClients = Integer.parseInt(localNumClients);
		numTotalClients = Integer.parseInt(totalNumClients);
		//System.out.println(numTotalClients);
		return Response.status(Response.Status.OK)
				.entity(String.format("SUCCESS: New configuration set in server")).build();
	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("start")
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
	@Path("registrar")
	public Response registrar(@QueryParam(value = "id") String id) {
		ClientUID uid = paramToUID(id);

		// Avoid registering duplicates
		if (localClients.containsKey(uid)) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(String.format("ERROR: process with given ID (%s) already registered in server", id))
					.build();
		}

		// Add client to map registry
		localClients.put(uid, new ClientData());

		return Response.status(Response.Status.OK)
				.entity(String.format("SUCCESS: Registered process %s with GUID %d", id, uid.getClientID())).build();
	}
	
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("/wait_synchronize")
	public Response waitSynchronize(@QueryParam(value="id") String id) {
		
		// Get requesting client information
		ClientData clientData = localClients.get(paramToUID(id));
		
		if (null == clientData){
			return Response.status(Response.Status.NOT_FOUND)
					.entity(String.format("ERROR: process with given ID (%s) does not exist", id)).build();
		}
		
		// Wait for supervisors release
		try {
			clientData.getWaitSynchronizeSemaphore().acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return Response.status(Response.Status.OK)
				.entity(String.format("SUCCESS: NTP Synchronization finished")).build();
	}
	
	@GET
	@Path("send_request")
	public Response sendRequest(@QueryParam(value = "id") String id) {
		ClientUID uid = paramToUID(id);
		ClientData clientData = localClients.get(uid);

		Request request = updateStateAndTimestamp(uid, clientData);
		multicastRequest(uid, request);
		waitResponses(id, clientData);
		enterCriticalSection(clientData);
		return Response.status(Response.Status.OK).entity(String.format("SUCCESS", id)).build();

	}

	@GET
	@Path("receive_request")
	public void receiveRequest(@QueryParam(value = "id") String id, @QueryParam(value = "request") String req) {
		ClientUID uid = paramToUID(id);
		ClientData clientData = localClients.get(uid);
		Request request = null;

		clientData.writeLock();
		try {
			request = Request.parse(req);
		} catch (java.text.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		updateLamportClock(clientData, request);
		CriticalSectionState state = clientData.getStateThreadUnsafe();
		Request requestToCompare = new Request(clientData.getRequestAccessTimestamp(), uid);

		if (state == CriticalSectionState.BUSY
				|| state == CriticalSectionState.REQUESTING && Request.compare(requestToCompare, request) <= 0) {
			clientData.addToQueueThreadUnsafe(request);
			clientData.writeUnlock();
		}else {
			clientData.writeUnlock();
			answerClient(request.getClientId());
		}
	}
	
	@GET
	@Path("/receive_grant")
	public void receiveGrant(@QueryParam(value="id") String id) {
		ClientUID uid = paramToUID(id);
		ClientData clientData = localClients.get(uid);
		
		clientData.writeLockPermissions(); // Between Lock and unlock is thread safe. Atomic operation
		
			// Adds one unit to number of permissions the current client needs from other processes to enter the critical section
			clientData.addPermissionThreadUnsafe();
			
			// When all the clients have answered the current one, this notifies the structure for waiting responses to realase the thread
			if (clientData.getPermissionsThreadUnsafe() == numTotalClients - 1) {
				
				// Resets the number of permission for this client to 0
				clientData.setPermissionsThreadUnsafe(0);
				
				// Decreases the value of the CountDownLatch in one unit. When it reaches value 0 the process is liberated
				clientData.getWaitForResponsesStructure().countDown();
			}
			
		clientData.writeUnlockPermissions(); // Between Lock and unlock is thread safe. Atomic operation
	}
	
	@GET
	@Path("/exit")
	public Response exitCriticalSection(@QueryParam(value="id") String id) {
		
		// Get requesting client information
		ClientUID uid = paramToUID(id);
		ClientData clientData = localClients.get(uid);
		
		// Get all requests in client�s queue
		clientData.writeLock(); // Between Lock and unlock is thread safe. Atomic operation
		List<Request> queuedRequests = getAllRequestsFromQueue(clientData);
			
		clientData.writeUnlock(); // Between Lock and unlock is thread safe. Atomic operation
				
		// Answer all requests in queue
		answerRequestsInQueue(queuedRequests);
		
		return Response.status(Response.Status.OK)
				.entity(String.format("SUCCESS: the client '%s' exited the critical section", id)).build();
	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("/synchronize")
	public String synchronize(
			@QueryParam(value="id")String id,
			@QueryParam(value="finished")boolean finished)
	{
		long receipt_time = System.currentTimeMillis();
		String response = String.format("%d#%d", receipt_time, System.currentTimeMillis());
		
		// Reached 10th time in loop (release client from wait_synchronize state)
		if (finished) {
			ClientData clientData = localClients.get(paramToUID(id));
			clientData.getWaitSynchronizeSemaphore().release();
		}
		
		return response;
	}
	
	private void answerRequestsInQueue(List<Request> queuedRequests) {
		// TODO Auto-generated method stub
		for (Request request : queuedRequests) {
			ClientUID destinationUID = request.getClientId();
			answerClient(destinationUID);
		}	
	}

	private List<Request> getAllRequestsFromQueue(ClientData clientData) {
		clientData.setStateThreadUnsafe(CriticalSectionState.FREE);
		return clientData.removeAllFromQueueThreadUnsafe();
	}

	private void answerClient(ClientUID clientId) {
		// TODO Auto-generated method stub
		RestHandler connectionHandler = new RestHandler(String.format("http://localhost:8080/RicardAgrawala", clientId.getIpAddress()));
		connectionHandler.callWebService(MediaType.TEXT_PLAIN, 
				"/rest/receive_grant",
				new RESTParameter("id", clientId.toUniqueFilename()));
		
	}

	private void updateLamportClock(ClientData clientData, Request request) {
		// TODO Auto-generated method stub
		LamportTime maxTimestamp = LamportTime.max(clientData.getLamportClockThreadUnsafe().getTime(), request.getTimestamp());
		clientData.setLamportClockThreadUnsafe(new LamportClock(new LamportTime(maxTimestamp.getValue() + 1)));
	}

	private void enterCriticalSection(ClientData clientData) {
		// TODO Auto-generated method stub
		clientData.writeLock();
		clientData.setStateThreadUnsafe(CriticalSectionState.BUSY);
		clientData.increaseLamportClockThreadUnsafe();
		clientData.writeUnlock();
	}

	private void waitResponses(String id, ClientData clientData) {
		// TODO Auto-generated method stub
		try {
			clientData.getWaitForResponsesStructure().await();
			clientData.resetWaitForResponsesStructure();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void multicastRequest(ClientUID uid, Request request) {
		// TODO Auto-generated method stub
		RestHandler connection;

		connection = new RestHandler(String.format("http://localhost:8080/RicardAgrawala", "localhost"));
		for (ClientUID localClient : localClients.keySet()) {
			if (localClient.equals(uid)) {
				continue;
			}
			connection.callWebService(MediaType.TEXT_PLAIN, "rest/receive_request",
					new RESTParameter[] { new RESTParameter("id", localClient.toUniqueFilename()),
							new RESTParameter("request", request.toString()) });
		}

	}

	private Request updateStateAndTimestamp(ClientUID uid, ClientData clientData) {
		// TODO Auto-generated method stub
		clientData.writeLock();
		clientData.setStateThreadUnsafe(CriticalSectionState.REQUESTING);
		LamportTime time = clientData.getLamportClockThreadUnsafe().getTime();
		clientData.setRequestAccessTimestamp(time);
		clientData.writeUnlock();
		return new Request(time, uid);
	}
	
	private ClientUID paramToUID(String id) {
		// TODO Auto-generated method stub
		ClientUID uid = null;
		try {
			uid = ClientUID.fromUniqueFilename(id);
		} catch (java.text.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return uid;
	}

}
