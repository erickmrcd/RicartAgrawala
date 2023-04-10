package rest;

import client.ClientUID;
import clientData.ClientData;

import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import javax.inject.Singleton;
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
	
	private static Map<ClientUID,ClientData> localClients;
	
	private static CyclicBarrier startClientBarrier = new CyclicBarrier(2);
	
	@GET
	@Produces(MediaType.TEXT_PLAIN) 
	@Path("reset") 
	public String reinicio() 
	{
		startClientBarrier.reset();
		return "reiniciado";

	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN) 
	@Path("wait")
	public String waitOthers() {
		try {
			startClientBarrier.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "inicio";
	}
	
	@GET
	@Path("/send_request")
	public String sendRequest(@QueryParam(value="id")String id) {
		
		return null;
		
	}
}
