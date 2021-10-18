package it.pasqualecavallo.s3sync.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.pasqualecavallo.s3sync.utils.GlobalPropertiesManager;
import it.pasqualecavallo.s3sync.web.dto.response.DeleteTrashItemResponse;
import it.pasqualecavallo.s3sync.web.dto.response.Folder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class TrashService {

	@Autowired
	private S3Client s3Client;

	@Autowired
	private UploadService uploadService;

	private static final Logger logger = LoggerFactory.getLogger(TrashService.class);
	
	public Folder navigate(String relativePath) {
		logger.info("[[INFO]] List elements with root key {}", relativePath);
		String s3bucket = GlobalPropertiesManager.getProperty("s3.bucket");
		ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(s3bucket).prefix(relativePath).build();
		List<String> keys = new ArrayList<>();
		ListObjectsV2Response list = null;
		do {
			list = s3Client.listObjectsV2(request);
			for (S3Object key : list.contents()) {
				keys.add(key.key());
			}
			request = ListObjectsV2Request.builder().bucket(s3bucket).prefix(relativePath)
					.continuationToken(list.nextContinuationToken()).build();
		} while (list.isTruncated());
		logger.debug("For root key {} return with {}# elements", relativePath, keys.size());
		return folderize(keys);
	}

	public DeleteTrashItemResponse deleteAll(List<String> keys) {
		logger.debug("[[DEBUG]] Deleting keys {}", String.join(",", keys));
		List<String>deleted = new ArrayList<>();
		for(String key : keys) {
			if(uploadService.deleteTrashed(key)) {
				deleted.add(key);
			}
		}
		DeleteTrashItemResponse response = new DeleteTrashItemResponse();
		response.setDeleted(deleted);
		return response;
	}

	private Folder folderize(List<String> keys) {
		Folder response = new Folder();
		for (String key : keys) {
			logger.trace("[[TRACE]] Tokenizing key {} to construct folders tree", keys);
			String[] tokenized = key.split("/");
			Folder f = response.addNodes(tokenized);
			f.addFile(tokenized[tokenized.length - 1]);
		}
		return response;
	}

//	public RecoverTrashedFileResponse recoverAll(String toRemoteFolder, List<String> keys) {
//		
//	}

}
