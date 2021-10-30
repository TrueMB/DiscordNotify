package me.truemb.disnotify.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PlayerManager {

	public static UUID getUUIDOffline(String playerName) {
		String url = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
		try {
			JsonObject json = getJson(url);
			getName(json.get("id").getAsString());
			String uuidS = json.get("id").getAsString();
			return UUID.fromString(uuidS.substring(0, 8) + "-" + uuidS.substring(8, 12) + "-" + uuidS.substring(12, 16) + "-" + uuidS.substring(16, 20) + "-" + uuidS.substring(20, 32));
		} catch (IOException e) {
			//e.printStackTrace();
		}
		return null;
	}

	public static String getName(String uuid) {
		String url = "https://api.mojang.com/user/profiles/" + uuid.replace("-", "") + "/names";
		try {
			JsonArray array = getJsonArray(url);
			JsonObject json = array.get(array.size() - 1).getAsJsonObject();
			String name = json.get("name").getAsString();
			return name;
		} catch (IOException e) {
			//e.printStackTrace();
		}
		return null;
	}

	private static JsonObject getJson(String sURL) throws IOException {

		    // Connect to the URL using java's native library
		    URL url = new URL(sURL);
		    URLConnection request = url.openConnection();
		    request.connect();

		    // Convert to a JSON object to print data
		    JsonParser jp = new JsonParser(); //from gson
		    JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
		    JsonObject rootobj = root.getAsJsonObject(); //May be an array, may be an object. 
		    //String zipcode = rootobj.get("zip_code").getAsString(); //just grab the zipcode
			return rootobj;
	}
	
	private static JsonArray getJsonArray(String sURL) throws IOException {

	    // Connect to the URL using java's native library
	    URL url = new URL(sURL);
	    URLConnection request = url.openConnection();
	    request.connect();

	    // Convert to a JSON object to print data
	    JsonParser jp = new JsonParser(); //from gson
	    JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
	    JsonArray rootobj = root.getAsJsonArray(); //May be an array, may be an object. 
	    //String zipcode = rootobj.get("zip_code").getAsString(); //just grab the zipcode
		return rootobj;
}
	
	
	
	
}