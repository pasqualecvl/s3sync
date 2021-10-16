package it.pasqualecavallo.s3sync.listener;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.json.JsonMapper;

import it.pasqualecavallo.s3sync.listener.SynchronizationMessageDto.S3Action;
import it.pasqualecavallo.s3sync.service.SynchronizationService;
import it.pasqualecavallo.s3sync.service.UploadService;
import it.pasqualecavallo.s3sync.utils.FileUtils;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;

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
						uploadService.getOrUpdate(localFolder + dto.getFile(), dto.getRemoteFolder() + dto.getFile());
					} else if (S3Action.DELETE.equals(dto.getS3Action())) {
						long localFileLastModified = Path.of(localFolder + dto.getFile()).toFile().lastModified();
						if (localFileLastModified <= dto.getTime()) {
							FileUtils.deleteFileAndEmptyTree(localFolder + dto.getFile());
						} else {
							logger.info("Local file is newer than the remote one: " + localFileLastModified + " -> " + dto.getTime());
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

}
