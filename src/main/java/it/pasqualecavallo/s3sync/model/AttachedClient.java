package it.pasqualecavallo.s3sync.model;

import java.math.BigInteger;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class AttachedClient {

	@Id
	private BigInteger _id;

	private String alias;

	private List<SyncFolder> syncFolder;

	public String getAlias() {
		return alias;
	}


	public BigInteger get_id() {
		return _id;
	}
	
	public void setAlias(String alias) {
		this.alias = alias;
	}

	public void set_id(BigInteger _id) {
		this._id = _id;
	}
	
	public List<SyncFolder> getSyncFolder() {
		return syncFolder;
	}

	public void setSyncFolder(List<SyncFolder> syncFolder) {
		this.syncFolder = syncFolder;
	}

	public class SyncFolder {

		private String localPath;
		private String remotePath;

		public String getLocalPath() {
			return localPath;
		}

		public void setLocalPath(String localPath) {
			this.localPath = localPath;
		}

		public String getRemotePath() {
			return remotePath;
		}

		public void setRemotePath(String remotePath) {
			this.remotePath = remotePath;
		}

	}

}
