package pulse.tasks.listeners;

import pulse.tasks.LogEntry;

public interface DataCollectionListener {
	public void onDataCollected(LogEntry e);
}