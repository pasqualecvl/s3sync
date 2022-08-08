package it.s3sync.sync;

public class AmqpEventData extends EventData {
	private String source;
	private String remoteFolder;
	private String file;
	private S3Action s3Action;
	private Long time = System.currentTimeMillis();

	public AmqpEventData() {
		super(EventSource.AMQP);
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

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public String getSource() {
		return source;
	}
	
	public void setSource(String source) {
		this.source = source;
	}
}
