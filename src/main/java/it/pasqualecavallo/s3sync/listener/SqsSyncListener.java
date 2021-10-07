package it.pasqualecavallo.s3sync.listener;

import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import it.pasqualecavallo.s3sync.listener.SynchronizationMessageDto.S3Action;
import it.pasqualecavallo.s3sync.service.SynchronizationService;
import it.pasqualecavallo.s3sync.service.UploadService;
import it.pasqualecavallo.s3sync.utils.FileUtils;
import it.pasqualecavallo.s3sync.utils.GlobalPropertiesManager;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Service
public class SqsSyncListener {
	
	@Autowired
	private SqsClient sqsClient;
	
	@Autowired
	private SynchronizationService synchronizationService;
	
	@Autowired
	@Qualifier("sqsJsonMapper")
	private JsonMapper jsonMapper;
	
	@Autowired
	private UploadService uploadService;
	
	private static final long pollQueueAnyMillis = 60000L;
	
	
	@Scheduled(initialDelay = 0, fixedDelay = pollQueueAnyMillis)
	public void pollQueue() {
		try {
			WatchListeners.lockSemaphore();
			boolean stop = false;
			do{
				ReceiveMessageRequest request = ReceiveMessageRequest
						.builder()
						.queueUrl(GlobalPropertiesManager.getProperty("sqs.notification.url"))
						.maxNumberOfMessages(10)
						.build();
				ReceiveMessageResponse response = sqsClient.receiveMessage(request);
				List<Message> messagesPart = response.messages();
				if(messagesPart.size() < 10) {
					stop = true;
				}
				for(Message m : messagesPart) {
					try {
						SynchronizationMessageDto dto = jsonMapper.readValue(m.body(), SynchronizationMessageDto.class);
						if(dto.getDest().equals(UserSpecificPropertiesManager.getProperty("client.alias"))) {
							DeleteMessageRequest deleteRequest = DeleteMessageRequest
									.builder()
									.queueUrl(GlobalPropertiesManager.getProperty("sqs.notification.url"))
									.receiptHandle(m.receiptHandle())
									.build();
							sqsClient.deleteMessage(deleteRequest);
							String localFolder = synchronizationService.getSynchronizedLocalRootFolderByRemoteFolder(dto.getRemoteFolder());
							if(S3Action.CREATE.name().equals(dto.getS3Action()) 
									|| S3Action.MODIFY.name().equals(dto.getS3Action())) {
								uploadService.getOrUpdate(localFolder + dto.getFile(), dto.getRemoteFolder());
							} else if(S3Action.DELETE.name().equals(dto.getS3Action())) {
								if(Path.of(localFolder + dto.getFile()).toFile().lastModified() <= dto.getTime()) {
									FileUtils.deleteFileAndEmptyTree(localFolder + dto.getFile());
								}
							}
						}
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
				}
			} while(!stop);
		} finally {
			WatchListeners.releaseSemaphore();
		}
	}

}
