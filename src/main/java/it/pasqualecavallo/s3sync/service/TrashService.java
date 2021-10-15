package it.pasqualecavallo.s3sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.pasqualecavallo.s3sync.utils.GlobalPropertiesManager;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;
import it.pasqualecavallo.s3sync.web.controller.NavigateTrashResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

@Service
public class TrashService {
	
	@Autowired
	private S3Client s3Client;

	public SearchTrashResponse search(String relativePath) {
		ListObjectsRequest request = ListObjectsV2Request
				.builder()
				.bucket(GlobalPropertiesManager.getProperty("s3.bucket"))
				.
		s3Client.listObjects(request)
	}

}
