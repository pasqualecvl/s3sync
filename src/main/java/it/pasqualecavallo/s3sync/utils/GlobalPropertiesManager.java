package it.pasqualecavallo.s3sync.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GlobalPropertiesManager {

	private static Properties properties = new Properties();

	static {
		try {
			FileInputStream inputStream = new FileInputStream("/etc/s3sync.conf");
			GlobalPropertiesManager.properties.load(inputStream);
		} catch (IOException e) {
			System.out.println("Error reading property files");
		}
	}

	public static String getProperty(String key) {
		return (String) GlobalPropertiesManager.properties.get(key);
	}
}
