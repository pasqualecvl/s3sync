package it.pasqualecavallo.s3sync.web.dto.request;

import java.util.List;

public class RecoverTrashedFileRequest {

	public List<String> keys;
	public String toRemoteFolder;
	
	public List<String> getKeys() {
		return keys;
	}
	
	public void setKeys(List<String> keys) {
		this.keys = keys;
	}
	
	public String getToRemoteFolder() {
		return toRemoteFolder;
	}
	
	public void setToRemoteFolder(String toRemoteFolder) {
		this.toRemoteFolder = toRemoteFolder;
	}
}
