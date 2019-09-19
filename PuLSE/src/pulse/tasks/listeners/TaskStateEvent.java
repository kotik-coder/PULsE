package pulse.tasks.listeners;

import pulse.tasks.LogEntry;
import pulse.tasks.SearchTask;
import pulse.tasks.Status;
import pulse.tasks.TaskManager;

public class TaskStateEvent extends LogEntry {
	
	private Status status;
	
	public TaskStateEvent(SearchTask task, Status status) {
		super(task);
		this.setStatus(status);
	}

	public SearchTask getTask() {
		return TaskManager.getTask(getIdentifier());
	}
	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
	
	public Status getState() {
		return status;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<br>");
		sb.append(getIdentifier().toString() + " changed status to ");
		sb.append(status.toString());
		sb.append(" at ");
		sb.append(getTime());
		return sb.toString();
	}

}
