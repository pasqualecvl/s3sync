package it.pasqualecavallo.s3sync.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import it.pasqualecavallo.s3sync.listener.WatchListeners;
import it.pasqualecavallo.s3sync.model.AttachedClient;
import it.pasqualecavallo.s3sync.model.AttachedClient.SyncFolder;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;
import it.pasqualecavallo.s3sync.web.dto.response.AddFolderResponse;
import it.pasqualecavallo.s3sync.web.dto.response.RestBaseResponse.ErrorMessage;

@Service
public class ManageFolderService {

	@Autowired
	private MongoOperations mongoOperations;

	@Autowired
	private SynchronizationService synchronizationService;
	
	@Autowired
	private UploadService uploadService;
	
	public AddFolderResponse addFolder(String localPath, String remotePath) {
		String clientAlias = UserSpecificPropertiesManager.getProperty("client.alias");
		AttachedClient client = mongoOperations.findOne(new Query(Criteria.where("alias").is(clientAlias)),
				AttachedClient.class);
		List<SyncFolder> folders = client.getSyncFolder();
		SyncFolder foundFolder = null;
		for(SyncFolder folder : folders) {
			if (folder.getLocalPath().startsWith(localPath)) {
				foundFolder = folder;
				break;
			}			
		}
		if (foundFolder == null) {
			addToPersistence(client, localPath, remotePath);
			synchronizationService.synchronize(remotePath, localPath);
			startListenerThread(remotePath, localPath);
			return new AddFolderResponse();
		} else {
			AddFolderResponse response = new AddFolderResponse();
			response.setError(ErrorMessage.E400_BAD_REQUEST, "folder alredy in sync: " + foundFolder.getLocalPath());
		}
	}

	private void addToPersistence(AttachedClient client, String localPath, String remotePath) {
		SyncFolder folder = client.new SyncFolder();
		folder.setLocalPath(localPath);
		folder.setRemotePath(remotePath);
		client.getSyncFolder().add(folder);
		mongoOperations.save(client);
	}

	private void startListenerThread(String remoteFolder, String localRootFolder) {
		WatchListeners.startThread(uploadService, remoteFolder, localRootFolder);
	}
}