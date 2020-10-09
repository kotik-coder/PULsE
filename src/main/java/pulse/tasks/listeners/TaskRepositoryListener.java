package pulse.tasks.listeners;

public interface TaskRepositoryListener {
	public void onTaskListChanged(TaskRepositoryEvent e);
}