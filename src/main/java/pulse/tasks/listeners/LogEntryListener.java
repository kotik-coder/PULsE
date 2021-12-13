package pulse.tasks.listeners;

import pulse.tasks.logs.Log;
import pulse.tasks.logs.LogEntry;

public interface LogEntryListener {

    public void onNewEntry(LogEntry e);

    public void onLogFinished(Log log);

}
