package pulse.tasks;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import pulse.tasks.listeners.DataCollectionListener;
import pulse.tasks.listeners.LogEntryListener;
import pulse.tasks.listeners.StatusChangeListener;
import pulse.tasks.listeners.TaskStateEvent;
import pulse.util.Saveable;

public class Log implements Saveable {

	private List<LogEntry> logEntries;
	private LocalDateTime start, end;
	private static LogFormat logFormat = LogFormat.DEFAULT_FORMAT;
	private Identifier id;
	private List<LogEntryListener> listeners;
	
	public Log(SearchTask task) {
		if(task == null)
			throw new IllegalArgumentException(Messages.getString("Log.NullTaskError")); //$NON-NLS-1$
		
		id = task.getIdentifier();
		
		this.logEntries = new CopyOnWriteArrayList<LogEntry>();
		listeners		= new CopyOnWriteArrayList<LogEntryListener>();
	
		task.addTaskListener(new DataCollectionListener() {
			
			@Override
			public void onDataCollected(TaskStateEvent src) {
				if(src.getState() == Status.INCOMPLETE) 
					return;
				
				LogEntry e = new DataLogEntry( task );
				logEntries.add( e );
				notifyListeners(e);
			}
				
		});	
		
		task.addStatusChangeListener(new StatusChangeListener() {

			@Override
			public void onStatusChange(TaskStateEvent e) {
				LogEntry logEntry = new EventLogEntry(e); 
				logEntries.add(logEntry);
				
				if(e.getStatus() != Status.DONE) {
					
					if(e.getState() == Status.IN_PROGRESS) {
						start = e.getTime();
						end = null;
					}
					
					notifyListeners(logEntry);
				}
				
				else {
					end = e.getTime();
					notifyListeners(logEntry);
					logFinished();
				}
			}
			
		});
			
	}
	
	private void logFinished() {
		listeners.stream().forEach( l -> l.onLogFinished(this));
	}
	
	private void notifyListeners(LogEntry logEntry) {
		listeners.stream().forEach( l -> l.onNewEntry(logEntry));
	}
	
	public List<LogEntryListener> getListeners() {
		return listeners;
	}
	
	public void addListener(LogEntryListener l) {
		listeners.add(l);
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
	
	public LogEntry lastEntry() {
		return logEntries.stream().reduce( (first,second) -> second).get();
	}
	
}
