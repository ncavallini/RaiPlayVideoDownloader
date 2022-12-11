package ch.ncavallini.raiplayvideodownloader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import ch.ncavallini.raiplayvideodownloader.util.Menu;

public class Application {
	

	public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {

		if(args != null && args.length != 0) headless(args);
		
		System.out.println("=== RaiPlay Video Downloader ===");

		Scanner sc = new Scanner(System.in);
		sc.useDelimiter(System.lineSeparator());
		int choice = Menu.menu(sc, "One request", "All episodes of a serie");
		

		switch (choice) {
		case 0: {
			System.out.println("Insert the URL of the episode: ");
			String url = sc.next();
			System.out.println("Insert the output directory (without trailing '/'):");
			String outputDir = sc.next();
			oneRequest(url, outputDir);
			break;
		}
		case 1: {
			System.out.println("Insert the URL of the title:");
			String url = sc.next();
			System.out.println("Insert the output directory (without trailing '/'):");
			String outputDir = sc.next();
			multipleRequests(url, outputDir);
			break;
		}
		default:
		}
		sc.close();
	}

	/**
	 * Handles headless execution (i.e., via command-line arguments).
	 * @param args the arguments (see below)
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws InterruptedException
	 * 
	 * 
		 * <h3>Arguments:</h3>
		 * <ul>
		 * <li>-1 (one request) * OR * </li>
		 * <li> -n (multiple requests)</li>
		 * <li> -u input</li>
		 * <li> -o output</li>
		 * </ul>
		 * Exactly 3 args (5 values)
		 
	*/
	private static void headless(String[] args) throws IOException, URISyntaxException, InterruptedException {
	
		if(args.length != 5) throw new IllegalArgumentException("Argument list: " + Arrays.toString(args) + " is not valid. See help section on the website");
		
		boolean multiple = args[0].equals("-n");
		String url = args[2];
		String outputDir = args[4];
		
		if(multiple) multipleRequests(url, outputDir);
		else oneRequest(url, outputDir);	
		
		return;
	}

	/**
	 * Submits one request.
	 * @param url the RaiPlay video URL.
	 * @param outputDir the directory to save the downloaded video to.
	 * @throws IOException if an I/O error occurs while parsing.
	 * @throws InterruptedException if the operation is interrupted.
	 * @throws URISyntaxException if the URL is malformed.
	 */
	private static void oneRequest(String url, String outputDir) throws IOException, InterruptedException, URISyntaxException {
		Request r = Request.of(url);
		r.submit(outputDir).run();
		
	}
	
	
	/**
	 * 
	 * Behaves exactly as {@link #multipleRequests(url, outputDir, Runtime.getRuntime().availableProcessors())}
	 */
	private static void multipleRequests(String url, String outputDir) throws IOException, URISyntaxException, InterruptedException {
		multipleRequests(url, outputDir, Runtime.getRuntime().availableProcessors());
	}
	
	/**
	 * Spawns multiple threads to handle concurrent requests (e.g., to download all episodes of a TV show)
	 * @param url the RaiPlay title URL
	 * @param outputDir the directory to save the files to.
	 * @param parallelism the level of parallelism used internally by the {@code ExecutorService}
	 * @throws IOException if an I/O error occurs.
	 * @throws URISyntaxException if the URL is malformed.
	 * @throws InterruptedException if the operation is interrupted.
	 */
	private static void multipleRequests(String url, String outputDir, int parallelism) throws IOException, URISyntaxException, InterruptedException {
		if(parallelism < 0) throw new IllegalArgumentException("Parallelism must be >= 0");
		ExecutorService executorService = Executors.newWorkStealingPool(parallelism);
		List<Request> queue = Request.ofAll(url);
		List<Callable<Object>> callables = queue.stream().map((req) -> Executors.callable(req.submit(outputDir))).collect(Collectors.toList());
		executorService.invokeAll(callables);
		executorService.shutdown();
	}
}
