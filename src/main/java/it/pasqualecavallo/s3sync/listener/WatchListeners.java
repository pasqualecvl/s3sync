package it.pasqualecavallo.s3sync.listener;

import java.util.HashMap;
import java.util.Map;

public class WatchListeners {
	
	private static Map<String, Thread> threadPool = new HashMap<>();
	
	public static void startThread(String rootLocation) {
		WatchListener listener = new WatchListener(rootLocation);
		Thread thread = new Thread(listener);
		thread.start();
		threadPool.put(rootLocation, thread);
	}
	
	public static void stopThread(String rootLocation) {
		if(threadPool.get(rootLocation) != null) {
			threadPool.get(rootLocation).interrupt();
			threadPool.remove(rootLocation);
		}
	}
	
}
