package it.pasqualecavallo.s3sync.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import it.pasqualecavallo.s3sync.listener.WatchListeners;
import it.pasqualecavallo.s3sync.model.AttachedClient;
import it.pasqualecavallo.s3sync.model.Item;
import it.pasqualecavallo.s3sync.model.AttachedClient.SyncFolder;
import it.pasqualecavallo.s3sync.utils.FileUtils;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;

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

	@PostConstruct
	public void synchronizeOnStartup() {
		WatchListeners.lockSemaphore();
		try {
			AttachedClient client = mongoOperations.findOne(
					new Query(Criteria.where("alias").is(UserSpecificPropertiesManager.getProperty("client.alias"))),
					AttachedClient.class);
			// fast fill synchronizedFolder map
			client.getSyncFolder().forEach(folder -> {
				synchronized (synchronizedFolder) {
					synchronizedFolder.put(folder.getLocalPath(), folder.getRemotePath());
				}
				synchronized (exclusionPatterns) {
					exclusionPatterns.put(folder.getLocalPath(), folder.getExclusionPattern() != null
							? folder.getExclusionPattern().stream().map(pattern -> {
								return Pattern.compile(pattern);
							}).collect(Collectors.toList())
							: new ArrayList<>());
				}
			});
			if(client.getClientConfiguration().isRunSynchronizationOnStartup()) {
				client.getSyncFolder().forEach(folder -> {
					synchronize(folder.getRemotePath(), folder.getLocalPath());
				});				
			}
		} finally {
			WatchListeners.releaseSemaphore();
		}
	}

	public void synchronize(String remoteFolder, String localRootFolder) {
		try {
			// DO NOT CHANGE THE ORDER
			localToS3Sync(remoteFolder, localRootFolder);
			s3toLocalSync(remoteFolder, localRootFolder);
		} catch (IOException e) {
			throw new RuntimeException("Exception synchronizing files", e);
		}

	}

	private void localToS3Sync(String remoteFolder, String localRootFolder) throws IOException {
		List<Item> items = mongoOperations.find(new Query(Criteria.where("ownedByFolder").is(remoteFolder)),
				Item.class);
		Map<String, Item> mapItems = items.stream().collect(Collectors.toMap(Item::getOriginalName, Item::get));
		Files.walk(Paths.get(localRootFolder)).forEach(path -> {
			File file = path.toFile();
			if (file.isFile() && file.canRead()
					&& FileUtils.notMatchFilters(exclusionPatterns.get(localRootFolder), path.toString().replaceFirst(localRootFolder, ""))) {
				// FIXME: apply regex filters here
				String relativePath = file.getAbsolutePath().replaceFirst(localRootFolder, "");
				if (!mapItems.containsKey(relativePath) || (mapItems.containsKey(relativePath)
						&& mapItems.get(relativePath).getLastUpdate() < file.lastModified())) {
					uploadService.upload(path, relativePath, remoteFolder, mapItems.get(relativePath));
				}
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
					if (item.getUploadedBy().equals(UserSpecificPropertiesManager.getProperty("client.alias"))) {
						//deleted items -> uploaded by current user but not found on local machine
						if(!Path.of(localRootFolder + item.getOriginalName()).toFile().exists()) {
							uploadService.delete(remoteFolder, item.getOriginalName());
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
		AttachedClient client = mongoOperations.findOne(
				new Query(Criteria.where("alias").is(UserSpecificPropertiesManager.getProperty("client.alias"))),
				AttachedClient.class);

		for (SyncFolder folder : client.getSyncFolder()) {
			if (folder.getLocalPath().equals(localRootFolder)) {
				client.getSyncFolder().remove(folder);
				break;
			}
		}
		mongoOperations.save(client);
	}

	public String getSynchronizedRemoteFolderByLocalRootFolder(String localRootFolder) {
		return synchronizedFolder.get(localRootFolder);
	}

	public static String getSynchronizedLocalRootFolderByRemoteFolder(String remoteFolder) {
		for (Entry<String, String> entry : synchronizedFolder.entrySet()) {
			if (entry.getValue().equals(remoteFolder)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public static List<Pattern> getExclusionPattern(String rootLocalFolder) {
		return exclusionPatterns.get(rootLocalFolder);
	}

}
