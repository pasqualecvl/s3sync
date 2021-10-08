package it.pasqualecavallo.s3sync.listener;

import java.io.Serializable;

public class SynchronizationMessageDto implements Serializable {

	private static final long serialVersionUID = 204252542632494654L;

	public String remoteFolder;
	public String file;
	public S3Action s3Action;
	public Long time = System.currentTimeMillis();

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

	public enum S3Action {
		CREATE, MODIFY, DELETE;
	}

	@Override
	public String toString() {
		return "SynchronizationMessageDto [remoteFolder=" + remoteFolder + ", file=" + file + ", s3Action=" + s3Action
				+ ", time=" + time + "]";
	}

}
