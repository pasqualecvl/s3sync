package it.s3sync.sync;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SynchronizationThreadPool {

	private static Queue<EventData> queue = new ConcurrentLinkedQueue<>();
	
	private static volatile AtomicInteger threadPoolSize = new AtomicInteger(0);
	
	private static final Integer threadPoolMaxSize = 30;
	
	public static void enqueue(EventData eventData) {
		if(threadPoolSize.get() < threadPoolMaxSize) {
			servingEvent(eventData);
		} else {
			SynchronizationThreadPool.queue.add(eventData);			
		}
		
	}
	
	
	
	public static boolean servingEvent(EventData eventData) {
		
	}
	
	
	public static class EventData {
		private String remoteFolder;
		private String localRootFolder;
		private String fullLocation;
		
		public EventData(String remoteFolder, String localRootFolder, String fullLocation) {
			this.remoteFolder = remoteFolder;
			this.localRootFolder = localRootFolder;
			this.fullLocation = fullLocation;
		}
		
		public String getRemoteFolder() {
			return remoteFolder;
		}
		
		public String getLocalRootFolder() {
			return localRootFolder;
		}
		
		public String getFullLocation() {
			return fullLocation;
		}
	}
	
	public static void serveNext(SynchornizationThread runnable) throws InterruptedException {
		runnable.join();
		synchronized(queue) {
			if(!queue.isEmpty()) {
				servingEvent(queue.poll());
			}			
		}
	}
}
