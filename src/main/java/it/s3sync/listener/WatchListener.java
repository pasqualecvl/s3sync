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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.nio.file.SensitivityWatchEventModifier;

import it.s3sync.listener.SynchronizationMessageDto.S3Action;
import it.s3sync.listener.WatchListeners.Operation;
import it.s3sync.model.AttachedClient;
import it.s3sync.model.AttachedClient.SyncFolder;
import it.s3sync.service.SynchronizationService;
import it.s3sync.service.UploadService;
import it.s3sync.utils.FileUtils;
import it.s3sync.utils.UserSpecificPropertiesManager;

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
							for (WatchEvent<?> event : sanitize(watchKey.pollEvents(), watchKey)) {
								logger.debug("[[DEBUG]] Managing event {}", event.toString());
								try {
									managingEvent(event, watchKey.watchable());
								} catch (Exception e) {
									logger.error("[[ERROR]] Exception managing event {} on {}, proceed with next",
											event.kind(), watchKey.watchable().toString(), e);
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

	List<WatchEvent<?>> sanitize(List<WatchEvent<?>> events, WatchKey watchKey) {
		return skipCreateOnFolder(removeDeleteOverCreation(deduplicateEvent(events)), watchKey);
	}
	
	private List<WatchEvent<?>> skipCreateOnFolder(List<WatchEvent<?>> events, WatchKey watchKey) {
		List<WatchEvent<?>> filteredEvents = new ArrayList<>();
		for(int i = 0; i< events.size(); i++) {
			if(events.get(i).kind().name().equals("EVENT_CREATE") && 
					Files.isDirectory(Path.of(watchKey.watchable().toString() + events.get(i).context().toString()))) {
				logger.trace("[[TRACE]] Discarding event on {} because it is a CRAETE on a folder (must do nothing)");
			} else {
				filteredEvents.add(events.get(i));
			}
		}
		return filteredEvents;
	}
	
	private List<WatchEvent<?>> removeDeleteOverCreation(List<WatchEvent<?>> events) {
		List<WatchEvent<?>> filteredEvents = new ArrayList<>();
		List<Integer> blockList = new ArrayList<>();
		for(int i = 0; i< events.size(); i++) {
			if(events.get(i).kind().name().equals("EVENT_CREATE") || events.get(i).kind().name().equals("EVENT_MODIFY")) {
				int foundPosition = -1;
				for(int j = i + 1; j < events.size(); j++) {
					if(events.get(j).kind().name().equals("EVENT_DELETE") && 
							(((Path)events.get(j).context()).equals((Path)events.get(j).context()))
									&& !blockList.contains(j)) {
						foundPosition = j;
						blockList.add(j);
						break;
					}
				}
				if(foundPosition == -1) {
					filteredEvents.add(events.get(i));
				}
			} else if(!blockList.contains(i)) {
				filteredEvents.add(events.get(i));
			}
		}
		return filteredEvents;
	}

	private List<WatchEvent<?>> deduplicateEvent(List<WatchEvent<?>> events) {
		List<WatchEvent<?>> filteredEvents = new ArrayList<>();
		for(WatchEvent<?> event : events) {
			int foundPosition = -1;
			for(int i = 0; i < filteredEvents.size(); i++) {
				if(filteredEvents.get(i).kind().name().equals(event.kind().name())
						&& ((Path)filteredEvents.get(i).context()).equals((Path)event.context())) {
					//alredy present, discard the old one
					foundPosition = i;
					break;
				}
 			}
			
			if(foundPosition > -1) {
				WatchEvent<?> lastEvent = filteredEvents.get(filteredEvents.size());
				filteredEvents.remove(filteredEvents.size());
				filteredEvents.add(foundPosition, lastEvent);
			}
			filteredEvents.add(event);
		}
		return filteredEvents;
	}

	private void managingEvent(WatchEvent<?> event, Watchable watchable) {
		String listenerPath = watchable.toString();
		String resourceName = event.context().toString();
		String fullLocation = listenerPath + "/" + resourceName;
		Path fullPath = Path.of(fullLocation);

		boolean workAsDirectory = Files.isDirectory(fullPath);
		
		Operation operation = new Operation();
		operation.setS3Action("ENTRY_CREATE".equals(event.kind().name()) ? 
				S3Action.CREATE : "ENTRY_MODIFY".equals(event.kind().name()) ? 
						S3Action.MODIFY : S3Action.DELETE);
		if(workAsDirectory) {
			operation.setOnFile(fullLocation);
		} else {
			operation.setOnFile(fullLocation.replaceFirst(localRootFolder, ""));			
		}
		if (FileUtils.notMatchFilters(synchronizationService.getExclusionPattern(localRootFolder), fullLocation)) {
			switch (event.kind().name()) {
			case "ENTRY_CREATE":
			case "ENTRY_MODIFY":
				logger.debug("[[DEBUG]] Managing event CREATED on file {}", fullLocation);
				if (workAsDirectory) {
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
//						uploadService.uploadAsFolder(fullPath, localRootFolder, remoteFolder);
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
				if(workAsDirectory) {
					if(watchKeys.containsKey(fullLocation)) {
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
								String relativeLocation = fullLocation.replaceFirst(localRootFolder, "");
								if (relativeLocation.isBlank()) {
									synchronizationService.removeSynchronizationFolder(
											synchronizationService.getSynchronizedLocalRootFolderByRemoteFolder(remoteFolder));
								} else {
									synchronizationService.addSynchronizationExclusionPattern(
											synchronizationService.getSynchronizedLocalRootFolderByRemoteFolder(remoteFolder),
											"^" + relativeLocation);
								}
//								uploadService.deleteAsFolder(remoteFolder, );
								logger.debug("[[DEBUG]] Remove folder {} from the folders tree");
								if(directories.contains(fullLocation)) {
									directories.remove(fullLocation);									
								}
							} catch (Exception e) {
								logger.error("Exception removing file {}", fullLocation, e);
							}
						}
					} else {
						logger.info("[[INFO]] Deleted folder was not in listening. Doing nothing...");
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
