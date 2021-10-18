package it.pasqualecavallo.s3sync.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Service;

import it.pasqualecavallo.s3sync.listener.WatchListeners;
import it.pasqualecavallo.s3sync.model.AttachedClient;
import it.pasqualecavallo.s3sync.model.AttachedClient.SyncFolder;
import it.pasqualecavallo.s3sync.model.SharedData;
import it.pasqualecavallo.s3sync.utils.ListUtils;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;
import it.pasqualecavallo.s3sync.web.controller.advice.exception.BadRequestException;
import it.pasqualecavallo.s3sync.web.controller.advice.exception.InternalServerErrorException;
import it.pasqualecavallo.s3sync.web.dto.response.AddExclusionPatterResponse;
import it.pasqualecavallo.s3sync.web.dto.response.AddFolderResponse;
import it.pasqualecavallo.s3sync.web.dto.response.ListRemoteFolderResponse;
import it.pasqualecavallo.s3sync.web.dto.response.ListSyncFoldersResponse;
import it.pasqualecavallo.s3sync.web.dto.response.RemoveExclusionPatterResponse;
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

	private static final Logger logger = LoggerFactory.getLogger(ManageFolderService.class); 
	
	public AddFolderResponse addFolder(String localPath, String remotePath) {
		AttachedClient client = UserSpecificPropertiesManager.getConfiguration();
		List<SyncFolder> folders = client.getSyncFolder();
		SyncFolder foundFolder = null;
		for (SyncFolder folder : folders) {
			if (folder.getLocalPath().equals(localPath) || folder.getLocalPath().startsWith(localPath + "/")) {
				foundFolder = folder;
				break;
			}
		}
		if (foundFolder == null) {
			logger.info("[[INFO]] Add folder {} -> {}", localPath, remotePath);
			synchronizationService.synchronize(remotePath, localPath);
			synchronizationService.cacheSynchronizationFolder(remotePath, localPath);
			addToPersistence(client, localPath, remotePath);
			startListenerThread(remotePath, localPath);
			addRemoteFolder(remotePath);
			AddFolderResponse response = new AddFolderResponse();
			response.setLocalFolder(localPath);
			response.setRemoteFolder(remotePath);
			return response;
		} else {
			throw new BadRequestException(ErrorMessage.E400_BAD_REQUEST, "Folder is alredy in sync");
		}
	}

	private void addRemoteFolder(String remotePath) {
		List<SharedData> data = mongoOperations.findAll(SharedData.class);
		if(data.size() == 0) {
			logger.debug("[[DEBUG]] Folder {} not found in SharedDatas because SharedData is currently null", remotePath);
			SharedData item = new SharedData();
			item.setRemoteFolders(Arrays.asList(remotePath));
			mongoOperations.save(item);
			logger.info("[[INFO]] Saved {} as remote folder in SharedData", remotePath);
		} else if(data.size() > 1) {
			throw new RuntimeException("SharedData must contains exactly one document");
		} else {
			SharedData item = data.get(0);
			if(item.getRemoteFolders().stream().filter(string -> string.equals(remotePath)).count() == 0L) {
				item.getRemoteFolders().add(remotePath);
				mongoOperations.save(item);
				logger.info("[[INFO]] Saved {} as remote folder in SharedData", remotePath);				
			}
		}
	}

	private void addToPersistence(AttachedClient client, String localPath, String remotePath) {
		SyncFolder folder = new SyncFolder();
		folder.setLocalPath(localPath);
		folder.setRemotePath(remotePath);
		client.getSyncFolder().add(folder);
		UserSpecificPropertiesManager.setConfiguration(client);
		logger.info("[[INFO]] Added sync folder configuration to config file");
	}

	private void startListenerThread(String remoteFolder, String localRootFolder) {
		logger.debug("[[DEBUG]] Starting listener on {} -> {}", localRootFolder, remoteFolder);
		WatchListeners.startThread(uploadService, synchronizationService, remoteFolder, localRootFolder);
	}

	public ListSyncFoldersResponse listFolders(Integer page, Integer pageSize) {
		AttachedClient client = UserSpecificPropertiesManager.getConfiguration();
		ListSyncFoldersResponse response = new ListSyncFoldersResponse();
		List<SyncFolderResponse> responseList = new ArrayList<>();
		List<SyncFolder> syncFolders = client.getSyncFolder();
		List<SyncFolder> subSyncFolders = ListUtils.getPageOf(syncFolders, page, pageSize);
		for (SyncFolder folder : subSyncFolders) {
			SyncFolderResponse responseItem = new SyncFolderResponse();
			responseItem.setLocalFolder(folder.getLocalPath());
			responseItem.setRemoteFolder(folder.getRemotePath());
			responseItem.setExclusionPatterns(folder.getExclusionPattern());
			logger.debug("[[DEBUG]] Adding folder to response: {}", responseItem.toString());
			responseList.add(responseItem);
		}
		response.setList(responseList);
		logger.info("[[INFO]] Returning with {} folders", response.getList().size());
		return response;
	}

	public RemoveFolderResponse removeFolder(String remoteFolder) {
		String localFolder = synchronizationService.getSynchronizedLocalRootFolderByRemoteFolder(remoteFolder);
		if(localFolder != null) {
			logger.info("[[INFO]] Removing synchronization folder {}", remoteFolder);
			synchronizationService.removeSynchronizationFolder(localFolder);			
			return new RemoveFolderResponse();
		} else {
			logger.error("[[ERROR]] Synchronization folder {} not found", remoteFolder);
			throw new InternalServerErrorException(ErrorMessage.E500_GENERIC_ERROR);
		}
	}

	public AddExclusionPatterResponse addExclusionPattern(String regexp, String remoteFolder) {
		logger.info("[[INFO]] Adding exclusion pattern {} from remote folder {}", regexp, remoteFolder);
		String localFolder = synchronizationService.getSynchronizedLocalRootFolderByRemoteFolder(remoteFolder);
		if(localFolder != null) {
			List<String> patterns = synchronizationService.addSynchronizationExclusionPattern(localFolder, regexp);
			logger.debug("[[DEBUG]] Added pattern {} from folder {}", regexp, remoteFolder);
			AddExclusionPatterResponse response = new AddExclusionPatterResponse();
			response.setPatterns(patterns);
			response.setLocalFolder(localFolder);
			response.setRemoteFolder(remoteFolder);
			return response;
		} else {
			logger.debug("[[DEBUG]] LocalFolder not found for remote folder {}", remoteFolder);
			throw new InternalServerErrorException(ErrorMessage.E500_GENERIC_ERROR);
		}		
	}

	public RemoveExclusionPatterResponse removeExclusionPattern(String regExp, String remoteFolder) {
		logger.info("[[INFO]] Removing exclusion pattern {} from remote folder {}", regExp, remoteFolder);
		String localFolder = synchronizationService.getSynchronizedLocalRootFolderByRemoteFolder(remoteFolder);
		if(localFolder != null) {
			List<String> patterns = synchronizationService.removeSynchronizationExclusionPattern(localFolder, regExp);
			logger.debug("[[DEBUG]] Removing pattern {} from folder {}", regExp, remoteFolder);			
			RemoveExclusionPatterResponse response = new RemoveExclusionPatterResponse();
			response.setExclusionPatterns(patterns);
			return response;
		} else {
			logger.debug("[[DEBUG]] LocalFolder not found for remote folder {}", remoteFolder);
			throw new InternalServerErrorException(ErrorMessage.E500_GENERIC_ERROR);
		}		
	}

	public ListRemoteFolderResponse listRemoteFolders(Integer page, Integer pageSize) {
		ListRemoteFolderResponse response = new ListRemoteFolderResponse();
		List<SharedData> data = mongoOperations.findAll(SharedData.class);
		if(data.size() != 1) {
			logger.debug("[[DEBUG]] Shared Folders is empty");
			response.setRemoteFolder(new ArrayList<>());
		} else {
			logger.debug("[[DEBUG]] Shared Folders size: {}", data.get(0).getRemoteFolders().size());
			response.setRemoteFolder(data.get(0).getRemoteFolders());
		}
		return response;
	}
	
	
}