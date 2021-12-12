package it.s3sync.sync;

import java.nio.file.WatchEvent;
import java.nio.file.Watchable;

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
	
	public static class AmqpEventData extends EventData {
		private String source;
		private String remoteFolder;
		private String file;
		private S3Action s3Action;
		private Long time = System.currentTimeMillis();

		public AmqpEventData() {
			super(EventSource.AMQP);
		}
		
		public String getRemoteFolder() {
			return remoteFolder;
		}

		public void setRemoteFolder(String remoteFolder) {
			this.remoteFolder = remoteFolder;
		}

		public void setFile(String file) {
			this.file = file;
		}

		public String getFile() {
			return file;
		}

		public void setS3Action(S3Action s3Action) {
			this.s3Action = s3Action;
		}

		public S3Action getS3Action() {
			return s3Action;
		}

		public Long getTime() {
			return time;
		}

		public void setTime(Long time) {
			this.time = time;
		}

		public String getSource() {
			return source;
		}
		
		public void setSource(String source) {
			this.source = source;
		}
	}

	public static class FileSystemEventData extends EventData {
		
		private Watchable watchable;
		private WatchEvent<?> watchEvent;
		private String localRootFolder;
		
		public FileSystemEventData() {
			super(EventSource.FILESYSTEM);
		}
		
		public void setWatchable(Watchable watchable) {
			this.watchable = watchable;
		}
		
		public void setWatchEvent(WatchEvent<?> watchEvent) {
			this.watchEvent = watchEvent;
		}
		
		public Watchable getWatchable() {
			return watchable;
		}
		
		public WatchEvent<?> getWatchEvent() {
			return watchEvent;
		}

		public String getLocalRootFolder() {
			return localRootFolder;
		}
		
		public void setLocalRootFolder(String localRootFolder) {
			this.localRootFolder = localRootFolder;
		}

	}
}
