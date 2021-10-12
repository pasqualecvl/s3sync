package it.pasqualecavallo.s3sync.service;

import java.util.ArrayList;
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
import it.pasqualecavallo.s3sync.web.controller.advice.exception.BadRequestException;
import it.pasqualecavallo.s3sync.web.controller.advice.exception.InternalServerErrorException;
import it.pasqualecavallo.s3sync.web.dto.response.AddExclusionPatterResponse;
import it.pasqualecavallo.s3sync.web.dto.response.AddFolderResponse;
import it.pasqualecavallo.s3sync.web.dto.response.ListSyncFoldersResponse;
import it.pasqualecavallo.s3sync.web.dto.response.RemoveFolderResponse;
import it.pasqualecavallo.s3sync.web.dto.response.RestBaseResponse.ErrorMessage;
import it.pasqualecavallo.s3sync.web.dto.response.SyncFolderResponse;

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
		for (SyncFolder folder : folders) {
			if (folder.getLocalPath().equals(localPath) || folder.getLocalPath().startsWith(localPath + "/")) {
				foundFolder = folder;
				break;
			}
		}
		if (foundFolder == null) {
			addToPersistence(client, localPath, remotePath);
			synchronizationService.synchronize(remotePath, localPath);
			startListenerThread(remotePath, localPath);
			AddFolderResponse response = new AddFolderResponse();
			response.setLocalFolder(localPath);
			response.setRemoteFolder(remotePath);
			return response;
		} else {
			throw new BadRequestException(ErrorMessage.E400_BAD_REQUEST, "Folder is alredy in sync");
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
		WatchListeners.startThread(uploadService, synchronizationService, remoteFolder, localRootFolder);
	}

	public ListSyncFoldersResponse listFolders(Integer page, Integer pageSize) {
		String clientAlias = UserSpecificPropertiesManager.getProperty("client.alias");
		AttachedClient client = mongoOperations.findOne(new Query(Criteria.where("alias").is(clientAlias)),
				AttachedClient.class);
		ListSyncFoldersResponse response = new ListSyncFoldersResponse();
		List<SyncFolderResponse> responseList = new ArrayList<>();
		List<SyncFolder> syncFolders = client.getSyncFolder();
		List<SyncFolder> subSyncFolders = new ArrayList<>();
		int listSize = syncFolders != null ? syncFolders.size() : 0;
		if (listSize != 0 && page < (int) Math.ceil((double) listSize / (double) pageSize)) {
			if (listSize > ((page + 1) * pageSize)) {
				subSyncFolders = syncFolders.subList(pageSize * page, pageSize * (page + 1));
			} else {
				subSyncFolders = syncFolders.subList(pageSize * page, listSize);
			}
		}

		for (SyncFolder folder : subSyncFolders) {
			SyncFolderResponse responseItem = new SyncFolderResponse();
			responseItem.setLocalFolder(folder.getLocalPath());
			responseItem.setRemoteFolder(folder.getRemotePath());
			responseItem.setExclusionPatterns(folder.getExclusionPattern());
			responseList.add(responseItem);
		}
		response.setList(responseList);
		return response;
	}

	public RemoveFolderResponse removeFolder(String remoteFolder) {
		String localFolder = synchronizationService.getSynchronizedLocalRootFolderByRemoteFolder(remoteFolder);
		if(localFolder != null) {
			synchronizationService.removeSynchronizationFolder(localFolder);			
			return new RemoveFolderResponse();
		} else {
			throw new InternalServerErrorException(ErrorMessage.E500_GENERIC_ERROR);
		}
	}

	public AddExclusionPatterResponse addExclusionPattern(String regexp, String remoteFolder) {
		String localFolder = synchronizationService.getSynchronizedLocalRootFolderByRemoteFolder(remoteFolder);
		if(localFolder != null) {
			List<String> patterns = synchronizationService.addSynchronizationExclusionPattern(localFolder, regexp);
			AddExclusionPatterResponse response = new AddExclusionPatterResponse();
			response.setPatterns(patterns);
			response.setLocalFolder(localFolder);
			response.setRemoteFolder(remoteFolder);
			return response;
		} else {
			throw new InternalServerErrorException(ErrorMessage.E500_GENERIC_ERROR);
		}		
	}
}