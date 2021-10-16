package it.pasqualecavallo.s3sync.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.pasqualecavallo.s3sync.service.SynchronizationService;
import it.pasqualecavallo.s3sync.service.UploadService;

public class WatchListeners {

	private static Map<String, Thread> threadPool = new HashMap<>();

	private static volatile Map<String, Collection<String>> changesWhileLocked = new HashMap<>();

	private static volatile int threadSemaphore = 0;

	private static final Logger logger = LoggerFactory.getLogger(WatchListeners.class);

	public static void startThread(UploadService uploadService, SynchronizationService synchronizationService,
			String remoteFolder, String localRootFolder) {
		WatchListener listener = new WatchListener(uploadService, synchronizationService, remoteFolder,
				localRootFolder);
		Thread thread = new Thread(listener);
		thread.start();
		logger.info("[[INFO]] Starting watch thread {} on local/remote folders {}/{}", thread.getName(),
				localRootFolder, remoteFolder);
		threadPool.put(localRootFolder, thread);
		logger.debug("[[DEBUG]] ThreadPool current size: {}", threadPool.size());
	}

	public static void stopThread(String localRootFolder) {
		if (threadPool.get(localRootFolder) != null) {
			threadPool.get(localRootFolder).interrupt();
			threadPool.remove(localRootFolder);
		}
	}

	public static void log() {
		if(!logger.isDebugEnabled()) {
			logger.info("[[INFO]] Listeners watching for #{} folders", WatchListeners.threadPool.size());			
		} else {
			for(Entry<String, Thread> item : WatchListeners.threadPool.entrySet()) {
				logger.debug("[[DEBUG]] Folder {} watched by thread {}", item.getKey(), item.getValue().getName());
			}
		}
	}

	public static void lockSemaphore() {
		logger.debug("[[DEBUG]] Semaphore change from {} to {}", WatchListeners.threadSemaphore, WatchListeners.threadSemaphore + 1);
		WatchListeners.threadSemaphore++;
	}

	public static void releaseSemaphore() {
		logger.debug("[[DEBUG]] Semaphore change from {} to {}", WatchListeners.threadSemaphore, WatchListeners.threadSemaphore - 1);
		WatchListeners.threadSemaphore--;
	}

	public static boolean threadNotLocked() {
		logger.debug(WatchListeners.threadSemaphore == 0 ? "Semaphore not locked"
				: "Semaphore locked with value {} ", threadSemaphore);
		return WatchListeners.threadSemaphore == 0;
	}

	public static void putChangesWhileLocked(String syncFolder, String file) {
		synchronized (changesWhileLocked) {
			logger.debug("[[DEBUG]] Changing on file {} was made on folder {} programmatically by listener", 
					file, syncFolder);
			if (changesWhileLocked.containsKey(syncFolder)) {
				Collection<String> fileForFolder = changesWhileLocked.get(syncFolder);
				if (fileForFolder == null) {
					fileForFolder = Collections.synchronizedCollection(new ArrayList<>());
				}
				fileForFolder.add(file);
				changesWhileLocked.put(syncFolder, fileForFolder);
			} else {
				Collection<String> fileForFolder = Collections.synchronizedCollection(new ArrayList<>());
				fileForFolder.add(file);
				changesWhileLocked.put(syncFolder, fileForFolder);
			}
		}
	}

	public static boolean checkForProgrammaticallyChange(String syncFolder, String file) {
		if (changesWhileLocked.containsKey(syncFolder) && changesWhileLocked.get(syncFolder) != null
				&& changesWhileLocked.get(syncFolder).contains(file)) {
			logger.debug("[[DEBUG]] Event on file {}/{} suppressed because it was modified by S3Sync.",
					syncFolder, file);
			synchronized (changesWhileLocked) {
				changesWhileLocked.get(syncFolder).remove(file);
			}
			return true;
		} else {
			logger.debug("[[DEBUG]] Event on file {}/{} must be served.", syncFolder, file);
			return false;
		}
	}

	public static void cleanChangesWhileLocked(String syncFolder) {
		synchronized (changesWhileLocked) {
			changesWhileLocked.put(syncFolder, new ArrayList<>());
		}
	}
}
