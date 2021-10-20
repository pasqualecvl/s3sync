package it.s3sync.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.json.JsonMapper;

import it.s3sync.listener.SynchronizationMessageDto;
import it.s3sync.listener.SynchronizationMessageDto.S3Action;
import it.s3sync.model.AttachedClient;
import it.s3sync.model.Item;
import it.s3sync.model.SharedData;
import it.s3sync.utils.FileUtils;
import it.s3sync.utils.GlobalPropertiesManager;
import it.s3sync.utils.UserSpecificPropertiesManager;
import it.s3sync.web.controller.advice.exception.InternalServerErrorException;
import it.s3sync.web.dto.response.RestBaseResponse.ErrorMessage;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
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
	
	private static final Logger logger = LoggerFactory.getLogger(UploadService.class);

	public void upload(Path path, String relativePath, String remoteFolder, Item item, Long lastModified) {
		logger.debug("Uploading {} to s3 folder {} with relative path {}", path, remoteFolder, relativePath);
		try {
			PutObjectRequest objectRequest = PutObjectRequest.builder()
					.bucket(GlobalPropertiesManager.getProperty("s3.bucket")).key(remoteFolder + relativePath).build();
			PutObjectResponse response = s3Client.putObject(objectRequest, path);
			if (response.sdkHttpResponse().isSuccessful()) {
				logger.debug("[[DEBUG]] Uploading {} to {} successfully", relativePath, remoteFolder);
				boolean isCreate = false;
				if (item == null) {
					logger.debug("Item never synchronized, create new Item with originalName {} and owning folder {} in DB", relativePath, remoteFolder);
					isCreate = true;
					item = new Item();
					item.setOriginalName(relativePath);
					item.setOwnedByFolder(remoteFolder);
				}
				item.setDeleted(false);
				item.setLastUpdate(lastModified);
				item.setUploadedBy(UserSpecificPropertiesManager.getConfiguration().getAlias());
				logger.trace("[[TRACE]] Writing item on mongo: {}", item);
				mongoOperations.save(item);
				SynchronizationMessageDto dto = new SynchronizationMessageDto();
				dto.setFile(relativePath);
				dto.setRemoteFolder(remoteFolder);
				dto.setSource(UserSpecificPropertiesManager.getConfiguration().getAlias());
				dto.setS3Action(isCreate ? S3Action.CREATE : S3Action.MODIFY);
				dto.setTime(lastModified);
				logger.debug("[[DEBUG]] Sending fanout notification through MQ");
				logger.trace("[[TRACE]] MQ Dto content: {}", dto);
				amqpTemplate.convertAndSend(dto);
			}
		} catch (UncheckedIOException e) {
			logger.error("[[ERROR]] Exception removing file {}", relativePath, e);
		}
	}

	public void upload(Path path, String remoteFolder, String relativePath, Long lastModified) {
		Item item = mongoOperations.findOne(
				new Query(Criteria.where("ownedByFolder").is(remoteFolder).and("originalName").is(relativePath)),
				Item.class);
		upload(path, relativePath, remoteFolder, item, lastModified);

	}

	public List<String> getOrUpdate(String localFullPathFolder, String remoteFullPathFolder, long lastModified) {
		logger.info("[[INFO]] Fetch or update file {} from remote folder {}", localFullPathFolder, remoteFullPathFolder);
		GetObjectRequest request = GetObjectRequest.builder().bucket(GlobalPropertiesManager.getProperty("s3.bucket"))
				.key(remoteFullPathFolder).build();
		ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
		try {
			logger.debug("[[DEBUG]] Creating folder tree for file {}", localFullPathFolder);
			return FileUtils.createFileTree(localFullPathFolder, response.readAllBytes(), lastModified);
		} catch (IOException e) {
			logger.error("Exception", e);
			throw new RuntimeException(e);
		}
	}

	public boolean deleteTrashed(String relativePath) {
		logger.info("[[INFO]] Deleting file {} from Trash", relativePath);
		String s3Bucket = GlobalPropertiesManager.getProperty("s3.bucket");
		String fileKey = "Trash/" + relativePath;
		DeleteObjectRequest s3request = DeleteObjectRequest.builder().bucket(s3Bucket).key(fileKey).build();
		DeleteObjectResponse response = s3Client.deleteObject(s3request);
		if(response.sdkHttpResponse().isSuccessful()) {
			logger.info("[[INFO]] Successfully delete {}", fileKey);
			return true;
		} else {
			logger.error("[[ERROR]] Error deleting file {} with error {}", fileKey, response.sdkHttpResponse().statusText());
			return false;
		}
	}

	public boolean delete(String remoteFolder, String relativePath) {
		logger.info("[[INFO]] Deleting object {} from s3 folder {}", relativePath, remoteFolder);
		String s3Bucket = GlobalPropertiesManager.getProperty("s3.bucket");
		String fileKey = remoteFolder + relativePath;
		if (UserSpecificPropertiesManager.getConfiguration().getClientConfiguration().isUseTrashOverDelete()) {
			CopyObjectRequest request = CopyObjectRequest.builder().sourceBucket(s3Bucket).sourceKey(fileKey)
					.destinationBucket(s3Bucket).destinationKey("Trash/" + fileKey).build();
			logger.debug("[[DEBUG]] Safe delete (useTrashOverDelete) is enabled. Copy the object {} in the reserved key Trash/", fileKey);
			CopyObjectResponse response = s3Client.copyObject(request);
			if(!response.sdkHttpResponse().isSuccessful()) {
				logger.error("[[ERROR]] Error copying object {}. Delete operation will be suppressed.", fileKey);
				throw new InternalServerErrorException(ErrorMessage.E500_SYNC_ERROR, fileKey);
			}
		}
		DeleteObjectRequest s3request = DeleteObjectRequest.builder().bucket(s3Bucket).key(fileKey).build();
		DeleteObjectResponse response = s3Client.deleteObject(s3request);
		if(response.sdkHttpResponse().isSuccessful()) {
			logger.debug("[[DEBUG]] Delete from S3 successfull, mark item {} as deleted in DB", fileKey);
			Item item = mongoOperations.findOne(
					new Query(Criteria.where("ownedByFolder").is(remoteFolder).and("originalName").is(relativePath)),
					Item.class);
			if (item != null) {
				// Item never sync
				logger.warn("[[WARN]] Deleted item {} not found in DB. This is a managed condition, but still an error", fileKey);
				item.setDeleted(true);
				item.setLastUpdate(System.currentTimeMillis());
				mongoOperations.save(item);
			}
	
			SynchronizationMessageDto dto = new SynchronizationMessageDto();
			dto.setFile(relativePath);
			dto.setRemoteFolder(remoteFolder);
			dto.setS3Action(S3Action.DELETE);
			dto.setSource(UserSpecificPropertiesManager.getConfiguration().getAlias());
			logger.debug("[[DEBUG]] Sending fanout notification for delete {}", fileKey);
			logger.trace("[[TRACE]] Sendi MQ message {}", dto);
			amqpTemplate.convertAndSend(dto);
			logger.info("[[INFO]] Successfully delete file {}", fileKey);
			return true;
		} else {
			return false;
		}
	}

	public void deleteAsFolder(String remoteFolder, String relativeLocation) {
		logger.debug("[[DEBUG]] Deleting localFolder {} from remote {}", relativeLocation, remoteFolder);
		AttachedClient currentUser = UserSpecificPropertiesManager.getConfiguration();

		// if recursive removal allowed, delete any items in folder locally and remote
		if (currentUser.getClientConfiguration().isPreventFolderRecursiveRemoval()) {
			logger.debug("[[DEBUG]] Client set recursive folder removal to disable."
					+ "Add exclusion pattern for the folder, but doesn't remove it from remote."); 
			if (relativeLocation.isBlank()) {
				//FIXME: This can't happen because top level folder has no listeners
				// deleting /folder/to/sync which has a listener should throw an event on /folder/to
				// which has no listener
				logger.warn("[[WARN]] Found event on localRootFolder. This is managed, but read and edit the FIXME please");
				// event on localRootFolder
				synchronizationService.removeSynchronizationFolder(
						synchronizationService.getSynchronizedLocalRootFolderByRemoteFolder(remoteFolder));
				removeRemoteFolder(remoteFolder);
			} else {
				logger.debug("[[DEBUG]] Add exclusion pattern {} to remote folder {}", "^" + relativeLocation, remoteFolder);
				synchronizationService.addSynchronizationExclusionPattern(
						synchronizationService.getSynchronizedLocalRootFolderByRemoteFolder(remoteFolder),
						"^" + relativeLocation);
			}
		} else {
			if (relativeLocation.isBlank()) {
				// localRootFolder -> pick all items in folder
				//FIXME: This can't happen because top level folder has no listeners
				// deleting /folder/to/sync which has a listener should throw an event on /folder/to
				// which has no listener
				logger.warn("[[WARN]] Found event on localRootFolder. This is managed, but read and edit the FIXME please");
				List<Item> toDelete = mongoOperations.find(new Query(Criteria.where("ownedByFolder").is(remoteFolder)),
						Item.class);
				toDelete.forEach(item -> {
					if(!delete(item.getOwnedByFolder(), item.getOriginalName())) {
						logger.error("Error deleting S3 file");
					}
				});
			} else {
				// subfolder -> filter filename by regexp
				List<Item> toDelete = mongoOperations.find(new Query(Criteria.where("ownedByFolder").is(remoteFolder)
						.and("originalName").regex("/^" + relativeLocation + "/")), Item.class);
				logger.debug("[[DEBUG]] Found {}# files to delete filtering by regexp {} on remote folder {}", 
						toDelete.size(), "/^" + relativeLocation + "/", remoteFolder);
				toDelete.forEach(item -> {
					if(!delete(item.getOwnedByFolder(), item.getOriginalName())) {
						logger.error("[[ERROR]] Error deleting S3 file {}", item.getOriginalName());
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
