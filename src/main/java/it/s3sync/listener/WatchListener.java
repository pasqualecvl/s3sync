package it.s3sync.listener;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.nio.file.SensitivityWatchEventModifier;

import it.s3sync.service.SynchronizationService;
import it.s3sync.service.UploadService;
import it.s3sync.utils.FileUtils;

public class WatchListener implements Runnable {

	private UploadService uploadService;
	private String remoteFolder;
	private String localRootFolder;
	private WatchService watchService;
	private SynchronizationService synchronizationService;

	private Map<String, WatchKey> watchKeys = new HashMap<>();
	private Set<String> directories = new HashSet<>();

	private static final Logger logger = LoggerFactory.getLogger(WatchListener.class);

	public WatchListener(UploadService uploadService, SynchronizationService synchronizationService,
			String remoteFolder, String localRootFolder) {
		logger.info("[[INFO]] Constructing watchlistener for root local folder: {} with remote folder {}",
				localRootFolder, remoteFolder);
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
			logger.debug("[[DEBUG]] Creating watchService for local folder {} working on remote folder {}",
					localRootFolder, remoteFolder);
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
				// Timeout needed because sometimes maps must be reloaded
				WatchKey watchKey = watchService.poll(60, TimeUnit.SECONDS);
				// Operation locked by batch processes (like startup synchonization, batch
				// synchronization, etc)
				boolean solved = false;
				do {
					if (WatchListeners.threadNotLocked()) {
						if (watchKey != null) {
							for (WatchEvent<?> event : watchKey.pollEvents()) {
								logger.debug("[[DEBUG]] Managing event {}", event.toString());
								try {
									managingEvent(event, watchKey.watchable());
								} catch (Exception e) {
									logger.error("[[ERROR]] Exception managing event {}/{}, proceed with next",
											event.kind(), watchKey.watchable().toString());
								}
							}
							watchKey.reset();
						}
						solved = true;
					} else {
						logger.debug("[[DEBUG]] Thread on {} currently locked by semaphore, wait 1000ms", localRootFolder);
						Thread.sleep(1000);
					}					
				} while(!solved);
			}
		} catch (Exception e) {
			throw new RuntimeException("[[ERROR]]Cannot start listening thread", e);
		}
	}

	private void managingEvent(WatchEvent<?> event, Watchable watchable) {
		String listenerPath = watchable.toString();
		String resourceName = event.context().toString();
		String fullLocation = listenerPath + "/" + resourceName;
		if (WatchListeners.checkForProgrammaticallyChange(localRootFolder,
				fullLocation.replaceFirst(localRootFolder, ""))) {
			logger.info("[[INFO]] Event on {} for file {} was made by s3sync. Skipped.", localRootFolder, fullLocation);

			return;
		}
		Path fullPath = Path.of(fullLocation);
		if (FileUtils.notMatchFilters(synchronizationService.getExclusionPattern(localRootFolder), fullLocation)) {
			switch (event.kind().name()) {
			case "ENTRY_CREATE":
			case "ENTRY_MODIFY":
				logger.debug("[[DEBUG]] Managing event CREATED on file {}", fullLocation);
				if (fullPath.toFile().isDirectory()) {
					logger.debug("[[DEBUG]] CREATE event on folder {}", fullLocation);
					try {
						Files.walkFileTree(fullPath, new SimpleFileVisitor<Path>() {
							@Override
							public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
									throws IOException {
								logger.debug("[[DEBUG]] Add watchKey on {}", fullLocation);
								watchKeys.put(dir.toString(), dir.register(watchService, new WatchEvent.Kind[] {
										StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
										StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.OVERFLOW },
										SensitivityWatchEventModifier.MEDIUM));
								logger.debug("[[DEBUG]] Add {} to folder tree", fullLocation);
								directories.add(dir.toString());
								return FileVisitResult.CONTINUE;
							}
						});
						uploadService.uploadAsFolder(fullPath, localRootFolder, remoteFolder);
					} catch (IOException e) {
						logger.error("Exception", e);
					}
				} else {
					logger.debug("[[DEBUG]] Managing event CREATE/MODIFY on file {}, start upload", fullLocation);
					try {
						uploadService.upload(fullPath, remoteFolder, fullLocation.replaceFirst(localRootFolder, ""),
								Files.getLastModifiedTime(fullPath).toMillis());
					} catch (Exception e) {
						logger.error("Exception saving file {}", fullLocation, e);
					}
				}
				break;
			case "ENTRY_DELETE":
				logger.debug("[[DEBUG]] Managing event DELETE on file {}", fullLocation);
				if (directories.contains(fullLocation)) {
					logger.debug("[[DEBUG]] DELETE event on folder {}", fullLocation);
					if (watchKeys.containsKey(fullLocation)) {
						logger.debug("[[DEBUG]] Kill watchKey and remote folder from tree");
						watchKeys.get(fullLocation).cancel();
						watchKeys.remove(fullLocation);
					} else {
						logger.warn("[[WARN]] WatchKey not found for this folder " + fullLocation);
					}
					logger.debug("[[DEBUG]] Delete all files in folder {}", fullLocation);
					try {
						uploadService.deleteAsFolder(remoteFolder, fullLocation.replaceFirst(localRootFolder, ""));
						logger.debug("[[DEBUG]] Remove folder {} from the folders tree");
						directories.remove(fullLocation);
					} catch (Exception e) {
						logger.error("Exception removing file {}", fullLocation, e);
					}
				} else {
					logger.debug("[[DEBUG]] DELETE event on file " + fullLocation);
					if (!uploadService.delete(remoteFolder, fullLocation.replaceFirst(localRootFolder, ""))) {
						logger.error("[[ERROR]] Error deleting file {} from S3", fullLocation);
					}
				}
				break;
			default:
				logger.debug("[[DEBUG]] Unhandled event {} on file {}", event.kind().name(), fullLocation);
			}
			logger.debug("[[DEBUG]] Finish serving event on {}", fullLocation);
		} else {
			logger.info("[[DEBUG]] Event on file {} was skipped by regexp filters.", fullLocation);
		}
	}

	public void addFolder(String fullLocation) {
		Path path = Path.of(fullLocation);
		synchronized (watchKeys) {
			if (!watchKeys.containsKey(fullLocation)) {
				try {
					watchKeys.put(fullLocation,
							path.register(watchService,
									new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_CREATE,
											StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY,
											StandardWatchEventKinds.OVERFLOW },
									SensitivityWatchEventModifier.MEDIUM));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				directories.add(fullLocation);
			}
		}
	}

	public void removeFolder(String fullLocation) {
		synchronized (watchKeys) {
			watchKeys.remove(fullLocation);
		}
		directories.remove(fullLocation);
		try {
			Files.delete(Path.of(fullLocation));
		} catch (Exception e) {
			logger.error("[[ERROR]] Exception deleting folder {}", fullLocation, e);
		}
	}

	public boolean existsWatchKey(String folder) {
		return watchKeys.get(folder) != null;
	}

}
