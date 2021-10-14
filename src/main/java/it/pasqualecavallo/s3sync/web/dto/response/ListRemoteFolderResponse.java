package it.pasqualecavallo.s3sync.web.dto.response;

import java.util.List;

public class ListRemoteFolderResponse extends RestBaseResponse {

	private List<String> remoteFolder;
	
	public List<String> getRemoteFolder() {
		return remoteFolder;
	}
	
	public void setRemoteFolder(List<String> remoteFolder) {
		this.remoteFolder = remoteFolder;
	}
}
