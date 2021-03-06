package it.s3sync.listener;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.json.JsonMapper;

import it.s3sync.listener.SynchronizationMessageDto.S3Action;
import it.s3sync.listener.WatchListeners.Operation;
import it.s3sync.model.Item;
import it.s3sync.service.SynchronizationService;
import it.s3sync.service.UploadService;
import it.s3sync.utils.FileUtils;
import it.s3sync.utils.UserSpecificPropertiesManager;

@Service
public class AmqpSyncListener {

	@Autowired
	@Qualifier("sqsJsonMapper")
	private JsonMapper jsonMapper;

	private static final Logger logger = LoggerFactory.getLogger(AmqpSyncListener.class);

	@Autowired
	private UploadService uploadService;

	@Autowired
	private SynchronizationService synchronizationService;
	
	@Autowired
	private MongoOperations mongoOperations;

	public void receiveSyncMessage(SynchronizationMessageDto dto) {
		// lock event on file
		WatchListeners.lockSemaphore();
		logger.info("Locking WatchListeners");
		try {
			if (!dto.getSource().equals(UserSpecificPropertiesManager.getConfiguration().getAlias())) {
				String localFolder = synchronizationService.getSynchronizedLocalRootFolderByRemoteFolder(dto.getRemoteFolder());
				if (localFolder != null) {
					logger.info("Serving action: {}", dto.toString());
					if (S3Action.CREATE.equals(dto.getS3Action()) || S3Action.MODIFY.equals(dto.getS3Action())) {
						Item item = mongoOperations.findOne(
								new Query(Criteria.where("ownedByFolder").is(dto.getRemoteFolder()).and("originalName").is(dto.getFile())),
								Item.class);
						List<String> folders = uploadService.getOrUpdate(localFolder + dto.getFile(), item);
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
								operation.setS3Action(dto.getS3Action());
							}
						}						
						Operation operation = new Operation();
						operation.setOnFile(localFolder + dto.getFile());
						operation.setS3Action(dto.getS3Action());
					} else if (S3Action.DELETE.equals(dto.getS3Action())) {
						long localFileLastModified = Path.of(localFolder + dto.getFile()).toFile().lastModified();
						if (localFileLastModified <= dto.getTime()) {
							List<String> toDelete = FileUtils.toDeleteFileAndEmptyTree(localFolder + dto.getFile());
							for (String s : toDelete) {
								// start as separate thread to prevent lock (the thread is probably waiting for
								// watchService.poll operation
								new Thread(new RemoveWatchKey(localFolder, s)).start();
							}
						} else {
							logger.info("Local file is newer than the remote one: {} -> {}", localFileLastModified,
									dto.getTime());
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

	public class AddNewWatchKey implements Runnable {

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

	public class RemoveWatchKey implements Runnable {

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
