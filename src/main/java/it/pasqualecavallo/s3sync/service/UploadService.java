package it.pasqualecavallo.s3sync.service;

import java.io.File;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.S3Client;

@Service
public class UploadService {

	@Autowired
	private S3Client s3Client;

	@Autowired
	private MongoOperations mongoOperations;
	
	
	public void s3Upload(File file, String relativePath, String remoteFolder) {
		
	}

	public void getOrUpdate(Path itemLocalFullPath, String remoteFolder, String s3Name) {
		// TODO Auto-generated method stub
		
	}
	
}
