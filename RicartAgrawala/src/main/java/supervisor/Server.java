/**
 * 
 */
package supervisor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;
import client.ClientUID;
import utils.SynchronizationException;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Erick
 * @author Daniel
 */

@Singleton
@Path("/supervisor")
public class Server {

	public static final String UPLOAD_LOCATION = System.getProperty("user.home") + File.separator + "tmp"
			+ File.separator + "supervisor";

	public static final String UPLOAD_LOCATION_2 = System.getProperty("user.home") + File.separator + "tmp"
			+ File.separator + "comprobador";

	static {
		File f = new File(UPLOAD_LOCATION);
		if (!f.isDirectory()) {
			f.mkdirs();
		}

		File f2 = new File(UPLOAD_LOCATION_2);
		if (!f2.isDirectory()) {
			f2.mkdirs();
		}
	}

	private Semaphore availableLogsSemaphore = new Semaphore(0);
	private static List<ClientUID> finishedClients = new ArrayList<>();
	private static int NUMBER_OF_CLIENTS_WAITING;

	@GET
	@Path("/setup")
	public Response setup(@QueryParam(value = "num_clients") int numberOfClients) {
		NUMBER_OF_CLIENTS_WAITING = numberOfClients;
		return Response.status(200).entity("OK").build();
	}

	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("/inform")
	public Response inform(@QueryParam(value = "filename") String filename,
			@QueryParam(value = "streamlog") String log) throws SynchronizationException, IOException {
		
		ClientUID clientUID = null;
		try {
			clientUID = ClientUID.fromUniqueFilename(null);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// Duplicate if already true
		if (finishedClients.contains(clientUID)) {
			throw new SynchronizationException(String.format("Client %s had already finished", clientUID.toString()));
		}
		
		//writeToFile(array);

		finishedClients.add(clientUID);
		if (finishedClients.size() == NUMBER_OF_CLIENTS_WAITING) {
			availableLogsSemaphore.release();
		}
		String output = "File was successfully uploaded.";
		return Response.status(200).entity(output).build();
	}


	@GET
	@Path("/collect_logs")
	@Produces(MediaType.TEXT_PLAIN)
	public String collectLogs() {
		try {
			availableLogsSemaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < finishedClients.size() - 1; ++i) {
			sb.append(finishedClients.get(i).toUniqueFilename("log") + ";");
		}
		sb.append(finishedClients.get(finishedClients.size() - 1).toUniqueFilename("log"));

		return sb.toString();
	}

	@SuppressWarnings("unused")
	private void writeToFile(InputStream uploadedInputStream, String uploadedFileLocation) {
		try {
			OutputStream os = new FileOutputStream(new File(uploadedFileLocation));
			int read = 0;
			byte[] bytes = new byte[1024];

			os = new FileOutputStream(new File(uploadedFileLocation));
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				os.write(bytes, 0, read);
			}
			os.flush();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
