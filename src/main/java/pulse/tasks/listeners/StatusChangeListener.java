package pulse.tasks.listeners;

import java.io.Serializable;
import pulse.tasks.logs.StateEntry;

public interface StatusChangeListener extends Serializable {

    public void onStatusChange(StateEntry e);
}
