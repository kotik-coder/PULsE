package pulse.tasks;

import java.time.LocalDateTime;
import java.time.LocalTime;

import pulse.ui.Messages;

public abstract class LogEntry {

	private Identifier identifier;
	private LocalTime time;
	
	public LogEntry(SearchTask t) {
		if(t == null)
			throw new IllegalArgumentException(Messages.getString("LogEntry.NullTaskError")); //$NON-NLS-1$
		time = LocalDateTime.now().toLocalTime();
		identifier = t.getIdentifier();
	}
	
	public Identifier getIdentifier() {
		return identifier;
	}
	
	public LocalTime getTime() {
		return time;
	}
	
	public boolean isEarlierThan(LogEntry logEntry) {
		return logEntry.getTime().isAfter(this.getTime());
	}
	
}
