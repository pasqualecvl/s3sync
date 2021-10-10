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

	private ClientConfiguration clientConfiguration = new ClientConfiguration();
	
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

	public ClientConfiguration getClientConfiguration() {
		return clientConfiguration;
	}
	
	public void setClientConfiguration(ClientConfiguration clientConfiguration) {
		this.clientConfiguration = clientConfiguration;
	}
	
	public class SyncFolder {

		private String localPath;
		private String remotePath;
		private List<String> exclusionPattern;

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

		public List<String> getExclusionPattern() {
			return exclusionPattern;
		}

		public void setExclusionPattern(List<String> exclusionPattern) {
			this.exclusionPattern = exclusionPattern;
		}
	}

	public class ClientConfiguration {
		
		private boolean preventFolderRecursiveRemoval = false;
		private boolean runSynchronizationOnStartup = true;
		
		
		public boolean isPreventFolderRecursiveRemoval() {
			return preventFolderRecursiveRemoval;
		}

		public void setPreventFolderRecursiveRemoval(boolean preventFolderRecursiveRemoval) {
			this.preventFolderRecursiveRemoval = preventFolderRecursiveRemoval;
		}
		
		public boolean isRunSynchronizationOnStartup() {
			return runSynchronizationOnStartup;
		}
		
		public void setRunSynchronizationOnStartup(boolean runSynchronizationOnStartup) {
			this.runSynchronizationOnStartup = runSynchronizationOnStartup;
		}
	}
}
