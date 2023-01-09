package pulse.tasks.logs;

import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import javax.swing.JComponent;
import pulse.tasks.TaskManager;
import static pulse.tasks.logs.Status.DONE;
import pulse.util.Descriptive;

public abstract class AbstractLogger implements Descriptive {

    private final ExecutorService updateExecutor;

    public AbstractLogger() {
        updateExecutor = newSingleThreadExecutor();
    }

    public synchronized void update() {
        var task = TaskManager.getManagerInstance().getSelectedTask();

        if (task == null) {
            return;
        }

        var log = task.getLog();

        if (log.isStarted()) {
            post(log.lastEntry());
        }
        
    }

    public ExecutorService getUpdateExecutor() {
        return updateExecutor;
    }

    public synchronized void callUpdate() {
        updateExecutor.submit(() -> update());
    }

    public void postAll() {
        clear();

        var task = TaskManager.getManagerInstance().getSelectedTask();

        if (task != null) {

            var log = task.getLog();

            if (log.isStarted()) {

                log.getLogEntries().stream().forEach(entry -> post(entry));

                if (task.getStatus() == DONE) {
                    printTimeTaken(log);
                }

            }

        }

    }

    @Override
    public String describe() {
        var task = TaskManager.getManagerInstance().getSelectedTask();
        return "Log" + (task == null ? "" : "_" + task.getIdentifier().getValue());
    }

    public abstract JComponent getGUIComponent();

    public abstract void printTimeTaken(Log log);

    public abstract void post(LogEntry logEntry);

    public abstract void post(String text);

    public abstract void clear();

    public abstract boolean isEmpty();

}
