package it.pasqualecavallo.s3sync.discovery;

import org.springframework.stereotype.Component;

@Component
public class HeartbeatDto {

	public String clientAlias;
	public Long timestamp = System.currentTimeMillis();

	public void setClientAlias(String clientAlias) {
		this.clientAlias = clientAlias;
	}

	public String getClientAlias() {
		return clientAlias;
	}

	public Long getTimestamp() {
		return timestamp;
	}
}
