package it.s3sync.sync;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SynchronizationThreadPool {

	private static volatile Queue<EventData> queue = new ConcurrentLinkedQueue<>();

	private static volatile AtomicInteger threadPoolSize = new AtomicInteger(0);

	private static final Integer threadPoolMaxSize = 30;

	private SynchronizationThreadPool() { }
	
	public static void enqueue(EventData eventData) {
		synchronized(threadPoolSize) {
			if (threadPoolSize.get() < threadPoolMaxSize) {
				servingEvent(eventData);
				threadPoolSize.decrementAndGet();
			} else {
				SynchronizationThreadPool.queue.add(eventData);
			}
		}
	}

	public static void servingEvent(EventData eventData) {
		new SynchornizationThread(eventData).start();
	}
	

	
	public static void serveNext(SynchornizationThread runnable) throws InterruptedException {
		runnable.join();
		synchronized (queue) {
			if (!queue.isEmpty()) {
				servingEvent(queue.poll());
			}
		}
	}
}
