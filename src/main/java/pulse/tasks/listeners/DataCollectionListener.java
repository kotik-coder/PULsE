package pulse.tasks.listeners;

import pulse.tasks.logs.LogEntry;

public interface DataCollectionListener {
	public void onDataCollected(LogEntry e);
}