package pulse.tasks;

import pulse.search.math.LogEntry;
import pulse.tasks.listeners.TaskStateEvent;

public class EventLogEntry extends LogEntry {

	private TaskStateEvent event;
	
	public EventLogEntry(TaskStateEvent e) {
		super(e.getTask());
		this.event = e;
	}

	@Override
	public String toString() {
		return event.toString();
	}
	
}
