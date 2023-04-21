package logging;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;

import client.ClientUID;
import supervisor.Server;

/**
 * 
 * @author Erick
 * @author Dani
 *
 */
public final class Logging {

	private static String logFile;
	private final static String LOG_LINE_FORMAT = "P%s %s %s";
	private final static String COMPROBADOR_LOGS_DIRECTORY = 
			System.getProperty("user.home")
			+ File.separator
			+ "tmp"
			+ File.separator
			+ "comprobador";

	public static void normalizeLog(String string, long[] offsetBounds) {
		// TODO Auto-generated method stub
		
		List<String> fileLines = null;
		StringBuffer newContent = new StringBuffer();  // Buffer of the content
		StringBuffer comprobadorFileContent = new StringBuffer();
		String[] lineParts = null;
		long temp = Long.MAX_VALUE;
		
		// ---------------------------------------------------------------------------------
		// To create Comprobador.java files
		long[] offsetAndDelay = new long[2];
		offsetAndDelay[0] = (offsetBounds[1] + offsetBounds[0])/2;
		offsetAndDelay[1] = (offsetBounds[1] - offsetBounds[0]);
		
		// Reads content from client�s log
		try {
			fileLines = Files.readAllLines(Paths.get(logFile), Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// For each line normalizes times from the machines of the clients 
		// and the time of the machine containing the supervisor
		for (String line : fileLines) {
			try {
				lineParts = digestLogFileLine(line);
				temp = Long.parseLong(lineParts[2]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			
			Timestamp timestamp = new Timestamp(temp - offsetBounds[0], temp - offsetBounds[1]);
			
			// Append line of file into buffer
			newContent.append(
					logMessage(
							lineParts[0],
							(lineParts[1].equals("E") ? "enter" : "exit"),
							String.format("%s", timestamp.toString())
					)
			);

			// ---------------------------------------------------------------------------------
			// To create Comprobador.java files
			long localtimePlusOffset = temp + offsetAndDelay[0];
			comprobadorFileContent.append(
					logMessage(
							lineParts[0],
							(lineParts[1].equals("E") ? "enter" : "exit"),
							String.format("%d", localtimePlusOffset)
					)
			);	
		}
		
		// Writes the content back to the same log file but with normalized times. The supervisor will lately take it
		try(FileWriter fw = new FileWriter(logFile))
		{
			fw.write(new String(newContent));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// ---------------------------------------------------------------------------------
		// To create Comprobador.java files
		Path filename = Paths.get(logFile).getFileName();
		try(FileWriter fw = 
				new FileWriter(
						Logging.COMPROBADOR_LOGS_DIRECTORY + File.separator + filename.toString() + offsetAndDelay[1]
				)
		) {
			fw.write(new String(comprobadorFileContent));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String logMessage(String pid, String type, String timestamp) {
		return String.format(
				LOG_LINE_FORMAT + "\n",
				pid,
				(type.equals("enter")) ? "E" : "S",
				timestamp
		);
	}
	
	public static String logMessage(int pid, String type, int timestamp) {
		return logMessage(
			String.format("%d", pid),
			type,
			String.format("%d", timestamp)
		);
	}
	
	public static String logMessage(int pid, String type) {
		return logMessage(
				String.format("%d", pid), type	);
	}

	public static String logMessage(String pid, String type) {	
		return String.format(
				Logging.LOG_LINE_FORMAT + "\n",
				pid,
				(type.equals("Enter")) ? "E" : "S",
				String.format("%d", System.currentTimeMillis())
		);
	}
	
	public static void doLog(ClientUID guid, String fileContent) {
		String filename = guid.toUniqueFilename("log");
		
		// Writes content from buffer into filename of the client with given GUID
		try(FileWriter fw = new FileWriter(filename)) {
			fw.write(fileContent);
		} catch (IOException e) {
		}
	}

	private static String[] digestLogFileLine(String logFileLine) {
		String[] lineParts = logFileLine.split(" ");
		lineParts[0] = lineParts[0].substring(1);
		return lineParts;
	}

	public static void mergeLogs(List<String> filenames, String mergedFilename) throws IOException {
		// Load files' lines
		List<List<String>> filesAsLines = new ArrayList<>();
		for (String filename : filenames) {
			filesAsLines.add(Files.readAllLines(
					Paths.get(Server.UPLOAD_LOCATION + File.separator + filename),
					Charset.defaultCharset()
			));
		}
		
		// Create iterators for lines
		List<ListIterator<String>> fileLinesIterators = new ArrayList<>();
		for (List<String> fileLines : filesAsLines)
			fileLinesIterators.add(fileLines.listIterator());
		
		// Create a variable to store current examined lines
		List<String> currentLines = new ArrayList<>();
		for (ListIterator<String> i : fileLinesIterators)
			currentLines.add(i.next());
		
		// Variable to store definitive file
		List<String> mergedFileLines = new ArrayList<>();
		
		// While not break
		int filesLeft = filenames.size();
		while(filesLeft > 1) {
			
			int leastTimestampLineIndex = -1;
			Timestamp leastTimestamp = null, tempTimestamp = null;
			
			// Find starting line
			for(int i = 0; i < currentLines.size(); ++i) {
				if (currentLines.get(i) != null) {
					leastTimestampLineIndex = i;
					leastTimestamp = new Timestamp(digestLogFileLine(currentLines.get(i))[2]);
					break;
				}
			}
			
			for (int i = leastTimestampLineIndex; i < currentLines.size(); ++i) {
				if (currentLines.get(i) == null) 
					continue;
				tempTimestamp = new Timestamp(digestLogFileLine(currentLines.get(i))[2]);
				int comparison = tempTimestamp.compareTo(leastTimestamp);
				if (comparison < 0 || 
						(comparison == 0 && tempTimestamp.getLowerBound() < leastTimestamp.getLowerBound())) 
				{
					leastTimestampLineIndex = i;
					leastTimestamp = tempTimestamp;
				}
			}
						
			mergedFileLines.add(currentLines.get(leastTimestampLineIndex));
			
			// Update current lines
			ListIterator<String> i = fileLinesIterators.get(leastTimestampLineIndex);
			if (i.hasNext()) {
				currentLines.set(leastTimestampLineIndex, i.next());
			} else {
				--filesLeft;
				currentLines.set(leastTimestampLineIndex, null);
			}
		}
		
		for (int i = 0; i < currentLines.size(); ++i) {
			if (currentLines.get(i) != null) {
				mergedFileLines.add(currentLines.get(i));  // DEBUG
				ListIterator<String> iterator = fileLinesIterators.get(i);
				while(iterator.hasNext()) {
					mergedFileLines.add(iterator.next());
				}
			}
		}
		
		try(FileWriter fw = new FileWriter(mergedFilename)) {
			for (String line : mergedFileLines)
				fw.write(line + "\n");
		} catch (IOException e) {
			throw new IOException(e);
		}
	}
	
}
