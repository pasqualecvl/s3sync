package it.pasqualecavallo.s3sync.web.dto.request;

import javax.validation.constraints.NotEmpty;

public class RemoveFolderRequest {

	@NotEmpty
	private String remoteFolder;
	
	public String getRemoteFolder() {
		return remoteFolder;
	}
	
	public void setRemoteFolder(String remoteFolder) {
		this.remoteFolder = remoteFolder;
	}
	
}
