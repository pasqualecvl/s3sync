package it.s3sync.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.json.JsonMapper;

import it.s3sync.sync.EventData.AmqpEventData;
import it.s3sync.sync.SynchronizationThreadPool;

@Service
public class AmqpSyncListener {

	@Autowired
	@Qualifier("sqsJsonMapper")
	private JsonMapper jsonMapper;

	public void receiveSyncMessage(AmqpEventData dto) {
		SynchronizationThreadPool.enqueue(dto);
	}

}
