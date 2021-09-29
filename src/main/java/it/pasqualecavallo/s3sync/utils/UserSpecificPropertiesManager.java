package it.pasqualecavallo.s3sync.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class UserSpecificPropertiesManager {

	private static Properties properties = new Properties();
	
	public UserSpecificPropertiesManager() throws IOException {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("~/.s3sync.conf");
		try {
			UserSpecificPropertiesManager.properties.load(inputStream);
		} catch (NullPointerException | IOException e) {
			throw new IOException("Property file '~/.s3sync.conf' not found or not well formed");
		}
	}
	
	
	public static String getProperty(String key) {
		return (String) UserSpecificPropertiesManager.properties.get(key);
	}

	public static void setProperty(String key, String value) {
		UserSpecificPropertiesManager.properties.put(key, value);
		//TODO: update file
	}
	
}
