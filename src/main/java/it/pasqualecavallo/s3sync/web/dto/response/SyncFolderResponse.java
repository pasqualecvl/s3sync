package it.pasqualecavallo.s3sync.web.dto.response;

import java.util.List;

public class SyncFolderResponse {

	private String localFolder;

	private String remoteFolder;

	private List<String> exclusionPatterns;

	public String getLocalFolder() {
		return localFolder;
	}

	public void setLocalFolder(String localFolder) {
		this.localFolder = localFolder;
	}

	public String getRemoteFolder() {
		return remoteFolder;
	}

	public void setRemoteFolder(String remoteFolder) {
		this.remoteFolder = remoteFolder;
	}

	public List<String> getExclusionPatterns() {
		return exclusionPatterns;
	}

	public void setExclusionPatterns(List<String> exclusionPatterns) {
		this.exclusionPatterns = exclusionPatterns;
	}

	@Override
	public String toString() {
		return "SyncFolderResponse [localFolder=" + localFolder + ", remoteFolder=" + remoteFolder
				+ ", exclusionPatterns=" + exclusionPatterns + "]";
	}

}
