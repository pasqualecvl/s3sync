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
import java.nio.file.Watchable;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.nio.file.SensitivityWatchEventModifier;

import it.pasqualecavallo.s3sync.service.SynchronizationService;
import it.pasqualecavallo.s3sync.service.UploadService;
import it.pasqualecavallo.s3sync.utils.FileUtils;

public class WatchListener implements Runnable {

	private UploadService uploadService;
	private String remoteFolder;
	private String localRootFolder;
	private WatchService watchService;
	private SynchronizationService synchronizationService;
	
	private Map<String, WatchKey> watchKeys = new HashMap<>();
	private Set<String> directories = new HashSet<>();
	
	
	public WatchListener(UploadService uploadService, SynchronizationService synchronizationService,
			String remoteFolder, String localRootFolder) {
		this.uploadService = uploadService;
		this.remoteFolder = remoteFolder;
		this.localRootFolder = localRootFolder;
		this.synchronizationService = synchronizationService;
	}

	@Override
	public void run() {
		try {
			// Operation locked by batch processes (like startup synchonization, batch
			// synchronization, etc)
			watchService = FileSystems.getDefault().newWatchService();
			Path path = Paths.get(this.localRootFolder);
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					watchKeys.put(dir.toString(),
							dir.register(watchService,
									new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_CREATE,
											StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY,
											StandardWatchEventKinds.OVERFLOW },
									SensitivityWatchEventModifier.MEDIUM));
					directories.add(dir.toString());
					return FileVisitResult.CONTINUE;
				}
			});
			while (true) {
				// Operation locked by batch processes (like startup synchonization, batch synchronization, etc)
				if(WatchListeners.threadNotLocked()) {
					WatchKey watchKey = watchService.take();
					if (watchKey != null) {
						for (WatchEvent<?> event : watchKey.pollEvents()) {
							managingEvent(event, watchKey.watchable());
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

	private void managingEvent(WatchEvent<?> event, Watchable watchable) {
		String listenerPath = watchable.toString();
		String resourceName = event.context().toString();
		String fullLocation = listenerPath + "/" + resourceName;
		if(WatchListeners.checkForProgrammaticallyChange(localRootFolder, fullLocation.replaceFirst(localRootFolder, ""))) {
			return;
		}
		Path fullPath = Path.of(fullLocation);
		if (FileUtils.notMatchFilters(synchronizationService.getExclusionPattern(localRootFolder), fullLocation)) {
			switch (event.kind().name()) {
			case "ENTRY_CREATE":
				if (fullPath.toFile().isDirectory()) {
					try {
						watchKeys.put(fullLocation, fullPath.register(watchService,
								new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_CREATE,
										StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY,
										StandardWatchEventKinds.OVERFLOW },
								SensitivityWatchEventModifier.MEDIUM));
						directories.add(fullLocation);
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				}
			case "ENTRY_MODIFY":
				System.out.println("Create or modify file: " + fullLocation);
				uploadService.upload(fullPath, remoteFolder, fullLocation.replaceFirst(localRootFolder, ""));
				break;
			case "ENTRY_DELETE":
				if (directories.contains(fullLocation)) {
					if (watchKeys.containsKey(fullLocation)) {
						watchKeys.get(fullLocation).cancel();
						watchKeys.remove(fullLocation);
					}
					uploadService.deleteAsFolder(remoteFolder, fullLocation.replaceFirst(localRootFolder, ""));
					directories.remove(fullLocation);
				} else {
					if(!uploadService.delete(remoteFolder, fullLocation.replaceFirst(localRootFolder, ""))) {
						System.out.println("Error deleting S3 file");
					};
				}
				break;
			default:
				System.out.println("Unhandled event " + event.kind().name() + " on file " + fullLocation);
			}
		}
	}

}
