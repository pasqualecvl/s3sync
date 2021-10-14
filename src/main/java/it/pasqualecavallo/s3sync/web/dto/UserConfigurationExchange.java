package it.pasqualecavallo.s3sync.web.dto;

public class UserConfigurationExchange {

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
