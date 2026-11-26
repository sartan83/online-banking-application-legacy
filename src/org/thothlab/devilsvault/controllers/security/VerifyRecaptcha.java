package org.thothlab.devilsvault.controllers.security;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

public class VerifyRecaptcha {

	public static final String url = "https://www.google.com/recaptcha/api/siteverify";
	public static final String secret = "6LecawoUAAAAAJExbMTuQv-w2frvPCGcboe_Lo2q";
	private final static String USER_AGENT = "Mozilla/5.0";

	public static boolean verify(String gRecaptchaResponse) throws IOException {
		return verify(gRecaptchaResponse, url);
	}

	static boolean verify(String gRecaptchaResponse, String endpoint) {
		if (gRecaptchaResponse == null || "".equals(gRecaptchaResponse)) {
			return false;
		}
		
		try{
		HttpURLConnection con = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		String postParams = "secret=" + URLEncoder.encode(secret, StandardCharsets.UTF_8) + "&response="
				+ URLEncoder.encode(gRecaptchaResponse, StandardCharsets.UTF_8);

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(postParams);
		wr.flush();
		wr.close();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// print result
		//System.out.println(response.toString());
		
		//parse JSON response and return 'success' value
		JsonReader jsonReader = Json.createReader(new StringReader(response.toString()));
		JsonObject jsonObject = jsonReader.readObject();
		jsonReader.close();
		
		return jsonObject.getBoolean("success");
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
}