package it.pasqualecavallo.s3sync.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesManager {

	private static Properties properties = new Properties();
	
	public PropertiesManager() throws IOException {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("/etc/s3sync.conf");
		try {
			PropertiesManager.properties.load(inputStream);
		} catch (NullPointerException | IOException e) {
			throw new IOException("Property file '/etc/s3sync.conf' not found or not well formed");
		}
	}
	
	
	public static String getProperty(String key) {
		return (String) PropertiesManager.properties.get(key);
	}
}

