package pulse.ui.components;

import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import javax.swing.JComponent;
import pulse.tasks.TaskManager;
import pulse.tasks.logs.Log;
import pulse.tasks.logs.LogEntry;
import pulse.util.Descriptive;

public abstract class AbstractLogger implements Descriptive {

    private final ExecutorService updateExecutor = newSingleThreadExecutor();
    
    public synchronized void update() {
        var task = TaskManager.getManagerInstance().getSelectedTask();

        if (task == null) {
            return;
        }

        var log = task.getLog();

        if (!log.isStarted()) {
            return;
        }

        post(log.lastEntry());
    }
    
    public ExecutorService getUpdateExecutor() {
        return updateExecutor;
    }
    
    public synchronized void callUpdate() {
        updateExecutor.submit(() -> update());
    }
    
    public abstract JComponent getGUIComponent();
    public abstract void printTimeTaken(Log log);
    public abstract void post(LogEntry logEntry);
    public abstract void post(String text);
    public abstract void postAll();
    public abstract void clear();
    public abstract boolean isEmpty();
    
    @Override
    public String describe() {
        return "Log_" + TaskManager.getManagerInstance().getSelectedTask().getIdentifier().getValue();
    }
    
}