package pulse.tasks.logs;

import static java.lang.Integer.toHexString;
import static pulse.tasks.logs.Details.NONE;

import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;

public class StateEntry extends LogEntry {

    private static final long serialVersionUID = 8380229394939453079L;
    private Status status;

    public StateEntry(SearchTask task, Status status) {
        super(task);
        this.setStatus(status);
    }

    public SearchTask getTask() {
        return TaskManager.getManagerInstance().getTask(getIdentifier());
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
        var sb = new StringBuilder();
        sb.append("<br>");
        sb.append(getIdentifier().toString() + " changed status to ");
        var hex = "#" + toHexString(status.getColor().getRGB()).substring(2);
        sb.append("<b><font color='" + hex + "'>" + status.toString() + "</font></b>");
        if (status.getDetails() != NONE) {
            sb.append(" due to <b>" + status.getDetails() + "</b>");
        }
        if (status.getDetailedMessage().length() > 0) {
            sb.append(" Details: ");
            sb.append(status.getDetailedMessage());
        }
        sb.append(" at ");
        sb.append(getTime());
        return sb.toString();
    }

}
