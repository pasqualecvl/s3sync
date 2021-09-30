package it.pasqualecavallo.s3sync.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import software.amazon.awssdk.services.sqs.SqsClient;

public class WatchListeners {
	
	private static Map<String, Thread> threadPool = new HashMap<>();
	
	public static void startThread(String rootLocation, SqsClient sqsClient) {
		WatchListener listener = new WatchListener(rootLocation, sqsClient);
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
	
	public static void log() {
		System.out.println("Watching for " + WatchListeners.threadPool.size() + " folders");
		for(Entry<String, Thread> item : WatchListeners.threadPool.entrySet()) {
			System.out.println("Folder " + item.getKey() + " watched by thread " + item.getValue().getName());
		}
	}

}
