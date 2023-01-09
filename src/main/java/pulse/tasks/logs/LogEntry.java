package pulse.tasks.logs;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import pulse.Response;

import pulse.tasks.Identifier;
import pulse.tasks.SearchTask;
import pulse.ui.Messages;

/**
 * <p>
 * An abstract class for {@code LogEntr}ies created for a specific
 * {@code SearchTask}. Contains a pointer to the {@code Identifier} of the
 * {@code SearchTask} and the local time associated with the moment this entry
 * has been initialised.
 * </p>
 *
 */
public class LogEntry {

    private final Identifier identifier;
    private final LocalTime time;
    private final LogEntry previous;
    
    /**
     * <p>
     * Creates a {@code LogEntry} from this {@code SearchTask}. The data of the
     * creation of this {@code LogEntry} will be stored.
     * </p>
     *
     * @param t a {@code SearchTask}
     */
    public LogEntry(SearchTask t) {
        Objects.requireNonNull(t, Messages.getString("LogEntry.NullTaskError"));
        time = LocalDateTime.now().toLocalTime();
        identifier = t.getIdentifier();
        var list = t.getLog().getLogEntries();
        if(list != null && !list.isEmpty()) {
            previous = list.get(list.size() - 1);
        }
        else {
            previous = null;
        }
    }
    
    public LogEntry getPreviousEntry() {
        return previous;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public LocalTime getTime() {
        return time;
    }

}