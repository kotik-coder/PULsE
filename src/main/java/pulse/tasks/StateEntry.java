package pulse.tasks;

public class StateEntry extends LogEntry {
	
	private Status status;
	
	public StateEntry(SearchTask task, Status status) {
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
