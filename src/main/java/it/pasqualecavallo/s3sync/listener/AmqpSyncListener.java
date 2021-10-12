package it.pasqualecavallo.s3sync.listener;

import java.nio.file.Path;

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

	@Autowired
	private UploadService uploadService;

	public void receiveSyncMessage(SynchronizationMessageDto dto) {
		WatchListeners.lockSemaphore();
		try {
//			SynchronizationMessageDto dto = jsonMapper.readValue(message, SynchronizationMessageDto.class);
			if(!dto.getSource().equals(UserSpecificPropertiesManager.getProperty("client.alias"))) {
				String localFolder = SynchronizationService
						.getSynchronizedLocalRootFolderByRemoteFolder(dto.getRemoteFolder());
				if(localFolder != null) {
					if (S3Action.CREATE.equals(dto.getS3Action()) || S3Action.MODIFY.equals(dto.getS3Action())) {
						uploadService.getOrUpdate(localFolder + dto.getFile(), dto.getRemoteFolder() + dto.getFile());
					} else if (S3Action.DELETE.equals(dto.getS3Action())) {
						if (Path.of(localFolder + dto.getFile()).toFile().lastModified() <= dto.getTime()) {
							FileUtils.deleteFileAndEmptyTree(localFolder + dto.getFile());
						}
					}
				}				
			}
		} finally {
			WatchListeners.releaseSemaphore();
		}
	}

}
