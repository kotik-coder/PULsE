package pulse.tasks.listeners;

import java.time.LocalDateTime;

import pulse.tasks.SearchTask;
import pulse.tasks.Status;

public class TaskStateEvent {
	
	private SearchTask task;
	private LocalDateTime time;
	private Status status;
	
	public TaskStateEvent(SearchTask task, Status status) {
		this.setTask(task);
		this.setStatus(status);
		this.time = LocalDateTime.now();
	}

	public SearchTask getTask() {
		return task;
	}

	public void setTask(SearchTask task) {
		this.task = task;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
	
	public Status getState() {
		return task.getStatus();
	}
	
	public LocalDateTime getTime() {
		return time;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<br>");
		sb.append(task.toString() + " changed status to ");
		sb.append(status.toString());
		sb.append(" at ");
		sb.append(time.toLocalTime());
		return sb.toString();
	}

}
