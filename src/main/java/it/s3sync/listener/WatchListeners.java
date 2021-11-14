package it.s3sync.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.s3sync.listener.SynchronizationMessageDto.S3Action;
import it.s3sync.service.SynchronizationService;
import it.s3sync.service.UploadService;

public class WatchListeners {

	private static Map<String, ThreadAndRunnable> threadPool = new HashMap<>();

	private static volatile Map<String, Collection<Operation>> changesWhileLocked = new HashMap<>();

	private static volatile int threadSemaphore = 0;

	private static final Logger logger = LoggerFactory.getLogger(WatchListeners.class);

	public static void startThread(UploadService uploadService, SynchronizationService synchronizationService,
			String remoteFolder, String localRootFolder) {
		WatchListener listener = new WatchListener(uploadService, synchronizationService, remoteFolder,
				localRootFolder);
		Thread thread = new Thread(listener);
		thread.start();
		logger.info("[[INFO]] Starting watch thread {} on local/remote folders {} -> {}", thread.getName(),
				localRootFolder, remoteFolder);
		threadPool.put(localRootFolder, new ThreadAndRunnable(thread, listener));
		logger.debug("[[DEBUG]] ThreadPool current size: {}", threadPool.size());
	}

	public static void stopThread(String localRootFolder) {
		if (threadPool.get(localRootFolder) != null) {
			threadPool.get(localRootFolder).getT().interrupt();
			threadPool.remove(localRootFolder);
		}
	}

	public static void log() {
		if(!logger.isDebugEnabled()) {
			logger.info("[[INFO]] Listeners watching for #{} folders", WatchListeners.threadPool.size());			
		} else {
			for(Entry<String, ThreadAndRunnable> item : WatchListeners.threadPool.entrySet()) {
				logger.debug("[[DEBUG]] Folder {} watched by thread {}", item.getKey(), item.getValue().getT().getName());
			}
		}
	}

	public static ThreadAndRunnable getThread(String localRootFolder) {
		return threadPool.get(localRootFolder);
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

	public static void putChangesWhileLocked(String folder, Operation operation) {
		synchronized (changesWhileLocked) {
			logger.debug("[[DEBUG]] Changing {} on file {} was made on folder {} programmatically by listener", 
					operation.getS3Action(), operation.getOnFile(), folder);
			if (changesWhileLocked.containsKey(folder)) {
				Collection<Operation> fileForFolder = changesWhileLocked.get(folder);
				if (fileForFolder == null) {
					fileForFolder = Collections.synchronizedCollection(new ArrayList<>());
				}
				fileForFolder.add(operation);
				changesWhileLocked.put(folder, fileForFolder);
			} else {
				Collection<Operation> fileForFolder = Collections.synchronizedCollection(new ArrayList<>());
				fileForFolder.add(operation);
				changesWhileLocked.put(folder, fileForFolder);
			}
		}
	}

	public static boolean checkForProgrammaticallyChange(String folder, Operation operation) {
		if (changesWhileLocked.containsKey(folder) && changesWhileLocked.get(folder) != null
				&& changesWhileLocked.get(folder).contains(operation)) {
			logger.debug("[[DEBUG]] Event {} on file {}/{} suppressed because it was modified by S3Sync.",
					operation.getS3Action(), folder, operation.getOnFile());
			synchronized (changesWhileLocked) {
				changesWhileLocked.get(folder).remove(operation);
			}
			return true;
		} else {
			logger.debug("[[DEBUG]] Event {} on file {}/{} must be served.", operation.getS3Action(), folder, operation.getOnFile());
			return false;
		}
	}

	public static void cleanChangesWhileLocked(String syncFolder) {
		synchronized (changesWhileLocked) {
			changesWhileLocked.put(syncFolder, new ArrayList<>());
		}
	}
	
	public static class ThreadAndRunnable {
		
		public ThreadAndRunnable(Thread t, Runnable r) {
			this.t = t;
			this.r = r;
		}
		
		private Thread t;
		private Runnable r;
		
		public Thread getT() {
			return t;
		}
		
		public void setT(Thread t) {
			this.t = t;
		}
		
		public Runnable getR() {
			return r;
		}
		
		public void setR(Runnable r) {
			this.r = r;
		}
	}
	
	public static class Operation {
		private String onFile;
		private S3Action s3Action;
		
		public String getOnFile() {
			return onFile;
		}
		
		public void setOnFile(String onFile) {
			this.onFile = onFile;
		}
		
		public S3Action getS3Action() {
			return s3Action;
		}
		
		public void setS3Action(S3Action s3Action) {
			this.s3Action = s3Action;
		}

		@Override
		public int hashCode() {
			return Objects.hash(onFile, s3Action);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Operation other = (Operation) obj;
			return onFile.equals(other.onFile) && s3Action == other.s3Action;
		}
		
		
	}
}
