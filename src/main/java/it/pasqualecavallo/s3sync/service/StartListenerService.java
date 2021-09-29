package it.pasqualecavallo.s3sync.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import it.pasqualecavallo.s3sync.listener.WatchListeners;
import it.pasqualecavallo.s3sync.model.AttachedClient;
import it.pasqualecavallo.s3sync.model.AttachedClient.SyncFolder;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;

@Service
public class StartListenerService {

	@Autowired
	private MongoOperations mongoOperations;

	@PostConstruct
	public void startListeners() {
		AttachedClient client = mongoOperations.findOne(
				new Query(Criteria.where("alias")
						.is(UserSpecificPropertiesManager.getProperty("s3sync.installation_alias"))),
				AttachedClient.class);
		List<SyncFolder> syncFolders = client.getSyncFolder();
		for(SyncFolder folder : syncFolders) {
			WatchListeners.startThread(folder.getLocalPath());
		}
	}
}
