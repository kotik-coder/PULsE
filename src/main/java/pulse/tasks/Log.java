package pulse.tasks;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import pulse.tasks.listeners.DataCollectionListener;
import pulse.tasks.listeners.LogEntryListener;
import pulse.tasks.listeners.StatusChangeListener;
import pulse.ui.Messages;
import pulse.util.Group;

/**
 * A {@code Log} is used to track changes for a specific {@code SearchTask},
 * such as changes of status and/or data collection events.
 *
 */

public class Log extends Group {

	private List<LogEntry> logEntries;
	private LocalTime start, end;
	private Identifier id;
	private List<LogEntryListener> listeners;
	private static boolean verbose = false;

	/**
	 * Creates a {@code Log} for this {@code task} that will automatically store
	 * {@code TaskStatEvent}s and a list of {@code DataLogEntr}ies in thread-safe
	 * collections. This is done by adding a {@code TaskListener} and a
	 * {@code StatusChangeListener} to the {@code task} object.
	 * 
	 * @param task the task to be logged.
	 */

	public Log(SearchTask task) {
		if (task == null)
			throw new IllegalArgumentException(Messages.getString("Log.NullTaskError"));

		setParent(task);
		id = task.getIdentifier();

		this.logEntries = new CopyOnWriteArrayList<>();
		listeners = new CopyOnWriteArrayList<>();

		task.addTaskListener(new DataCollectionListener() {

			/**
			 * Do these actions each time data has been collected for this task.
			 */

			@Override
			public void onDataCollected(LogEntry le) {
				if (task.getStatus() == Status.INCOMPLETE)
					return;

				if (verbose) {
					logEntries.add(le);
					notifyListeners(le);
				}

			}

		});

		task.addStatusChangeListener(new StatusChangeListener() {

			/**
			 * Do these actions every time the task status has changed.
			 */

			@Override
			public void onStatusChange(StateEntry e) {
				logEntries.add(e);

				if (e.getStatus() != Status.DONE) {

					if (e.getState() == Status.IN_PROGRESS) {
						start = e.getTime();
						end = null;
					}

					notifyListeners(e);

				}

				else {
					end = e.getTime();
					notifyListeners(e);
					logFinished();
				}
			}

		});

	}

	private void logFinished() {
		listeners.stream().forEach(l -> l.onLogFinished(this));
	}

	private void notifyListeners(LogEntry logEntry) {
		listeners.stream().forEach(l -> l.onNewEntry(logEntry));
	}

	public List<LogEntryListener> getListeners() {
		return listeners;
	}

	public void addListener(LogEntryListener l) {
		listeners.add(l);
	}

	public Identifier getIdentifier() {
		return id;
	}

	/**
	 * Checks whether this {@code Log} has observed a {@code TaskStateEvent}
	 * triggered by a change of status of its respective {@code SearchTask} to
	 * {@code IN_PROGRESS}.
	 * 
	 * @return {@code true} if the start time is not {@code null}
	 */

	public boolean isStarted() {
		return start != null;
	}

	/**
	 * Outputs all log entries consecutively.
	 */

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		String newLine = System.lineSeparator();

		sb.append(TaskManager.getTask(id));
		sb.append(newLine);
		sb.append(newLine);

		for (LogEntry le : logEntries) {
			sb.append(le);
			sb.append(newLine);
		}

		return sb.toString();

	}

	public List<LogEntry> getLogEntries() {
		return logEntries;
	}

	/**
	 * This is the time after the creation of the {@code Log} when a change of
	 * status to {@code IN_PROGRESS} happened.
	 * 
	 * @return the start time
	 */

	public LocalTime getStart() {
		return start;
	}

	/**
	 * This is the time after the creation of the {@code Log} when a change of
	 * status to {@code DONE} happened.
	 * 
	 * @return the start time
	 */

	public LocalTime getEnd() {
		return end;
	}

	/**
	 * Finds the last recorded entry in this {@code Log}.
	 * 
	 * @return last recorded entry.
	 */

	public LogEntry lastEntry() {
		return logEntries.stream().reduce((first, second) -> second).get();
	}

	/**
	 * Checks whether this {@code Log} is verbose. Verbose logs stores all data
	 * entries and outputs them on request. This is useful to get an idea of how the
	 * search method works, how many iterations are taken to reach a converged
	 * value, etc.
	 * 
	 * @return {@code true} if the verbose flag is on
	 */

	public static boolean isVerbose() {
		return verbose;
	}

	/**
	 * Sets the verbose flag to {@code verbose}
	 * 
	 * @param verbose the new value of the flag
	 * @see isVerbose()
	 */

	public static void setVerbose(boolean verbose) {
		Log.verbose = verbose;
	}

}