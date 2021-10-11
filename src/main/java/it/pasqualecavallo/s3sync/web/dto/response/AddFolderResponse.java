package it.pasqualecavallo.s3sync.web.dto.response;

public class AddFolderResponse extends RestBaseResponse {

	private String localFolder;
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
