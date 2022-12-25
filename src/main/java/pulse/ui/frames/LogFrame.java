package pulse.ui.frames;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.PAGE_END;
import static java.awt.GridBagConstraints.WEST;
import static java.lang.System.err;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.io.export.ExportManager.askToExport;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_ADDED;
import static pulse.ui.Messages.getString;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;

import pulse.tasks.TaskManager;
import pulse.tasks.listeners.LogEntryListener;
import pulse.tasks.logs.Log;
import pulse.tasks.logs.LogEntry;
import pulse.tasks.logs.AbstractLogger;
import pulse.ui.components.TextLogPane;
import pulse.ui.components.panels.LogToolbar;
import pulse.ui.components.panels.SystemPanel;

@SuppressWarnings("serial")
public class LogFrame extends JInternalFrame {

    private AbstractLogger logger;

    public LogFrame() {
        super("Log", true, false, true, true);
        initComponents();
        scheduleLogEvents();
        setVisible(true);
    }

    private void initComponents() {
        logger = new TextLogPane();
        var logScroller = new JScrollPane();
        logScroller.setViewportView(logger.getGUIComponent());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(logScroller, CENTER);

        var gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = WEST;
        gridBagConstraints.weightx = 0.5;

        getContentPane().add(new SystemPanel(), PAGE_END);

        var logToolbar = new LogToolbar();
        logToolbar.addLogExportListener(() -> {
            if (!logger.isEmpty()) {
                askToExport(logger, (JFrame) getWindowAncestor(this),
                        getString("LogToolBar.FileFormatDescriptor"));
            }
        });
        getContentPane().add(logToolbar, NORTH);

    }

    private void scheduleLogEvents() {
        var instance = TaskManager.getManagerInstance();
        instance.addSelectionListener(e -> logger.postAll());

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

}