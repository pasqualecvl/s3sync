package it.pasqualecavallo.s3sync.listener;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sun.nio.file.SensitivityWatchEventModifier;

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
	        final Map<WatchKey, Path> keys = new HashMap<>();
			WatchService watchService = FileSystems.getDefault().newWatchService();
			Path path = Paths.get(this.rootLocation);
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    WatchKey watchKey = dir.register(watchService, 
                    		new WatchEvent.Kind[]{
                    				StandardWatchEventKinds.ENTRY_CREATE,
                    				StandardWatchEventKinds.ENTRY_DELETE,
                    				StandardWatchEventKinds.ENTRY_MODIFY,
                    				StandardWatchEventKinds.OVERFLOW}, 
                    		SensitivityWatchEventModifier.MEDIUM);
                    keys.put(watchKey, dir);
                    return FileVisitResult.CONTINUE;
                }
            });
			while (true) {
				WatchKey watchKey = watchService.take();
				if (watchKey != null) {
					for (WatchEvent<?> event : watchKey.pollEvents()) {
						managingEvent(event);
					}
					watchKey.reset();
				}
			}

		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Cannot start listening thread", e);
		}
	}

	private void managingEvent(WatchEvent<?> event) {
		switch(event.kind().name()) {
		case "ENTRY_CREATE":
			System.out.println("Creating file: " + event.context().toString());
			break;
		case "ENTRY_DELETE":
			System.out.println("Deleting file: " + event.context().toString());
			break;
		case "ENTRY_MODIFY":
			System.out.println("Modifing file: " + event.context().toString());
			break;
		default:
			System.out.println("Unhandled event " + event.kind().name() + " on file " + event.context().toString());
			
		}
			
	}

}
