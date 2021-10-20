package it.s3sync.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.management.RuntimeErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalPropertiesManager {

	private static Properties properties = new Properties();

	private static final Logger logger = LoggerFactory.getLogger(GlobalPropertiesManager.class);
	
	static {
		try {
			FileInputStream inputStream = new FileInputStream("/etc/s3sync.conf");
			GlobalPropertiesManager.properties.load(inputStream);
		} catch (IOException e) {
			logger.error("Error reading property files");
			throw new RuntimeException(e);
		}
	}

	public static String getProperty(String key) {
		return (String) GlobalPropertiesManager.properties.get(key);
	}
}
