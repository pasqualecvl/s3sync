package it.pasqualecavallo.s3sync.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import it.pasqualecavallo.s3sync.discovery.DiscoveryService;
import it.pasqualecavallo.s3sync.listener.SynchronizationMessageDto;
import it.pasqualecavallo.s3sync.listener.SynchronizationMessageDto.S3Action;
import it.pasqualecavallo.s3sync.model.Item;
import it.pasqualecavallo.s3sync.utils.FileUtils;
import it.pasqualecavallo.s3sync.utils.GlobalPropertiesManager;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

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
	private SqsClient sqsClient;

	@Autowired
	@Qualifier("sqsJsonMapper")
	private JsonMapper jsonMapper;

	public void upload(Path path, String relativePath, String remoteFolder, Item item) {
		System.out.println("Upload " + path.toString() + " to s3 folder: " + remoteFolder + " with relative path " + relativePath);
		PutObjectRequest objectRequest = PutObjectRequest.builder()
				.bucket(GlobalPropertiesManager.getProperty("s3.bucket")).key(remoteFolder + "/" + relativePath)
				.build();
		PutObjectResponse response = s3Client.putObject(objectRequest, path);
		if (response.eTag() != null) {
			AtomicBoolean isCreate = new AtomicBoolean(false);
			if (item == null) {
				System.out.println("Item not found, mark as creation");
				isCreate.set(true);
				item = new Item();
				item.setOriginalName(relativePath);
				item.setOwnedByFolder(remoteFolder);
			}
			item.setDeleted(false);
			item.setLastUpdate(System.currentTimeMillis());
			item.setUploadedBy(UserSpecificPropertiesManager.getProperty("client.alias"));
			mongoOperations.save(item);

			List<String> connectedClients = DiscoveryService.getConnectedClientAlias();
			connectedClients.forEach(alias -> {
				SynchronizationMessageDto dto = new SynchronizationMessageDto();
				dto.setDest(alias);
				dto.setFile(relativePath);
				dto.setRemoteFolder(remoteFolder);
				dto.setS3Action(isCreate.get() ? S3Action.CREATE : S3Action.MODIFY);
				SendMessageRequest request;
				System.out.println("Sending notification to " + alias + " with payload: " + dto.toString());
				try {
					request = SendMessageRequest.builder().messageBody(jsonMapper.writeValueAsString(dto))
							.messageDeduplicationId(UUID.randomUUID().toString()).messageGroupId("s3sync").build();
					sqsClient.sendMessage(request);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
			});
		}
	}

	public void upload(Path path, String remoteFolder, String relativePath) {
		Item item = mongoOperations.findOne(
				new Query(Criteria.where("ownedByFolder").is(remoteFolder).and("originalName").is(relativePath)),
				Item.class);
		System.out.println("Uploading item: " + item.toString());
		upload(path, remoteFolder, relativePath, item);

	}

	public void getOrUpdate(String localFullPathFolder, String remoteFullPathFolder) {
		System.out.println("Fetch or update file: " + localFullPathFolder + " from remote folder " + remoteFullPathFolder);
		GetObjectRequest request = GetObjectRequest.builder().bucket(GlobalPropertiesManager.getProperty("s3.bucket"))
				.key(remoteFullPathFolder).build();
		ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
		try {
			FileUtils.createFileTree(remoteFullPathFolder, response.readAllBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void delete(Path path, String remoteFolder, String relativePath) {
		System.out.println("Deleting object " + relativePath + "from s3 folder " + remoteFolder);
		DeleteObjectRequest s3request = DeleteObjectRequest
				.builder()
				.bucket(GlobalPropertiesManager.getProperty("s3.bucket"))
				.key(remoteFolder + "/" + relativePath)
				.build();
		DeleteObjectResponse response = s3Client.deleteObject(s3request);
		System.out.println("Mark item as deleted");
		Item item = mongoOperations.findOne(
				new Query(Criteria.where("ownedByFolder").is(remoteFolder).and("originalName").is(relativePath)),
				Item.class);
		item.setDeleted(true);
		item.setLastUpdate(System.currentTimeMillis());
		mongoOperations.save(item);
		
		SynchronizationMessageDto dto = new SynchronizationMessageDto();
		dto.setDest(UserSpecificPropertiesManager.getProperty("client.alias"));
		dto.setFile(relativePath);
		dto.setRemoteFolder(remoteFolder);
		dto.setS3Action(S3Action.DELETE);
		SendMessageRequest request;
		try {
			request = SendMessageRequest.builder().messageBody(jsonMapper.writeValueAsString(dto))
					.messageDeduplicationId(UUID.randomUUID().toString()).messageGroupId("s3sync").build();
			sqsClient.sendMessage(request);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

}
