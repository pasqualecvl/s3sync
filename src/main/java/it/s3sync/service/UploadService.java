package it.s3sync.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

import it.s3sync.exception.PreventUploadForFolderException;
import it.s3sync.model.Item;
import it.s3sync.model.SharedData;
import it.s3sync.sync.AmqpEventData;
import it.s3sync.sync.EventData.S3Action;
import it.s3sync.utils.FileUtils;
import it.s3sync.utils.GlobalPropertiesManager;
import it.s3sync.utils.UserSpecificPropertiesManager;
import it.s3sync.web.controller.advice.exception.InternalServerErrorException;
import it.s3sync.web.dto.response.RestBaseResponse.ErrorMessage;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Response;

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
	@Qualifier("sqsJsonMapper")
	private JsonMapper jsonMapper;

	private static final long multipatUploadStartSize = (long)(5 * 1024 * 1024);
	
	private static final Logger logger = LoggerFactory.getLogger(UploadService.class);

	public void upload(Path path, String relativePath, String remoteFolder, Item item, Long lastModified) {
		// FIXME: for a strange reason, in some cases, folder is uploaded.
		// Rework this if with a real fix
		if (path.toFile().isDirectory()) {
			throw new PreventUploadForFolderException();
		}
		if (lastModified == null || lastModified == 0) {
			throw new RuntimeException("lastModified MUST be valorised");
		}

		logger.debug("Uploading {} to s3 folder {} with relative path {}", path, remoteFolder, relativePath);
		try {
			if (item == null || item.getDeleted() ||  FileUtils.checkForDifferentChecksum(item.getChecksum(), path)) {
				S3Response response = uploadOrMultipart(remoteFolder + relativePath, path);
				if (response.sdkHttpResponse().isSuccessful()) {
					logger.debug("[[DEBUG]] Uploading {} to {} successfully", relativePath, remoteFolder);
					boolean isCreate = false;
					if (item == null) {
						logger.debug(
								"Item never synchronized, create new Item with originalName {} and owning folder {} in DB",
								relativePath, remoteFolder);
						isCreate = true;
						item = new Item();
						item.setOriginalName(relativePath);
						item.setOwnedByFolder(remoteFolder);
					}
					item.setChecksum(FileUtils.getChecksum(path));
					item.setDeleted(false);
					item.setLastUpdate(lastModified);
					item.setUploadedBy(UserSpecificPropertiesManager.getConfiguration().getAlias());
					logger.trace("[[TRACE]] Writing item on mongo: {}", item);
					mongoOperations.save(item);
					AmqpEventData dto = new AmqpEventData();
					dto.setFile(relativePath);
					dto.setRemoteFolder(remoteFolder);
					dto.setSource(UserSpecificPropertiesManager.getConfiguration().getAlias());
					dto.setS3Action(isCreate ? S3Action.CREATE : S3Action.MODIFY);
					dto.setTime(lastModified);
					logger.debug("[[DEBUG]] Sending fanout notification through MQ");
					logger.trace("[[TRACE]] MQ Dto content: {}", dto);
					amqpTemplate.convertAndSend(dto);
				}
			}
		} catch (UncheckedIOException e) {
			logger.error("[[ERROR]] Exception removing file {}", relativePath, e);
		}
	}

	private S3Response uploadOrMultipart(String s3Key, Path path) {
        long bytes;
		try {
			bytes = Files.size(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if(bytes > multipatUploadStartSize) {
    		CreateMultipartUploadRequest upload = CreateMultipartUploadRequest.builder().build();
        	
        } else {
    		PutObjectRequest objectRequest = PutObjectRequest.builder()
    				.bucket(GlobalPropertiesManager.getProperty("s3.bucket")).key(s3Key)
    				.build();
    		return s3Client.putObject(objectRequest, path);        	
        }
	}

	public void upload(Path path, String remoteFolder, String relativePath, Long lastModified) {
		Item item = mongoOperations.findOne(
				new Query(Criteria.where("ownedByFolder").is(remoteFolder).and("originalName").is(relativePath)),
				Item.class);
		upload(path, relativePath, remoteFolder, item, lastModified);

	}

	public List<String> getOrUpdate(String localFullPathFolder, Item item) {
		String remoteFullPathFolder = item.getOwnedByFolder() + item.getOriginalName();
		long lastModified = item.getLastUpdate();
		try {
			Path path = Path.of(localFullPathFolder);
			File file = path.toFile();
			if (!file.exists() || (file.exists() && file.isFile()
					&& FileUtils.checkForDifferentChecksum(item.getChecksum(), path))) {
				logger.info("[[INFO]] Fetch or update file {} from remote folder {}", localFullPathFolder,
						remoteFullPathFolder);
				GetObjectRequest request = GetObjectRequest.builder()
						.bucket(GlobalPropertiesManager.getProperty("s3.bucket")).key(remoteFullPathFolder).build();
				ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
				logger.debug("[[DEBUG]] Creating folder tree for file {}", localFullPathFolder);
				return FileUtils.createFileTree(localFullPathFolder, response.readAllBytes(), lastModified);
			} else {
				logger.info("[[INFO]] Trying update file {} with the same checksum. Discarded", item.getOriginalName());
			}
		} catch (IOException e) {
			logger.error("Exception creating file tree. File {} not synchronized", localFullPathFolder, e);
		} catch (NoSuchKeyException e) {
			logger.error("Exception reading file {} from s3 to {}", remoteFullPathFolder, localFullPathFolder, e);
		}
		return new ArrayList<>();
	}

	public boolean deleteTrashed(String relativePath) {
		logger.info("[[INFO]] Deleting file {} from Trash", relativePath);
		String s3Bucket = GlobalPropertiesManager.getProperty("s3.bucket");
		String fileKey = "Trash/" + relativePath;
		DeleteObjectRequest s3request = DeleteObjectRequest.builder().bucket(s3Bucket).key(fileKey).build();
		DeleteObjectResponse response = s3Client.deleteObject(s3request);
		if (response.sdkHttpResponse().isSuccessful()) {
			logger.info("[[INFO]] Successfully delete {}", fileKey);
			return true;
		} else {
			logger.error("[[ERROR]] Error deleting file {} with error {}", fileKey,
					response.sdkHttpResponse().statusText());
			return false;
		}
	}

	public boolean delete(String remoteFolder, String relativePath) {
		logger.info("[[INFO]] Deleting object {} from s3 folder {}", relativePath, remoteFolder);
		String s3Bucket = GlobalPropertiesManager.getProperty("s3.bucket");
		String fileKey = remoteFolder + relativePath;
		try {
			if (UserSpecificPropertiesManager.getConfiguration().getClientConfiguration().isUseTrashOverDelete()) {
				CopyObjectRequest request = CopyObjectRequest.builder().sourceBucket(s3Bucket).sourceKey(fileKey)
						.destinationBucket(s3Bucket).destinationKey("Trash/" + fileKey).build();
				logger.debug(
						"[[DEBUG]] Safe delete (useTrashOverDelete) is enabled. Copy the object {} in the reserved key Trash/",
						fileKey);
				CopyObjectResponse response = s3Client.copyObject(request);
				if (!response.sdkHttpResponse().isSuccessful()) {
					logger.error("[[ERROR]] Error copying object {}. Delete operation will be suppressed.", fileKey);
					throw new InternalServerErrorException(ErrorMessage.E500_SYNC_ERROR, fileKey);
				}
			}
			DeleteObjectRequest s3request = DeleteObjectRequest.builder().bucket(s3Bucket).key(fileKey).build();
			DeleteObjectResponse response = s3Client.deleteObject(s3request);
			if (response.sdkHttpResponse().isSuccessful()) {
				logger.debug("[[DEBUG]] Delete from S3 successfull, mark item {} as deleted in DB", fileKey);
				Item item = mongoOperations.findOne(
						new Query(
								Criteria.where("ownedByFolder").is(remoteFolder).and("originalName").is(relativePath)),
						Item.class);
				if (item != null) {
					// Item never sync
					logger.warn(
							"[[WARN]] Deleted item {} not found in DB. This is a managed condition, but still an error",
							fileKey);
					item.setDeleted(true);
					item.setLastUpdate(System.currentTimeMillis());
					mongoOperations.save(item);
				}
				AmqpEventData dto = new AmqpEventData();
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
		} catch (NoSuchKeyException e) {
			logger.error("[[MAYBE-NOT-AN-ERROR]] Key {} not found, maybe alredy deleted", fileKey);
			return false;
		}
	}
	
	public void removeRemoteFolder(String remoteFolder) {
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
