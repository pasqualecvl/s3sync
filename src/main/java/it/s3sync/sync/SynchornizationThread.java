package it.s3sync.sync;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.Watchable;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import it.s3sync.listener.WatchListener;
import it.s3sync.listener.WatchListeners;
import it.s3sync.listener.WatchListeners.Operation;
import it.s3sync.model.Item;
import it.s3sync.service.SynchronizationService;
import it.s3sync.service.UploadService;
import it.s3sync.sync.EventData.EventSource;
import it.s3sync.sync.EventData.S3Action;
import it.s3sync.utils.FileUtils;
import it.s3sync.utils.SpringContext;
import it.s3sync.utils.UserSpecificPropertiesManager;

public class SynchornizationThread extends Thread {

	private EventData eventData;
	private SynchronizationService synchronizationService;
	private UploadService uploadService;
	private MongoOperations mongoOperations;
	
	private static final Logger logger = LoggerFactory.getLogger(SynchornizationThread.class);
	
	public SynchornizationThread(EventData eventData) {
		this.eventData = eventData;
		// SAFE OP -> It is impossible for a thread like this to start before spring context is fully loaded 
		this.synchronizationService = SpringContext.getBean(SynchronizationService.class);
		this.uploadService = SpringContext.getBean(UploadService.class);
		this.mongoOperations = SpringContext.getBean(MongoOperations.class);
	}
	
	@Override
	public void run() {
		if(EventSource.AMQP.equals(eventData.getEventSource())) {
			rusAsAmqpEvent(eventData);
		} else {
			runAsFilesystemEvent(eventData);
		}
	}

	private void runAsFilesystemEvent(EventData eventData) {
		if(!(eventData instanceof FileSystemEventData)) {
			logger.info("[[INFO]] Trying serving event with kind {} from filesystem event manager", eventData.getEventSource());
			return;
		}		
		FileSystemEventData castedEventData = (FileSystemEventData)eventData;
		
		Watchable watchable = castedEventData.getWatchable();
		WatchEvent<?> event = castedEventData.getWatchEvent();
		String localRootFolder = castedEventData.getLocalRootFolder();
		String remoteFolder = castedEventData.getRemoteFolder();
		
		
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
								WatchListeners.registerNewFolder(localRootFolder, fullLocation);
								return FileVisitResult.CONTINUE;
							}
						});
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
					WatchListeners.removeFolder(localRootFolder, fullLocation);
				} else {
					logger.debug("[[DEBUG]] DELETE event on file {}", fullLocation);
					if (!uploadService.delete(remoteFolder, fullLocation.replaceFirst(localRootFolder, ""))) {
						logger.error("[[ERROR]] Error deleting file {} from S3", fullLocation);
					}
				}
				break;
			default:
				logger.debug("[[DEBUG]] Unhandled event {} on file {}", event.kind(), fullLocation);
			}
			logger.debug("[[DEBUG]] Finish serving event on {}", fullLocation);
		} else {
			logger.info("[[DEBUG]] Event on file {} was skipped by regexp filters.", fullLocation);
		}
	}

	private void rusAsAmqpEvent(EventData eventData) {
		if(!(eventData instanceof AmqpEventData)) {
			logger.info("[[INFO]] Trying serving event with kind {} from amqp event manager", eventData.getEventSource());
			return;
		}
		AmqpEventData localEvent = (AmqpEventData)eventData;
		
		WatchListeners.lockSemaphore();
		logger.info("Locking WatchListeners");
		try {
			if (!localEvent.getSource().equals(UserSpecificPropertiesManager.getConfiguration().getAlias())) {
				String localFolder = synchronizationService.getSynchronizedLocalRootFolderByRemoteFolder(eventData.getRemoteFolder());
				if (localFolder != null) {
					logger.info("Serving action: {}", eventData.toString());
					if (S3Action.CREATE.equals(localEvent.getS3Action()) || S3Action.MODIFY.equals(localEvent.getS3Action())) {
						Item item = mongoOperations.findOne(
								new Query(Criteria.where("ownedByFolder").is(eventData.getRemoteFolder()).and("originalName").is(localEvent.getFile())),
								Item.class);
						List<String> folders = uploadService.getOrUpdate(localFolder + localEvent.getFile(), item);
						if (!folders.isEmpty()) {
							for (String folder : folders) {
								// if a new folder will be created, current watchers will throw an event on the
								// parent folder of this event
								if (!((WatchListener) WatchListeners.getThread(localFolder).getR())
										.existsWatchKey(folder)) {
									// start as separate thread to prevent lock (the thread is probably waiting for
									// watchService.poll operation
									new Thread(new AddNewWatchKey(localFolder, folder)).start();
								}
								Operation operation = new Operation();
								operation.setOnFile(folder);
								operation.setS3Action(localEvent.getS3Action());
							}
						}						
						Operation operation = new Operation();
						operation.setOnFile(localFolder + localEvent.getFile());
						operation.setS3Action(localEvent.getS3Action());
					} else if (S3Action.DELETE.equals(localEvent.getS3Action())) {
						long localFileLastModified = Path.of(localFolder + localEvent.getFile()).toFile().lastModified();
						if (localFileLastModified <= localEvent.getTime()) {
							List<String> toDelete = FileUtils.toDeleteFileAndEmptyTree(localFolder + localEvent.getFile());
							for (String s : toDelete) {
								// start as separate thread to prevent lock (the thread is probably waiting for
								// watchService.poll operation
								new Thread(new RemoveWatchKey(localFolder, s)).start();
							}
						} else {
							logger.info("Local file is newer than the remote one: {} -> {}", localFileLastModified,
									localEvent.getTime());
						}
					}
				}
			} else {
				logger.info("Consuming AMQP message made by this client -> discarded");
			}
		} finally {
			logger.info("Release semaphore");
			WatchListeners.releaseSemaphore();
		}
		
	}
	
	
	protected class AddNewWatchKey implements Runnable {

		private String folder;
		private String localRootFolder;

		public AddNewWatchKey(String localRootFolder, String folder) {
			this.folder = folder;
			this.localRootFolder = localRootFolder;
		}

		@Override
		public void run() {
			((WatchListener) WatchListeners.getThread(localRootFolder).getR()).addFolder(folder);
		}
	}

	protected class RemoveWatchKey implements Runnable {

		private String folder;
		private String localRootFolder;

		public RemoveWatchKey(String localRootFolder, String folder) {
			this.folder = folder;
			this.localRootFolder = localRootFolder;
		}

		@Override
		public void run() {
			((WatchListener) WatchListeners.getThread(localRootFolder).getR()).removeFolder(folder);
		}
	}

}
