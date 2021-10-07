package it.pasqualecavallo.s3sync.listener;

public class SynchronizationMessageDto {
	public String dest;
	public String remoteFolder;
	public String file;
	public S3Action s3Action;

	public String getDest() {
		return dest;
	}

	public void setDest(String dest) {
		this.dest = dest;
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
	
	
	public enum S3Action {
		CREATE, MODIFY, DELETE;
	}


	@Override
	public String toString() {
		return "SynchronizationMessageDto [dest=" + dest + ", remoteFolder=" + remoteFolder + ", file=" + file
				+ ", s3Action=" + s3Action + "]";
	}
	
	
}
