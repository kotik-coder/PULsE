package pulse.tasks.logs;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import pulse.tasks.Identifier;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.LogEntryListener;
import pulse.ui.Messages;
import pulse.util.Group;

/**
 * A {@code Log} is used to track changes for a specific {@code SearchTask},
 * such as changes of status and/or data collection events.
 *
 */
public class Log extends Group {

    private List<LogEntry> logEntries;
    private LocalTime start;
    private LocalTime end;
    private final Identifier id;
    private final List<LogEntryListener> listeners;
    private static boolean verbose = false;

    /**
     * Creates a {@code Log} for this {@code task} that will automatically store
     * {@code TaskStatEvent}s and a list of {@code DataLogEntr}ies in
     * thread-safe collections. This is done by adding a {@code TaskListener}
     * and a {@code StatusChangeListener} to the {@code task} object.
     *
     * @param task the task to be logged.
     */
    public Log(SearchTask task) {
        Objects.requireNonNull(task, Messages.getString("Log.NullTaskError"));

        setParent(task);
        id = task.getIdentifier();

        this.logEntries = new CopyOnWriteArrayList<>();
        listeners = new CopyOnWriteArrayList<>();

        task.addTaskListener(le -> {

            /**
             * Do these actions each time data has been collected for this task.
             */
            if (task.getStatus() != Status.INCOMPLETE && verbose) {
                logEntries.add(le);
                notifyListeners(le);
            }

        });

        task.addStatusChangeListener((StateEntry e) -> {
            logEntries.add(e);
            
            if (e.getStatus() == Status.IN_PROGRESS) {
                start = e.getTime();
                end = null;
            } else {
                end = e.getTime();
            }
            
            notifyListeners(e);
            
            if (e.getState() == Status.DONE) {
                logFinished();
            }
        } /**
         * Do these actions every time the task status has changed.
         */ 
        );

    }

    private void logFinished() {
        listeners.stream().forEach(l -> l.onLogFinished(this));
    }

    private void notifyListeners(LogEntry logEntry) {
        listeners.stream().forEach(l -> l.onNewEntry(logEntry));
    }

    public final List<LogEntryListener> getListeners() {
        return listeners;
    }

    public final void addListener(LogEntryListener l) {
        listeners.add(l);
    }

    public final Identifier getIdentifier() {
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

        sb.append(TaskManager.getManagerInstance().getTask(id));
        sb.append(newLine);
        sb.append(newLine);

        logEntries.stream().map(le -> {
            sb.append(le);
            return le;
        }).forEachOrdered(_item -> {
            sb.append(newLine);
        });

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
     * entries and outputs them on request. This is useful to get an idea of how
     * the search method works, how many iterations are taken to reach a
     * converged value, etc.
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
