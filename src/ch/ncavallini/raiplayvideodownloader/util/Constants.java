package ch.ncavallini.raiplayvideodownloader.util;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;

import com.google.gson.Gson;

public class Constants {
	
	public static final Gson GSON = new Gson();
	public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
															.followRedirects(Redirect.ALWAYS)
															.build();

}
