package it.pasqualecavallo.s3sync.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.json.JsonMapper;

import it.pasqualecavallo.s3sync.listener.SynchronizationMessageDto;
import it.pasqualecavallo.s3sync.listener.SynchronizationMessageDto.S3Action;
import it.pasqualecavallo.s3sync.model.AttachedClient;
import it.pasqualecavallo.s3sync.model.Item;
import it.pasqualecavallo.s3sync.model.SharedData;
import it.pasqualecavallo.s3sync.utils.FileUtils;
import it.pasqualecavallo.s3sync.utils.GlobalPropertiesManager;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Service implementing methods to upload/download/delete files
 *
 */
@Service
public class UploadService {

	@Autowired
	private S3Client s3Client;

	@Autowired
	private MongoOperations mongoOperations;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private SynchronizationService synchronizationService;

	@Autowired
	@Qualifier("sqsJsonMapper")
	private JsonMapper jsonMapper;

	public void upload(Path path, String relativePath, String remoteFolder, Item item) {
		System.out.println(
				"Upload " + path.toString() + " to s3 folder: " + remoteFolder + " with relative path " + relativePath);
		try {
			PutObjectRequest objectRequest = PutObjectRequest.builder()
					.bucket(GlobalPropertiesManager.getProperty("s3.bucket")).key(remoteFolder + relativePath).build();
			PutObjectResponse response = s3Client.putObject(objectRequest, path);
			if (response.eTag() != null) {
				boolean isCreate = false;
				if (item == null) {
					System.out.println("Item not found, mark as creation");
					isCreate = true;
					item = new Item();
					item.setOriginalName(relativePath);
					item.setOwnedByFolder(remoteFolder);
				}
				item.setDeleted(false);
				item.setLastUpdate(System.currentTimeMillis());
				item.setUploadedBy(UserSpecificPropertiesManager.getConfiguration().getAlias());
				mongoOperations.save(item);
				SynchronizationMessageDto dto = new SynchronizationMessageDto();
				dto.setFile(relativePath);
				dto.setRemoteFolder(remoteFolder);
				dto.setSource(UserSpecificPropertiesManager.getConfiguration().getAlias());
				dto.setS3Action(isCreate ? S3Action.CREATE : S3Action.MODIFY);
				amqpTemplate.convertAndSend(dto);
			}
		} catch (UncheckedIOException e) {
			// File removed
			System.err.println("File removed");
		}
	}

	public void upload(Path path, String remoteFolder, String relativePath) {
		Item item = mongoOperations.findOne(
				new Query(Criteria.where("ownedByFolder").is(remoteFolder).and("originalName").is(relativePath)),
				Item.class);
		upload(path, relativePath, remoteFolder, item);

	}

	public void getOrUpdate(String localFullPathFolder, String remoteFullPathFolder) {
		System.out.println(
				"Fetch or update file: " + localFullPathFolder + " from remote folder " + remoteFullPathFolder);
		GetObjectRequest request = GetObjectRequest.builder().bucket(GlobalPropertiesManager.getProperty("s3.bucket"))
				.key(remoteFullPathFolder).build();
		ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
		try {
			FileUtils.createFileTree(localFullPathFolder, response.readAllBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean deleteTrashed(String relativePath) {
		String s3Bucket = GlobalPropertiesManager.getProperty("s3.bucket");
		String fileKey = "Trash/" + relativePath;
		DeleteObjectRequest s3request = DeleteObjectRequest.builder().bucket(s3Bucket).key(fileKey).build();
		DeleteObjectResponse response = s3Client.deleteObject(s3request);
		return response.sdkHttpResponse().isSuccessful(); 
	}

	public boolean delete(String remoteFolder, String relativePath) {
		System.out.println("Deleting object " + relativePath + "from s3 folder " + remoteFolder);
		String s3Bucket = GlobalPropertiesManager.getProperty("s3.bucket");
		String fileKey = remoteFolder + relativePath;
		if (UserSpecificPropertiesManager.getConfiguration().getClientConfiguration().isUseTrashOverDelete()) {
			CopyObjectRequest request = CopyObjectRequest.builder().sourceBucket(s3Bucket).sourceKey(fileKey)
					.destinationBucket(s3Bucket).destinationKey("Trash/" + fileKey).build();
			s3Client.copyObject(request);
		}
		DeleteObjectRequest s3request = DeleteObjectRequest.builder().bucket(s3Bucket).key(fileKey).build();
		DeleteObjectResponse response = s3Client.deleteObject(s3request);
		if(response.sdkHttpResponse().isSuccessful()) {
			System.out.println("Mark item as deleted");
			Item item = mongoOperations.findOne(
					new Query(Criteria.where("ownedByFolder").is(remoteFolder).and("originalName").is(relativePath)),
					Item.class);
			if (item != null) {
				// Item never sync
				item.setDeleted(true);
				item.setLastUpdate(System.currentTimeMillis());
				mongoOperations.save(item);
			}
	
			SynchronizationMessageDto dto = new SynchronizationMessageDto();
			dto.setFile(relativePath);
			dto.setRemoteFolder(remoteFolder);
			dto.setS3Action(S3Action.DELETE);
			dto.setSource(UserSpecificPropertiesManager.getConfiguration().getAlias());
			amqpTemplate.convertAndSend(dto);
			return true;
		} else {
			return false;
		}
	}

	public void deleteAsFolder(String remoteFolder, String relativeLocation) {
		AttachedClient currentUser = UserSpecificPropertiesManager.getConfiguration();

		// if recursive removal allowed, delete any items in folder locally and remote
		if (currentUser.getClientConfiguration().isPreventFolderRecursiveRemoval()) {
			if (relativeLocation.isBlank()) {
				// event on localRootFolder
				synchronizationService.removeSynchronizationFolder(
						synchronizationService.getSynchronizedLocalRootFolderByRemoteFolder(remoteFolder));
				removeRemoteFolder(remoteFolder);
			} else {
				synchronizationService.addSynchronizationExclusionPattern(
						synchronizationService.getSynchronizedLocalRootFolderByRemoteFolder(remoteFolder),
						"^" + relativeLocation);
			}
		} else {
			if (relativeLocation.isBlank()) {
				// localRootFolder -> pick all items in folder
				List<Item> toDelete = mongoOperations.find(new Query(Criteria.where("ownedByFolder").is(remoteFolder)),
						Item.class);
				toDelete.forEach(item -> {
					if(!delete(item.getOwnedByFolder(), item.getOriginalName())) {
						System.out.println("Error deleting S3 file");
					}
				});
			} else {
				// subfolder -> filter filename by regexp
				List<Item> toDelete = mongoOperations.find(new Query(Criteria.where("ownedByFolder").is(remoteFolder)
						.and("originalName").regex("/^" + relativeLocation + "/")), Item.class);
				toDelete.forEach(item -> {
					if(!delete(item.getOwnedByFolder(), item.getOriginalName())) {
						System.out.println("Error deleting S3 file");
					}
				});
			}
		}
	}

	private void removeRemoteFolder(String remoteFolder) {
		List<SharedData> data = mongoOperations.findAll(SharedData.class);
		if (data.size() != 1) {
			throw new RuntimeException("SharedData must contains exactly one document");
		} else {
			SharedData item = data.get(0);
			if (item.getRemoteFolders().stream().filter(string -> string.equals(remoteFolder)).count() == 1L) {
				item.getRemoteFolders().remove(remoteFolder);
				mongoOperations.save(item);
			}
		}

	}

}
