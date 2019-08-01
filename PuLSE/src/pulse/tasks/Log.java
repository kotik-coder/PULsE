package pulse.tasks;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import pulse.search.math.LogEntry;
import pulse.tasks.listeners.TaskListener;
import pulse.tasks.listeners.TaskStateEvent;
import pulse.util.Saveable;

public class Log implements Saveable {

	private List<LogEntry> logEntries;
	private LocalDateTime start, end;
	private static LogFormat logFormat = LogFormat.DEFAULT_FORMAT;
	private Identifier id;
	
	public Log(SearchTask task) {
		if(task == null)
			throw new IllegalArgumentException(Messages.getString("Log.NullTaskError")); //$NON-NLS-1$
		
		id = task.getIdentifier();
		this.logEntries = new LinkedList<LogEntry>();
	
		task.addTaskListener(new TaskListener() {
			
			@Override
			public void onDataCollected(TaskStateEvent src) {
				if(src.getState() != Status.INCOMPLETE) 
					logEntries.add( new DataLogEntry( task ) );
			}

			@Override
			public void onStatusChange(TaskStateEvent e) {
				logEntries.add(new EventLogEntry(e));	
				if(e.getState() == Status.IN_PROGRESS) {
					start = LocalDateTime.now();
					end = null;
				}
				if(e.getState() == Status.DONE)
					end = LocalDateTime.now();
			}
			
				
		});					
		
	}
	
	public static void setLogFormat(LogFormat logFormat) {
		Log.logFormat = logFormat;
	}
	
	public Identifier getIdentifier() {
		return id;
	}
	
	public boolean isStarted() {
		return start != null;
	}
	
	public boolean isFinished() {
		return end != null;
	}
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		String newLine = System.lineSeparator(); 
		
		sb.append(TaskManager.getTask(id));
		sb.append(newLine);
		sb.append(newLine);
		
		for(LogEntry le : logEntries) {
			sb.append(le);
			sb.append(newLine);
		}
		
		return sb.toString();
		
	}

	public List<LogEntry> getLogEntries() {
		return logEntries;
	}

	public static LogFormat getLogFormat() {
		return logFormat;
	}

	public LocalDateTime getStart() {
		return start;
	}

	public LocalDateTime getEnd() {
		return end;
	}

	@Override
	public void printData(FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);
		stream.print(toString());
	}
	
}
