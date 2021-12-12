package it.s3sync.sync;

import java.nio.file.WatchEvent;
import java.nio.file.Watchable;

public class FileSystemEventData extends EventData {
	
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
