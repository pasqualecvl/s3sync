package it.pasqualecavallo.s3sync.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import it.pasqualecavallo.s3sync.listener.WatchListeners;
import it.pasqualecavallo.s3sync.model.AttachedClient;
import it.pasqualecavallo.s3sync.model.AttachedClient.SyncFolder;
import it.pasqualecavallo.s3sync.model.Item;
import it.pasqualecavallo.s3sync.utils.FileUtils;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;
import it.pasqualecavallo.s3sync.web.controller.advice.exception.InternalServerErrorException;
import it.pasqualecavallo.s3sync.web.dto.response.RestBaseResponse.ErrorMessage;

/**
 * This service is responsible to synchronize local and remote data on startup.
 * 
 * @author pasquale
 *
 */
@Service
public class SynchronizationService {

	@Autowired
	private MongoOperations mongoOperations;

	@Autowired
	private UploadService uploadService;

	private static volatile Map<String, String> synchronizedFolder = new HashMap<>();
	private static volatile Map<String, List<Pattern>> exclusionPatterns = new HashMap<>();

	private static final Logger logger = LoggerFactory.getLogger(SynchronizationService.class);

	@PostConstruct
	private void synchronizeOnStartup() {
		logger.info("Run startup synchronization. Lock listeners semaphore");
		WatchListeners.lockSemaphore();
		try {
			AttachedClient client = UserSpecificPropertiesManager.getConfiguration();
			// fast fill synchronizedFolder map
			client.getSyncFolder().forEach(folder -> {
				logger.debug("[[DEBUG]] Add {} -> {} to synchronizationFolders map", folder.getLocalPath(),
						folder.getRemotePath());
				synchronized (synchronizedFolder) {
					synchronizedFolder.put(folder.getLocalPath(), folder.getRemotePath());
				}
				synchronized (exclusionPatterns) {
					exclusionPatterns.put(folder.getLocalPath(), folder.getExclusionPattern() != null
							? folder.getExclusionPattern().stream().map(pattern -> {
								logger.debug("[[DEBUG]] Add exclusion pattern {} to {} -> {}", pattern,
										folder.getLocalPath(), folder.getRemotePath());
								return Pattern.compile(pattern);
							}).collect(Collectors.toList())
							: new ArrayList<>());
				}
			});
			if (client.getClientConfiguration().isRunSynchronizationOnStartup()) {
				client.getSyncFolder().forEach(folder -> {
					synchronize(folder.getRemotePath(), folder.getLocalPath());
				});
			} else {
				logger.debug("[[DEBUG]] Synchronization on startup is disabled.");
			}
		} finally {
			logger.info("Finished. Release lock");
			WatchListeners.releaseSemaphore();
		}
	}

	public void synchronize(String remoteFolder, String localRootFolder) {
		try {
			// DO NOT CHANGE THE ORDER
			logger.info("[[INFO]] Run startup synchronization from local {} to s3 {}", localRootFolder, remoteFolder);
			localToS3Sync(remoteFolder, localRootFolder);
			logger.info("[[INFO]] Run startup synchronization from s3 {} to local {}", remoteFolder, localRootFolder);
			s3toLocalSync(remoteFolder, localRootFolder);
		} catch (IOException e) {
			throw new InternalServerErrorException(ErrorMessage.E500_SYNC_ERROR,
					"Unable to synchronize local and remote folders for: " + localRootFolder + " -> " + remoteFolder);
		}

	}

	private void localToS3Sync(String remoteFolder, String localRootFolder) throws IOException {
		List<Item> items = mongoOperations.find(new Query(Criteria.where("ownedByFolder").is(remoteFolder)),
				Item.class);
		Map<String, Item> mapItems = items.stream().collect(Collectors.toMap(Item::getOriginalName, Item::get));
		Files.walk(Paths.get(localRootFolder)).forEach(path -> {
			File file = path.toFile();
			if (file.isFile() && file.canRead() && FileUtils.notMatchFilters(exclusionPatterns.get(localRootFolder),
					path.toString().replaceFirst(localRootFolder, ""))) {
				String relativePath = file.getAbsolutePath().replaceFirst(localRootFolder, "");
				if (!mapItems.containsKey(relativePath) || (mapItems.containsKey(relativePath)
						&& mapItems.get(relativePath).getLastUpdate() < file.lastModified())) {
					logger.trace("[[TRACE]] Uploading file {}", path.toString());
					uploadService.upload(path, relativePath, remoteFolder, mapItems.get(relativePath));
				} else {
					logger.debug("[[DEBUG]] {} will not be synchronized because it is oldest than the remote one",path.toString());
				}
			} else {
				logger.debug("[[DEBUG]] {} will not be synchronized because missing prerequisite (e.g. exclusion pattern)", path.toString());
			}
		});
	}

