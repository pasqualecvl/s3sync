package it.pasqualecavallo.s3sync.controller.dto;

import javax.validation.constraints.NotBlank;

public class AddFolderRequest {

	@NotBlank
	private String localFolder;

	@NotBlank
	private String remoteFolder;

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

}
