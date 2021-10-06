package it.pasqualecavallo.s3sync.discovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import it.pasqualecavallo.s3sync.utils.GlobalPropertiesManager;
import it.pasqualecavallo.s3sync.utils.UserSpecificPropertiesManager;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class DiscoveryService {

	@Autowired
	private SqsClient sqsClient;

	@Autowired
	@Qualifier("sqsJsonMapper")
	private JsonMapper jsonMapper;

	private static volatile Map<String, Long> connectedClients = new HashMap<>();

	private static final long sendHeartbitAnyMillis = 180000L;
	private static final long consumeHeartbitQueueAnyMillis = 180000L;
	//must be at least 2 times consumeHeartbitQueueAnyMillis
	private static final long cleanConnectedClientAnyMillis = 2*consumeHeartbitQueueAnyMillis;

	/**
	 * Use short-living queue to notify the client is still alive.
	 * Since fixed delay is 180 seconds, the queue could expire in 181 seconds or more.
	 * @throws JsonProcessingException 
	 */
	@Scheduled(initialDelay =  0, fixedDelay = sendHeartbitAnyMillis)
	public void sendHeartbeat() throws JsonProcessingException {
		HeartbeatDto dto = new HeartbeatDto();
		dto.setClientAlias(UserSpecificPropertiesManager.getProperty("client.alias"));
		SendMessageRequest request = SendMessageRequest
				.builder()
				.messageBody(jsonMapper.writeValueAsString(dto))
				.queueUrl(GlobalPropertiesManager.getProperty("sqs.heartbeat.url"))
				.messageGroupId("s3sync")
				//random UUID prevents deduplication
				.messageDeduplicationId(UUID.randomUUID().toString())
				.build();
		sqsClient.sendMessage(request);
	}

	@Scheduled(initialDelay = 0, fixedDelay = consumeHeartbitQueueAnyMillis)
	public void consumeHeartbeat() throws JsonMappingException, JsonProcessingException {
		List<Message> heartbeats = new ArrayList<>();
		boolean stop = false;
		do{
			ReceiveMessageRequest request = ReceiveMessageRequest
					.builder()
					.queueUrl(GlobalPropertiesManager.getProperty("sqs.heartbeat.url"))
					.maxNumberOfMessages(10)
					.build();
			ReceiveMessageResponse response = sqsClient.receiveMessage(request);
			List<Message> messagesPart = response.messages();
			if(messagesPart.size() < 10) {
				stop = true;
			}
			heartbeats.addAll(messagesPart);
		} while(!stop);
		synchronized (connectedClients) {
			for(Message m : heartbeats) {
				HeartbeatDto dto = jsonMapper.readValue(m.body(), HeartbeatDto.class);
				if(!dto.getClientAlias().equals(UserSpecificPropertiesManager.getProperty("client.alias"))) {
					connectedClients.put(dto.getClientAlias(), dto.getTimestamp());					
				}
			}
		}
	}

	@Scheduled(initialDelay = cleanConnectedClientAnyMillis, fixedDelay = cleanConnectedClientAnyMillis)
	public void clean() {
		synchronized (connectedClients) {
			for (Entry<String, Long> connectedClient : connectedClients.entrySet()) {
				if (connectedClient.getValue() < System.currentTimeMillis() - cleanConnectedClientAnyMillis) {
					connectedClients.remove(connectedClient.getKey());
				}
			}
		}
	}

	public static List<String> getConnectedClientAlias() {
		List<String> connectedAlias = new ArrayList<>();
		connectedClients.forEach((key, value) -> {
			connectedAlias.add(key);
		});
		return connectedAlias;
	}
	
}
