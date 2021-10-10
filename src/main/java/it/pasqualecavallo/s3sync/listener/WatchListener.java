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

import com.sun.nio.file.SensitivityWatchEventModifier;

import it.pasqualecavallo.s3sync.service.SynchronizationService;
import it.pasqualecavallo.s3sync.service.UploadService;
import it.pasqualecavallo.s3sync.utils.FileUtils;

public class WatchListener implements Runnable {

	private UploadService uploadService;
	private String remoteFolder;
	private String localRootFolder;
	
	public WatchListener(UploadService uploadService,
			String remoteFolder, String localRootFolder) {
		this.uploadService = uploadService;
		this.remoteFolder = remoteFolder;
		this.localRootFolder = localRootFolder;
	}

	@Override
	public void run() {
		try {
	        final Map<WatchKey, Path> keys = new HashMap<>();
			WatchService watchService = FileSystems.getDefault().newWatchService();
			Path path = Paths.get(this.localRootFolder);
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
				// Operation locked by batch processes (like startup synchonization, batch synchronization, etc)
				if(WatchListeners.threadNotLocked()) {
					WatchKey watchKey = watchService.take();
					if (watchKey != null) {
						for (WatchEvent<?> event : watchKey.pollEvents()) {
							managingEvent(event);
						}
						watchKey.reset();
					}
				} else {
					Thread.sleep(1000);
				}
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Cannot start listening thread", e);
		}
	}

	private void managingEvent(WatchEvent<?> event) {
		Path path = (Path) event.context();
		if(FileUtils.notMatchFilters(SynchronizationService.getExclusionPattern(localRootFolder), path)) {
			switch(event.kind().name()) {
			case "ENTRY_CREATE":
			case "ENTRY_MODIFY":
				System.out.println("Create or modify file: " + path.toString());
				uploadService.upload(path, remoteFolder, localRootFolder + "/" + path.toFile().getAbsolutePath());
				break;
			case "ENTRY_DELETE":
				System.out.println("Delete file: " + path.toString());
				uploadService.delete(path, remoteFolder, path.toFile().getAbsolutePath().replaceFirst(localRootFolder, ""));
				break;
			default:
				System.out.println("Unhandled event " + event.kind().name() + " on file " + event.context().toString());			
			}			
		}
	}

}
