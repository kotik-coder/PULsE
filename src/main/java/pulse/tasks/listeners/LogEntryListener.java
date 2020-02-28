package pulse.tasks.listeners;

import pulse.tasks.Log;
import pulse.tasks.LogEntry;

public interface LogEntryListener {

	public void onNewEntry(LogEntry e);
	public void onLogFinished(Log log);
	
}