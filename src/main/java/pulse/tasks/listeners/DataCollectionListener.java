package pulse.tasks.listeners;

import java.io.Serializable;
import pulse.tasks.logs.LogEntry;

public interface DataCollectionListener extends Serializable {

    public void onDataCollected(LogEntry e);

}
