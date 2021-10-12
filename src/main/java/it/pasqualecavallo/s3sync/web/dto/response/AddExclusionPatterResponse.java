package it.pasqualecavallo.s3sync.web.dto.response;

import java.util.List;

public class AddExclusionPatterResponse {

	private List<String> patterns;
	
	private String localFolder;
	
	private String remoteFolder;
	
	public List<String> getPatterns() {
		return patterns;
	}
	
	public void setPatterns(List<String> patterns) {
		this.patterns = patterns;
	}
	
	public String getLocalFolder() {
		return localFolder;
	}
	
	public void setLocalFolder(String localFolder) {
		this.localFolder = localFolder;
	}
	
	public void setRemoteFolder(String remoteFolder) {
		this.remoteFolder = remoteFolder;
	}
	
	public String getRemoteFolder() {
		return remoteFolder;
	}
}
