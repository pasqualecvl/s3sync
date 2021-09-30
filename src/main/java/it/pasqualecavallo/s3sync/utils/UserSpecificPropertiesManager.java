package it.pasqualecavallo.s3sync.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

public class UserSpecificPropertiesManager {

	private static Properties properties = new Properties();

	static {
		String userHome = System.getProperty("user.home");
		try {
			FileInputStream inputStream = new FileInputStream(userHome + "/.s3sync.conf");
			UserSpecificPropertiesManager.properties.load(inputStream);
		} catch (IOException e) {
			File file = new File(userHome + "/.s3sync.conf");
			try {
				FileWriter confWriter = new FileWriter(file);
				confWriter.write("client.alias=" + UUID.randomUUID().toString());
				confWriter.close();
				FileInputStream inputStream = new FileInputStream(userHome + "/.s3sync.conf");
				UserSpecificPropertiesManager.properties.load(inputStream);
			} catch (IOException e1) {
				System.err.println("Cannot create configuration file");
			}
		}
	}
	
	public static String getProperty(String key) {
		return (String) UserSpecificPropertiesManager.properties.get(key);
	}

	public static void setProperty(String key, String value) {
		UserSpecificPropertiesManager.properties.put(key, value);
		// TODO: update file
	}

}
