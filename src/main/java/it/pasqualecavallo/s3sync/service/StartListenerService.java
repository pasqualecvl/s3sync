package it.pasqualecavallo.s3sync.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
	
	@PostConstruct
	public void startListeners() {
		String clientAlias = UserSpecificPropertiesManager.getProperty("client.alias");
		AttachedClient client = mongoOperations.findOne(
				new Query(Criteria.where("alias")
						.is(clientAlias)),
				AttachedClient.class);
		if(client == null) {
			AttachedClient attachedClient = new AttachedClient();
			attachedClient.setAlias(clientAlias);
			attachedClient.setSyncFolder(new ArrayList<>());
			mongoOperations.insert(attachedClient);
		} else {
			List<SyncFolder> syncFolders = client.getSyncFolder();
			for(SyncFolder folder : syncFolders) {
				WatchListeners.startThread(uploadService, folder.getRemotePath(), folder.getLocalPath());
			}
		}
	}
	
	@Scheduled(fixedDelay = 10000)
	public void log() {
		WatchListeners.log();
	}
}
