package it.s3sync.listener;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.json.JsonMapper;

import it.s3sync.listener.SynchronizationMessageDto.S3Action;
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

	public void receiveSyncMessage(SynchronizationMessageDto dto) {
		WatchListeners.lockSemaphore();
		logger.info("Locking WatchListeners");
		try {
			if(!dto.getSource().equals(UserSpecificPropertiesManager.getConfiguration().getAlias())) {
				String localFolder = synchronizationService
						.getSynchronizedLocalRootFolderByRemoteFolder(dto.getRemoteFolder());
				if(localFolder != null) {
					WatchListeners.putChangesWhileLocked(localFolder, dto.getFile());
					logger.info("Serving action: " + dto.toString());
					if (S3Action.CREATE.equals(dto.getS3Action()) || S3Action.MODIFY.equals(dto.getS3Action())) {
						List<String> folders = uploadService.getOrUpdate(localFolder + dto.getFile(), dto.getRemoteFolder() + dto.getFile());
						for(String folder : folders) {
							//start as separate thread to prevent lock (the thread is probably waiting for watchService.poll operation
							new Thread(new AddNewWatchKey(localFolder, folder)).start();							
						}
					} else if (S3Action.DELETE.equals(dto.getS3Action())) {
						long localFileLastModified = Path.of(localFolder + dto.getFile()).toFile().lastModified();
						if (localFileLastModified <= dto.getTime()) {
							List<String> deleted = FileUtils.deleteFileAndEmptyTree(localFolder + dto.getFile());
							for(String s : deleted) {
								//start as separate thread to prevent lock (the thread is probably waiting for watchService.poll operation
								new Thread(new RemoveWatchKey(localFolder, s)).start();
							}
						} else {
							logger.info("Local file is newer than the remote one: {} -> {}", localFileLastModified, dto.getTime());
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
			((WatchListener)WatchListeners.getThread(localRootFolder).getR()).addFolder(folder);
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
			((WatchListener)WatchListeners.getThread(localRootFolder).getR()).removeFolder(folder);
		}		
	}

}
