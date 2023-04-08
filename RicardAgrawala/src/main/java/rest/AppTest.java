package rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class AppTest {	
	@GET
	@Path("/startup_check")
	@Produces(MediaType.TEXT_HTML)
	public String startupCheck() {
		return "<html><head><title>Success!</title></head>"
				+ "<body><h1>This application has been successfully deployed</h1>"
				+ "<p>Success.</p></body></html>";
	}
}
