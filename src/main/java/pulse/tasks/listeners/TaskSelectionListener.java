package pulse.tasks.listeners;

import java.io.Serializable;

public interface TaskSelectionListener extends Serializable {

    public void onSelectionChanged(TaskSelectionEvent e);

}