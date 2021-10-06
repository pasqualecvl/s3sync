package it.pasqualecavallo.s3sync.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import it.pasqualecavallo.s3sync.model.AttachedClient;
import it.pasqualecavallo.s3sync.model.Item;
import it.pasqualecavallo.s3sync.model.AttachedClient.SyncFolder;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;

/**
 * This service is responsible to synchronize local and remote data on startup.
 * @author pasquale
 *
 */
@Service
public class SynchronizationService {

	@Autowired
	private MongoOperations mongoOperations;

	@Autowired
	private UploadService uploadService;

	@PostConstruct
	public void synchronizeOnStartup() {
		AttachedClient client = mongoOperations.findOne(
				new Query(Criteria.where("alias").is(
						UserSpecificPropertiesManager.getProperty("client.alias"))), AttachedClient.class);
		for(SyncFolder folder : client.getSyncFolder()) {
			synchronize(folder.getRemotePath(), folder.getLocalPath());
		}
	}

	public void synchronize(String remoteFolder, String localRootFolder) {
		List<Item> items = mongoOperations.find(new Query(Criteria.where("ownedByFolder").is(remoteFolder)),
				Item.class);
		Map<String, Item> mapItems = items.stream().collect(Collectors.toMap(Item::getOriginalName, Item::get));
		try {
			localToS3Sync(remoteFolder, localRootFolder, mapItems);
			if (items != null) {
				s3toLocalSync(remoteFolder, localRootFolder, items);
			}
		} catch (IOException e) {
			throw new RuntimeException("Exception synchronizing files", e);
		}

	}

	private void localToS3Sync(String remoteFolder, String localRootFolder, Map<String, Item> items)
			throws IOException {
		Files.walk(Paths.get(localRootFolder)).forEach(path -> {
			File file = path.toFile();
			if (file.isFile() && file.canRead()) {
				// FIXME: apply regex filters here
				String relativePath = file.getAbsolutePath().replaceFirst(localRootFolder, "");
				if (!items.containsKey(relativePath) || items.get(relativePath).getLastUpdate() < file.lastModified()) {
					uploadService.upload(path, relativePath, remoteFolder, items.get(relativePath));
				}
			}
		});
	}

	private void s3toLocalSync(String remoteFolder, String localRootFolder, List<Item> items) {
		for (Item item : items) {
			if (item.getUploadedBy().equals(UserSpecificPropertiesManager.getProperty("client.alias"))) {
				continue;
			}
			if (!localRootFolder.endsWith("/") && !item.getOriginalName().startsWith("/")) {
				localRootFolder = localRootFolder + "/";
			}
			String itemLocalFullLocation = localRootFolder + item.getOriginalName();
			Path itemLocalFullPath = Paths.get(itemLocalFullLocation);
			// check for file exists
			// FIXME: apply regex filters here
			if (Files.exists(itemLocalFullPath)) {
				// check for obsolete file
				if (itemLocalFullPath.toFile().lastModified() < item.getLastUpdate()) {
					// file is obsolete, go for update
					uploadService.getOrUpdate(itemLocalFullLocation,
							item.getOwnedByFolder() + "/" + item.getOriginalName());
				}
				// file exists and up to date
			} else {
				uploadService.getOrUpdate(itemLocalFullLocation,
						item.getOwnedByFolder() + "/" + item.getOriginalName());
			}
		}
	}
}
