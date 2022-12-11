package ch.ncavallini.raiplayvideodownloader;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Objects;

/**
 * Represents a task committed at downloading the video file indicated in the {@link #request}.
 * @author Niccolò Cavallini
 *
 */
public class Downloader implements Runnable {
	
	
	private static final String FFMPEG_COMMAND_TEMPLATE = "ffmpeg -i \"%s\" -c copy -bsf:a aac_adtstoasc \"%s.mp4\"";
	
	private final Request request;
	private final String outputDir;
	private boolean terminated;
	
	public Downloader(Request request, String outputDir) {
		
		Objects.requireNonNull(request);
		Objects.requireNonNull(outputDir);
		
		this.request = request;
		this.outputDir = outputDir;
		this.terminated = false;
	}

	/**
	 * Perform the actual downloading of the video file using FFmpeg. 
	 */
	@Override
	public void run() {
		String command = String.format(FFMPEG_COMMAND_TEMPLATE, request.getContentUrl(), outputDir + "\\" + request.getTitle());
		try {
			Process p = new ProcessBuilder().command(command.split(" "))
											.redirectOutput(Redirect.DISCARD)
											.redirectError(Redirect.DISCARD)
											.start();
			int exitCode = p.waitFor();
			if(exitCode != 0) throw new RuntimeException(request + " --- TERMINATED with code " + exitCode);
			terminated = true;
			System.out.println(request + " --- TERMINATED with code " + exitCode);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean isTerminated() {
		return terminated;
	}

}
