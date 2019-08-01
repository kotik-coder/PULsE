package pulse.tasks.listeners;

public interface TaskListener {
	public void onStatusChange(TaskStateEvent e);
	public void onDataCollected(TaskStateEvent e);
}