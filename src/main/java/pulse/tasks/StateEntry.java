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
		String hex = "#"+Integer.toHexString(status.getColor().getRGB()).substring(2);
		sb.append("<b><font color='" + hex + "'>" + status.toString() + "</font></b>");
		if(status.getDetails() != Status.Details.NONE) sb.append(" due to <b>" + status.getDetails() + "</b>");
		sb.append(" at ");
		sb.append(getTime());
		return sb.toString();
	}

}
