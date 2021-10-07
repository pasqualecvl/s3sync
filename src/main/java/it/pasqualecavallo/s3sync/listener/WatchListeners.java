package it.pasqualecavallo.s3sync.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import it.pasqualecavallo.s3sync.service.UploadService;

public class WatchListeners {
	
	private static Map<String, Thread> threadPool = new HashMap<>();
	
	private static volatile int threadSemaphore = 0;
	
	public static void startThread(UploadService uploadService, String remoteFolder, String localRootFolder) {
		WatchListener listener = new WatchListener(uploadService, remoteFolder, localRootFolder);
		Thread thread = new Thread(listener);
		thread.start();
		threadPool.put(localRootFolder, thread);
	}
	
	public static void stopThread(String localRootFolder) {
		if(threadPool.get(localRootFolder) != null) {
			threadPool.get(localRootFolder).interrupt();
			threadPool.remove(localRootFolder);
		}
	}
	
	public static void log() {
		System.out.println("Watching for " + WatchListeners.threadPool.size() + " folders");
		for(Entry<String, Thread> item : WatchListeners.threadPool.entrySet()) {
			System.out.println("Folder " + item.getKey() + " watched by thread " + item.getValue().getName());
		}
	}
	
	public static void lockSemaphore() {
		System.out.println("Semaphore current value " + WatchListeners.threadSemaphore);
		WatchListeners.threadSemaphore++;
		System.out.println("Semaphore increased value to: " + WatchListeners.threadSemaphore);
	}
	
	public static void releaseSemaphore() {
		System.out.println("Semaphore current value " + WatchListeners.threadSemaphore);
		WatchListeners.threadSemaphore--;
		System.out.println("Semaphore decreased value to: " + WatchListeners.threadSemaphore);
	}
	
	public static boolean threadNotLocked() {
		System.out.println(WatchListeners.threadSemaphore == 0 ? "Semaphore not locked" : "Semaphore locked with value: " + WatchListeners.threadSemaphore);
		return WatchListeners.threadSemaphore == 0;
	}
}
