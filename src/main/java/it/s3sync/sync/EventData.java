package it.s3sync.sync;

public class EventData {
	
	private EventSource eventSource;
	private String remoteFolder;

	public EventData(EventSource eventSource) {
		this.eventSource = eventSource;
	}
	
	public EventSource getEventSource() {
		return eventSource;
	}
	
	public String getRemoteFolder() {
		return remoteFolder;
	}
	
	public void setRemoteFolder(String remoteFolder) {
		this.remoteFolder = remoteFolder;
	}

	
	public enum EventSource {
		AMQP, FILESYSTEM;
	}
	
	public enum S3Action {
		CREATE, MODIFY, DELETE;
	}

}
