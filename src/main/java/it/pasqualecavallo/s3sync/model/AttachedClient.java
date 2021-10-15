package it.pasqualecavallo.s3sync.model;

import java.util.ArrayList;
import java.util.List;

public class AttachedClient {

	private String alias;

	private List<SyncFolder> syncFolder = new ArrayList<>();

	private ClientConfiguration clientConfiguration = new ClientConfiguration();
	
	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
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
	
	public static class SyncFolder {

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

	public static class ClientConfiguration {
		
		private boolean preventFolderRecursiveRemoval = false;
		private boolean runSynchronizationOnStartup = true;
		private boolean useTrashOverDelete = true;
		
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
		
		public boolean isUseTrashOverDelete() {
			return useTrashOverDelete;
		}
		
		public void setUseTrashOverDelete(boolean useTrashOverDelete) {
			this.useTrashOverDelete = useTrashOverDelete;
		}
	}
}
