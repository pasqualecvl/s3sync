package it.pasqualecavallo.s3sync.service;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Service;

import it.pasqualecavallo.s3sync.model.Item;
import it.pasqualecavallo.s3sync.utils.FileUtils;
import it.pasqualecavallo.s3sync.utils.GlobalPropertiesManager;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.servicemetadata.SqsServiceMetadata;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.sqs.SqsClient;

@Service
public class UploadService {

	@Autowired
	private S3Client s3Client;

	@Autowired
	private MongoOperations mongoOperations;
	
	@Autowired
	private SqsClient sqsClient;
	
	public void upload(Path path, String relativePath, String remoteFolder, Item item) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(GlobalPropertiesManager.getProperty("s3.bucket"))
                .key(remoteFolder + "/" + relativePath)
                .build();		
        PutObjectResponse response = s3Client.putObject(objectRequest, path);
        if(response.eTag() != null) {
	        if(item == null) {
	        	item = new Item();
	            item.setOriginalName(relativePath);
	            item.setOwnedByFolder(remoteFolder);
	            item.setLastUpdate(System.currentTimeMillis());
	        }
	        item.setLastUpdate(System.currentTimeMillis());
	        mongoOperations.save(item);
	        for()
        }
	}

	public void getOrUpdate(String localFullPathFolder, String remoteFullPathFolder) {
		GetObjectRequest request = GetObjectRequest
				.builder()
				.bucket(GlobalPropertiesManager.getProperty("s3.bucket"))
				.key(remoteFullPathFolder)
				.build();
		ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);

		try {
			FileUtils.createFileTree(remoteFullPathFolder, response.readAllBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
}