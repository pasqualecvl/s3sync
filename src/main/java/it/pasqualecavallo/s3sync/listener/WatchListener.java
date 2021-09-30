package it.pasqualecavallo.s3sync.listener;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.springframework.scheduling.annotation.Scheduled;

import software.amazon.awssdk.services.sqs.SqsClient;

public class WatchListener implements Runnable {

	private String rootLocation;
	private SqsClient sqsClient;
	
	public WatchListener(String rootLocation, SqsClient sqsClient) {
		this.rootLocation = rootLocation;
		this.sqsClient = sqsClient;
	}
	
	@Override
	public void run() {
		try {
			WatchService watchService = FileSystems.getDefault().newWatchService();
			Path path = Paths.get(this.rootLocation);
			path.register(watchService, 
					StandardWatchEventKinds.ENTRY_CREATE, 
					StandardWatchEventKinds.ENTRY_DELETE, 
					StandardWatchEventKinds.ENTRY_MODIFY);
			WatchKey watchKey = null;
			while(true) {
				watchKey = watchService.take();
				if(watchKey != null) {
					for(WatchEvent<?> event : watchKey.pollEvents()) {
						managingEvent(event);
					}
				}
			}
			
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Cannot start listening thread", e);
		}
	}

	private void managingEvent(WatchEvent<?> event) {
		System.out.println(event.toString());
	}
    
}
