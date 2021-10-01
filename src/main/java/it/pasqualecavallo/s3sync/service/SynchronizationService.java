package it.pasqualecavallo.s3sync.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import it.pasqualecavallo.s3sync.model.Item;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;

@Service
public class SynchronizationService {

	@Autowired
	private MongoOperations mongoOperations;

	@Autowired
	private UploadService uploadService;
	
	public void synchronize(String remoteFolder, String localRootFolder) {
		List<Item> items = mongoOperations.find(new Query(Criteria.where("ownedByFolder").is(remoteFolder)),
				Item.class);
		Map<String, Item> mapItems = items.stream().collect(Collectors.toMap(Item::getOriginalName, Item::get));
		if (items != null) {
			try {
				localToS3Sync(remoteFolder, localRootFolder, mapItems);
				s3toLocalSync(remoteFolder, localRootFolder, items);
			} catch (IOException e) {
				throw new RuntimeException("Exception synchronizing files", e);
			}
		}
	}

	private void localToS3Sync(String remoteFolder, String localRootFolder, Map<String, Item> items) throws IOException {
		Files.walk(Paths.get(localRootFolder)).forEach(path -> {
			File f = path.toFile();
			if(f.isFile() && f.canRead()) {
				String relativePath = f.getAbsolutePath().replaceFirst(localRootFolder, "");
				if(!items.containsKey(relativePath) || items.get(relativePath).getLastUpdate() < f.lastModified()) {
					uploadService.s3Upload(f, relativePath, remoteFolder);
				}
			}
		});
	}

	private void s3toLocalSync(String remoteFolder, String localRootFolder, List<Item> items) {
		for (Item item : items) {
			if(item.getUploadedBy().equals(UserSpecificPropertiesManager.getProperty("client.alias"))) {
				continue;
			}
			if (!localRootFolder.endsWith("/") && !item.getOriginalName().startsWith("/")) {
				localRootFolder = localRootFolder + "/";
			}
			String itemLocalFullLocation = localRootFolder + item.getOriginalName();
			Path itemLocalFullPath = Paths.get(itemLocalFullLocation);
			// check for file exists
			// FIXME: apply regexs filter here
			if (Files.exists(itemLocalFullPath)) {
				// check for obsolete file
				if (itemLocalFullPath.toFile().lastModified() < item.getLastUpdate()) {
					// file is obsolete, go for update
					uploadService.getOrUpdate(itemLocalFullPath, remoteFolder, item.getS3Name());
				}
				// file exists and up to date
			} else {
				uploadService.getOrUpdate(itemLocalFullPath, remoteFolder, item.getS3Name());
			}
		}
	}
}
