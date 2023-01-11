package pulse.ui.frames;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.PAGE_END;
import static java.awt.GridBagConstraints.WEST;
import static java.lang.System.err;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pulse.io.export.ExportManager.askToExport;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_ADDED;
import static pulse.ui.Messages.getString;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;

import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;

import pulse.tasks.TaskManager;
import pulse.tasks.listeners.LogEntryListener;
import pulse.tasks.logs.Log;
import pulse.tasks.logs.LogEntry;
import pulse.tasks.logs.AbstractLogger;
import pulse.ui.components.GraphicalLogPane;
import pulse.ui.components.TextLogPane;
import pulse.ui.components.listeners.LogListener;
import pulse.ui.components.panels.LogToolbar;
import pulse.ui.components.panels.SystemPanel;

@SuppressWarnings("serial")
public class LogFrame extends JInternalFrame {

    private AbstractLogger logger;
    private final static AbstractLogger graphical = new GraphicalLogPane();
    private final static AbstractLogger text = new TextLogPane();

    public LogFrame() {
        super("Log", true, false, true, true);
        initComponents();
        scheduleLogEvents();
        setVisible(true);
    }

    private void initComponents() {
        logger = Log.isGraphicalLog() ? graphical : text;

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(logger.getGUIComponent(), CENTER);

        var gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = WEST;
        gridBagConstraints.weightx = 0.5;

        getContentPane().add(new SystemPanel(), PAGE_END);

        var logToolbar = new LogToolbar();

        var lel = new LogListener() {
            @Override
            public void onLogExportRequest() {
                if (logger == text) {
                    askToExport(logger, null, getString("LogToolBar.FileFormatDescriptor"));
                } else {
                    System.out.println("To export the log entries, please switch to text mode first!");
                }
            }

            @Override
            public void onLogModeChanged(boolean graphical) {
                SwingUtilities.invokeLater(() -> setGraphicalLogger(graphical));
            }
        };

        logToolbar.addLogListener(lel);
        getContentPane().add(logToolbar, NORTH);

    }

    private void scheduleLogEvents() {
        var instance = TaskManager.getManagerInstance();
        instance.addSelectionListener(
                e -> SwingUtilities.invokeLater(() -> logger.postAll()));

        instance.addTaskRepositoryListener(event -> {
            if (event.getState() != TASK_ADDED) {
                return;
            }

            var task = instance.getTask(event.getId());

            task.getLog().addListener(new LogEntryListener() {

                @Override
                public void onLogFinished(Log log) {
                    if (instance.getSelectedTask() == task) {

                        try {
                            logger.getUpdateExecutor().awaitTermination(10, MILLISECONDS);
                        } catch (InterruptedException e) {
                            err.println("Log not finished in time");
                        }

                        logger.printTimeTaken(log);

                    }
                }

                @Override
                public void onNewEntry(LogEntry e) {
                    if (instance.getSelectedTask() == task) {
                        logger.callUpdate();
                    }
                }

            }
            );

        });
    }

    public AbstractLogger getLogger() {
        return logger;
    }

    private void setGraphicalLogger(boolean graphicalLog) {
        var old = logger;
        logger = graphicalLog ? graphical : text;

        if (old != logger) {
            getContentPane().remove(old.getGUIComponent());
            getContentPane().add(logger.getGUIComponent(), BorderLayout.CENTER);
            SwingUtilities.invokeLater(() -> logger.postAll());
        }

    }

}