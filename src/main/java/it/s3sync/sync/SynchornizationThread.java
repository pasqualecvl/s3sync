package it.s3sync.sync;

import it.s3sync.sync.SynchronizationThreadPool.EventData;

public class SynchornizationThread extends Thread {

	private EventData eventData;
	
	public SynchornizationThread(EventData eventData) {
		this.eventData = eventData;
	}
	
	@Override
	public void run() {
		
	}

}
