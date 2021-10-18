package it.pasqualecavallo.s3sync.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import it.pasqualecavallo.s3sync.listener.WatchListeners;
import it.pasqualecavallo.s3sync.model.AttachedClient;
import it.pasqualecavallo.s3sync.model.AttachedClient.SyncFolder;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;

@Service
public class StartListenerService {

	@Autowired
	private MongoOperations mongoOperations;

	@Autowired
	private UploadService uploadService;

	@Autowired
	private SynchronizationService synchronizationService;

	private static final Logger logger = LoggerFactory.getLogger(StartListenerService.class);

	@PostConstruct
	public void startListeners() {
		AttachedClient client = UserSpecificPropertiesManager.getConfiguration();
		if (client == null) {
			logger.warn("[[WARN]] Client not found, but might be. Create a new one.");
			AttachedClient attachedClient = new AttachedClient();
			attachedClient.setAlias(UUID.randomUUID().toString());
			attachedClient.setSyncFolder(new ArrayList<>());
			UserSpecificPropertiesManager.setConfiguration(attachedClient);
			client = attachedClient;
		}
		List<SyncFolder> syncFolders = client.getSyncFolder();
		logger.info("[[INFO]] Starting {}# listeners", syncFolders.size());
		for (SyncFolder folder : syncFolders) {
			logger.debug("[[DEBUG]] Start thread for remote folder {} sync with local folder {}",
					folder.getRemotePath(), folder.getLocalPath());
			WatchListeners.startThread(uploadService, synchronizationService, folder.getRemotePath(),
					folder.getLocalPath());
		}

	}

	@Scheduled(fixedDelay = 60000)
	public void log() {
		WatchListeners.log();
	}
}
