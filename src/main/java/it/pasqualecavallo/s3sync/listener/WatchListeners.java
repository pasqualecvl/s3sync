package it.pasqualecavallo.s3sync.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import it.pasqualecavallo.s3sync.service.UploadService;

public class WatchListeners {
	
	private static Map<String, Thread> threadPool = new HashMap<>();
	
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

}
