/**
 * 
 */
package supervisor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.io.OutputStream;
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

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * @author Erick
 * @author Daniel
 */

@Singleton
@Path("/supervisor")
public class Server {
	private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
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
		LOGGER.info("Set up number of clients: " + numberOfClients);
		System.out.println(NUMBER_OF_CLIENTS_WAITING);
		return Response.status(200).entity("OK").build();
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
	
	@POST
	@Path("/inform")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response inform(
			@FormDataParam("file") InputStream is,
			@FormDataParam("file") FormDataContentDisposition fdcd) throws SynchronizationException, IOException
	{
		ClientUID clientUID = null;
		try {
			clientUID = ClientUID.fromUniqueFilename(fdcd.getFileName());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		LOGGER.log(Level.INFO, String.format(
				"Client finished: {%s, %s}",
				clientUID.toUniqueFilename(),
				fdcd.getFileName())
		);
		
		// Duplicate if already true
		if (finishedClients.contains(clientUID)) {
			throw new SynchronizationException(String.format("Client %s had already finished", clientUID.toString()));
		}
		
		// Create directory if it does not exists
		LOGGER.info("Checking file " + System.getProperty("user.dir") + File.separator + UPLOAD_LOCATION);
		if (! Files.exists(Paths.get(UPLOAD_LOCATION), LinkOption.NOFOLLOW_LINKS)) {
			throw new IOException(String.format("Destination directory '%s' does not exists", UPLOAD_LOCATION));
		}
		String uploadedFileLocation = UPLOAD_LOCATION + File.separator + fdcd.getFileName();
		writeToFile(is, uploadedFileLocation);

		finishedClients.add(clientUID);
		if (finishedClients.size() == NUMBER_OF_CLIENTS_WAITING) {
			availableLogsSemaphore.release();
		}
		String output = "File was successfully uploaded.";
		return Response.status(200).entity(output).build();
	}



	
	private void writeToFile(InputStream is, String uploadedFileLocation) {
		LOGGER.info("Writing file '" + uploadedFileLocation + "'");
		try {
			OutputStream os = new FileOutputStream(new File(uploadedFileLocation));
			int read = 0;
			byte[] bytes = new byte[1024];

			os = new FileOutputStream(new File(uploadedFileLocation));
			while ((read = is.read(bytes)) != -1) {
				os.write(bytes, 0, read);
			}
			os.flush();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
