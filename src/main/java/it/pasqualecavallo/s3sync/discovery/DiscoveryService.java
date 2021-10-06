package it.pasqualecavallo.s3sync.discovery;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import com.sun.tools.classfile.ConstantPool.CPInfo;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class DiscoveryService {

	@Autowired
	private SqsClient sqsClient;

	private volatile Map<String, Long> connectedClients = new HashMap<>();

	private static final long sendHeartbitAnyMillis = 180000L;
	private static final long consumeHeartbitQueueAnyMillis = 180000L;
	private static final long cleanConnectedClientAnyMillis = 
	/**
	 * Use short-living queue to notify the client is still alive.
	 * Since fixed delay is 180 seconds, the queue could expire in 181 seconds or more.
	 */
	@Scheduled(initialDelay =  0, fixedDelay = sendHeartbitAnyMillis)
	public void sendHeartbit() {
			SendMessageRequest request = SendMessageRequest.builder().messageBody().;
			request.
			sqsClient.sendMessage(null)			
		
	}

	@Scheduled(initialDelay = 0, fixedDelay = consumeHeartbitQueueAnyMillis)
	public void consumeHeartbit() {
		synchronized (connectedClients) {
			
		}
	}

	@Scheduled(initialDelay = consumeHeartbitQueueAnyMillis/2, fixedDelay = consumeHeartbitQueueAnyMillis)
	public void clean() {
		
		sqsClient.receiveMessage(null)
		synchronized (connectedClients) {
			for(Entry<String, Long> connectedClient : connectedClients) {
				if(connectedClient.getValue() < System.) {
					
				}
			}
		}
	}

}