	private void s3toLocalSync(String remoteFolder, String localRootFolder) {
		List<Item> items = mongoOperations.find(new Query(Criteria.where("ownedByFolder").is(remoteFolder)),
				Item.class);
		for (Item item : items) {
			if (!localRootFolder.endsWith("/") && !item.getOriginalName().startsWith("/")) {
				localRootFolder = localRootFolder + "/";
			}
			String itemLocalFullLocation = localRootFolder + item.getOriginalName();
			Path itemLocalFullPath = Paths.get(itemLocalFullLocation);
			if (FileUtils.notMatchFilters(exclusionPatterns.get(localRootFolder), item.getOriginalName())) {
				if (item.getDeleted()) {
					try {
						if (Files.exists(itemLocalFullPath) && Files.isWritable(itemLocalFullPath)) {
							Files.delete(itemLocalFullPath);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					if (item.getUploadedBy().equals(UserSpecificPropertiesManager.getConfiguration().getAlias())) {
						// deleted items -> uploaded by current user but not found on local machine
						if (!Path.of(localRootFolder + item.getOriginalName()).toFile().exists()) {
							if (!uploadService.delete(remoteFolder, item.getOriginalName())) {
								System.out.println("Error deleting s3 file");
							}
						}
						continue;
					}
					// check for file exists
					// FIXME: apply regex filters here
					if (Files.exists(itemLocalFullPath)) {
						// check for obsolete file
						if (itemLocalFullPath.toFile().lastModified() < item.getLastUpdate()) {
							// file is obsolete, go for update
							uploadService.getOrUpdate(itemLocalFullLocation,
									item.getOwnedByFolder() + item.getOriginalName());
							// update created file last modified to prevent synchronization loop
							try {
								Files.setLastModifiedTime(itemLocalFullPath, FileTime.fromMillis(item.getLastUpdate()));
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						// file exists and up to date
					} else {
						uploadService.getOrUpdate(itemLocalFullLocation,
								item.getOwnedByFolder() + item.getOriginalName());
						try {
							Files.setLastModifiedTime(itemLocalFullPath, FileTime.fromMillis(item.getLastUpdate()));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public void removeSynchronizationFolder(String localRootFolder) {
		synchronized (synchronizedFolder) {
			synchronizedFolder.remove(localRootFolder);
		}
		WatchListeners.stopThread(localRootFolder);
		AttachedClient client = UserSpecificPropertiesManager.getConfiguration();

		for (SyncFolder folder : client.getSyncFolder()) {
			if (folder.getLocalPath().equals(localRootFolder)) {
				client.getSyncFolder().remove(folder);
				break;
			}
		}
		UserSpecificPropertiesManager.setConfiguration(client);
	}

	public String getSynchronizedRemoteFolderByLocalRootFolder(String localRootFolder) {
		return synchronizedFolder.get(localRootFolder);
	}

	public String getSynchronizedLocalRootFolderByRemoteFolder(String remoteFolder) {
		for (Entry<String, String> entry : synchronizedFolder.entrySet()) {
			if (entry.getValue().equals(remoteFolder)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public List<Pattern> getExclusionPattern(String rootLocalFolder) {
		return exclusionPatterns.get(rootLocalFolder);
	}

	public List<String> addSynchronizationExclusionPattern(String localFolder, String regexp) {
		List<String> toReturn = new ArrayList<>();
		List<Pattern> patterns = exclusionPatterns.get(localFolder);
		boolean present = false;
		for (Pattern pattern : patterns) {
			String patternString = pattern.toString();
			toReturn.add(patternString);
			if (pattern.toString().equals(regexp)) {
				present = true;
			}
		}
		synchronized (exclusionPatterns) {
			if (!present) {
				toReturn.add(regexp);
				patterns.add(Pattern.compile(regexp));
			}
		}
		if (!present) {
			AttachedClient currentUser = UserSpecificPropertiesManager.getConfiguration();
			List<String> exclusionPatterns = null;
			SyncFolder syncFolder = null;
			for (SyncFolder folder : currentUser.getSyncFolder()) {
				if (folder.getLocalPath().equals(localFolder)) {
					syncFolder = folder;
					if (folder.getExclusionPattern() != null) {
						exclusionPatterns = folder.getExclusionPattern();
						for (String regexpSaved : folder.getExclusionPattern()) {
							if (regexpSaved.equals(regexp)) {
								exclusionPatterns = null;
								break;
							}
						}
						if (exclusionPatterns == null) {
							break;
						}
					} else {
						exclusionPatterns = new ArrayList<>();
					}
				}

			}
			if (exclusionPatterns != null && syncFolder != null) {
				exclusionPatterns.add(regexp);
				syncFolder.setExclusionPattern(Collections.synchronizedList(exclusionPatterns));
				UserSpecificPropertiesManager.setConfiguration(currentUser);
			}
		}
		return toReturn;
	}

	public List<String> removeSynchronizationExclusionPattern(String localFolder, String regexp) {
		List<String> toReturn = new ArrayList<>();
		List<Pattern> patterns = exclusionPatterns.get(localFolder);
		Pattern foundPattern = null;
		for (Pattern pattern : patterns) {
			String patternString = pattern.toString();
			if (pattern.toString().equals(regexp)) {
				foundPattern = pattern;
			} else {
				toReturn.add(patternString);
			}
		}
		synchronized (exclusionPatterns) {
			if (foundPattern != null) {
				patterns.remove(foundPattern);
			}
		}

		if (foundPattern != null) {
			AttachedClient currentUser = UserSpecificPropertiesManager.getConfiguration();
			for (SyncFolder folder : currentUser.getSyncFolder()) {
				if (folder.getLocalPath().equals(localFolder)) {
					if (folder.getExclusionPattern() != null) {
						folder.getExclusionPattern().remove(regexp);
						UserSpecificPropertiesManager.setConfiguration(currentUser);
					}
				}
			}
		}
		return toReturn;
	}

	public void cacheSynchronizationFolder(String remotePath, String localPath) {
		synchronized (synchronizedFolder) {
			synchronizedFolder.put(localPath, remotePath);
		}
	}

}
