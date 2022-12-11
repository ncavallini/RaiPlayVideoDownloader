package ch.ncavallini.raiplayvideodownloader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ch.ncavallini.raiplayvideodownloader.util.Constants;

/**
 * This class represents a request to the RaiPlay website.
 * @author Niccolò Cavallini
 *
 */

public class Request {
	
	/**
	 * The URL of the RaiPlay video page.
	 */
	private final String url;
	/**
	 * The title of the movie or episode this Request is trying to download.
	 */
	private final String title;
	/**
	 * The season in which the episode this Request is trying to download was first aired (0 if not applicable).
	 */
	private final int season;
	/**
	 * The number of the episode this Request is trying to download (0 if not applicable).
	 */
	private final int episode;
	/**
	 * The URL of the m3u playlist containing references to download this video.
	 */
	private final String contentUrl;
	
	/**
	 * All-args constructor to create a complete Request.
	 * The way to create a Request is to call the static method {@code Request.of(String url)}
	 * That method, internally, uses this constructor.
	 * @param url the RaiPlay URL
	 * @param title the title of the video
	 * @param season the season number
	 * @param episode the episode number
	 * @param contentUrl the URL of the RaiPlay m3u playlist. 
	 */
	private Request(String url, String title, int season, int episode, String contentUrl) {
	
		Objects.requireNonNull(url);
		Objects.requireNonNull(title);
		Objects.requireNonNull(season);
		Objects.requireNonNull(episode);
		Objects.requireNonNull(contentUrl);
		
		this.url = url;
		this.title = title;
		this.season = season;
		this.episode = episode;
		this.contentUrl = contentUrl;
	}
	
	
	/**
	 * Main access point of this API.
	 * Creates a Request given the RaiPlay URL.
	 * Grabs all the needed information from the RaiPlay website (parsing their JSON) and returns a new {@code Request} object accordingly.
	 * 
	 * @param url the URL of the RaiPlay title.
	 * @return a {@code Request} object with all the needed information grabbed automatically from RaiPlay.
	 * @throws IOException if an error occurs when sending HTTP requests.
	 * @throws InterruptedException if the operation is interrupted.
	 * @throws URISyntaxException if the URL provided is invalid.
	 */
	public static Request of(String url) throws IOException, InterruptedException, URISyntaxException {
		String title, contentUrl;
		int season, episode;
		
		String jsonUrl = FilenameUtils.removeExtension(url) + ".json";
		HttpRequest req = HttpRequest.newBuilder(new URI(jsonUrl)).build();
		HttpResponse<String> res = Constants.HTTP_CLIENT.send(req, BodyHandlers.ofString());
		String json = res.body();
		JsonObject mainObj = JsonParser.parseString(json).getAsJsonObject();
		
		title = mainObj.get("name").getAsString();
		season = mainObj.get("season").getAsString().equals("") ? 0 : mainObj.get("season").getAsInt();
		episode = mainObj.get("episode").getAsString().equals("") ? 0 : mainObj.get("episode").getAsInt();
		contentUrl = mainObj.get("video").getAsJsonObject().get("content_url").getAsString();
		
		return new Request(url, title, season, episode, contentUrl);
	}
	
	/**
	 * Creates a {@code List} of {@code Request}s to be processed in parallel.
	 * It is useful to download all episodes of a TV show.
	 * @param titleUrl the URL of the main TV show RaiPlay page.
	 * @return a {@code List<Request>} of all successfully created requests.
	 * @throws IOException if an I/O exception in parsing occurs.
	 * @throws URISyntaxException if the provided URL is malformed.
	 * @throws InterruptedException if the operation is interrupted.
	 */
	public static List<Request> ofAll(String titleUrl) throws IOException, URISyntaxException, InterruptedException {
		
		List<Request> queue = new LinkedList<>();
		String json = getEpisodesJson(titleUrl);
		JsonObject mainObj = JsonParser.parseString(json).getAsJsonObject();
		JsonArray episodes = mainObj.get("seasons").getAsJsonArray().get(0).getAsJsonObject().get("episodes").getAsJsonArray();
		JsonArray cards = null;
		
		for(int i=0; i < episodes.size(); i++) {
			JsonArray currCards = episodes.get(i).getAsJsonObject().get("cards").getAsJsonArray();
			if(currCards.size() != 0) {
				cards = currCards;
				break;
			}
		}
		
		if(cards == null) throw new RuntimeException("No episodes found!!");
		
		for(int i=0; i < cards.size(); i++) {
			String url = "https://raiplay.it" + cards.get(i).getAsJsonObject().get("path_id").getAsString();
			queue.add(Request.of(url)); 
		}
		
		return queue;
	}
	
	/**
	 * Returns a string containing the JSON describing the TV show located at {@code titleUrl}
	 * @param titleUrl a RaiPlay TV show url
	 * @return   a string containing the JSON describing the TV show located at {@code titleUrl}
	 * @throws IOException if an I/O exception occurred while parsing.
	 * @throws URISyntaxException if the provided URL is malformed.
	 * @throws InterruptedException if the operation is interrupted.
	 */
	private static String getEpisodesJson(String titleUrl) throws IOException, URISyntaxException, InterruptedException {
		
		Document doc = Jsoup.connect(titleUrl).get();
		Element raiEpisodes = doc.getElementsByTag("rai-episodes").get(0);
		String jsonUrl = String.format("https://www.raiplay.it%s/%s/%s/%s", raiEpisodes.attr("base_path"), raiEpisodes.attr("block"), raiEpisodes.attr("set"), raiEpisodes.attr("episode_path"));
		
		HttpRequest req = HttpRequest.newBuilder(new URI(jsonUrl)).build();
		return Constants.HTTP_CLIENT.send(req, BodyHandlers.ofString()).body();
	}
	
	/**
	 * Creates a {@code Downloader} object (an implementation of {@link java.lang.Runnable}) and returns it.
	 * @param outputDir the path of the directory to save the video file to.
	 * @return a {@code Downloader} representing the task "Download the video contained in this {@code Request}".
	 */
	public Downloader submit(String outputDir) {
		System.out.println(this + " --- SUBMITTED");
		return new Downloader(this, outputDir);
	}
	

	
	public String getUrl() {
		return url;
	}


	public String getTitle() {
		return title;
	}


	public int getSeason() {
		return season;
	}


	public int getEpisode() {
		return episode;
	}


	public String getContentUrl() {
		return contentUrl;
	}


	@Override
	public int hashCode() {
		return Objects.hash(contentUrl, episode, season, title, url);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Request other = (Request) obj;
		return Objects.equals(contentUrl, other.contentUrl) && episode == other.episode
			 && season == other.season && Objects.equals(title, other.title) && Objects.equals(url, other.url);
	}
	
	@Override
	public String toString() {
		return "Request[" + title + "]";
	}
}

