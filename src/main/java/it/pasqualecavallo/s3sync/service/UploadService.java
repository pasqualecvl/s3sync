package it.pasqualecavallo.s3sync.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

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

	public void upload(Path path, String relativePath, String remoteFolder, Item item) {
		System.out.println(
				"Upload " + path.toString() + " to s3 folder: " + remoteFolder + " with relative path " + relativePath);
		try{
			PutObjectRequest objectRequest = PutObjectRequest.builder()
				.bucket(GlobalPropertiesManager.getProperty("s3.bucket")).key(remoteFolder + relativePath)
				.build();
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
				item.setUploadedBy(UserSpecificPropertiesManager.getProperty("client.alias"));
				mongoOperations.save(item);
				SynchronizationMessageDto dto = new SynchronizationMessageDto();
				dto.setFile(relativePath);
				dto.setRemoteFolder(remoteFolder);
				dto.setS3Action(isCreate ? S3Action.CREATE : S3Action.MODIFY);
				amqpTemplate.convertAndSend(dto);
			}
		} catch(UncheckedIOException e) {
			//File removed 
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

	public void delete(Path path, String remoteFolder, String relativePath) {
		System.out.println("Deleting object " + relativePath + "from s3 folder " + remoteFolder);
		DeleteObjectRequest s3request = DeleteObjectRequest.builder()
				.bucket(GlobalPropertiesManager.getProperty("s3.bucket")).key(remoteFolder + relativePath)
				.build();
		DeleteObjectResponse response = s3Client.deleteObject(s3request);
		System.out.println("Mark item as deleted");
		Item item = mongoOperations.findOne(
				new Query(Criteria.where("ownedByFolder").is(remoteFolder).and("originalName").is(relativePath)),
				Item.class);
		if(item != null) {
			//Item never sync
			item.setDeleted(true);
			item.setLastUpdate(System.currentTimeMillis());
			mongoOperations.save(item);			
		}

		SynchronizationMessageDto dto = new SynchronizationMessageDto();
		dto.setFile(relativePath);
		dto.setRemoteFolder(remoteFolder);
		dto.setS3Action(S3Action.DELETE);
		amqpTemplate.convertAndSend(dto);
	}

}
