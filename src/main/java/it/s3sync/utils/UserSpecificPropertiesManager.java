package it.s3sync.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.json.JsonMapper;

import it.s3sync.model.AttachedClient;

public class UserSpecificPropertiesManager {

	private static JsonMapper mapper = new JsonMapper();
	
	private static final Logger logger = LoggerFactory.getLogger(UserSpecificPropertiesManager.class);
	
	static {
		String userHome = System.getProperty("user.home");
		try (FileInputStream inputStream = new FileInputStream(userHome + "/.s3sync.conf")) {
			AttachedClient client = mapper.readValue(inputStream.readAllBytes(), AttachedClient.class);
			if(client.getAlias() == null) {
				throw new Exception();
			}
		} catch (Exception e) {
			File file = new File(userHome + "/.s3sync.conf");
			try {
				FileWriter confWriter = new FileWriter(file);
				AttachedClient client = new AttachedClient();
				client.setAlias(UUID.randomUUID().toString());
				confWriter.write(mapper.writeValueAsString(client));
				confWriter.close();
			} catch (IOException e1) {
				logger.error("[[ERROR]] Cannot create configuration file", e);
			}
		}
	}
	
	public static AttachedClient getConfiguration() {
		String userHome = System.getProperty("user.home");
		try (FileInputStream inputStream = new FileInputStream(userHome + "/.s3sync.conf")) {
			return mapper.readValue(inputStream.readAllBytes(), AttachedClient.class);
		} catch(Exception e) {
			throw new RuntimeException("Can't read configuration file", e);
		}
	}

	public static void setConfiguration(AttachedClient attachedClient) {
		String userHome = System.getProperty("user.home");
		File file = new File(userHome + "/.s3sync.conf");
		try (FileWriter confWriter = new FileWriter(file)){
			confWriter.write(mapper.writeValueAsString(attachedClient));
		} catch (IOException e) {
			throw new RuntimeException("Can't write configuration file", e);
		}
	}
	
}
