package pulse.tasks.listeners;

import java.io.Serializable;
import pulse.tasks.logs.Log;
import pulse.tasks.logs.LogEntry;

public interface LogEntryListener extends Serializable {

    public void onNewEntry(LogEntry e);

    public void onLogFinished(Log log);

}
