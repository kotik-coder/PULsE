package pulse.tasks.listeners;

import java.io.Serializable;

public interface TaskRepositoryListener extends Serializable {

    public void onTaskListChanged(TaskRepositoryEvent e);

}