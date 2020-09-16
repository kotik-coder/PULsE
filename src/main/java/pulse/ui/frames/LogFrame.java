package pulse.ui.frames;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.PAGE_END;
import static java.awt.GridBagConstraints.WEST;
import static java.lang.System.err;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.io.export.ExportManager.askToExport;
import static pulse.tasks.TaskManager.addSelectionListener;
import static pulse.tasks.TaskManager.addTaskRepositoryListener;
import static pulse.tasks.TaskManager.getSelectedTask;
import static pulse.tasks.TaskManager.getTask;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_ADDED;
import static pulse.ui.Messages.getString;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;

import pulse.tasks.listeners.LogEntryListener;
import pulse.tasks.logs.Log;
import pulse.tasks.logs.LogEntry;
import pulse.ui.components.LogPane;
import pulse.ui.components.panels.LogToolbar;
import pulse.ui.components.panels.SystemPanel;

@SuppressWarnings("serial")
public class LogFrame extends JInternalFrame {

	private LogPane logTextPane;

	public LogFrame() {
		super("Log", true, false, true, true);
		initComponents();
		scheduleLogEvents();
		setVisible(true);
	}

	private void initComponents() {
		logTextPane = new LogPane();
		var logScroller = new JScrollPane();
		logScroller.setViewportView(logTextPane);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(logScroller, CENTER);

		var gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.anchor = WEST;
		gridBagConstraints.weightx = 0.5;

		getContentPane().add(new SystemPanel(), PAGE_END);

		var logToolbar = new LogToolbar();
		logToolbar.addLogExportListener(() -> {
			if (logTextPane.getDocument().getLength() > 0)
				askToExport(logTextPane, (JFrame) getWindowAncestor(this),
						getString("LogToolBar.FileFormatDescriptor"));
		});
		getContentPane().add(logToolbar, NORTH);

	}

	private void scheduleLogEvents() {
		addSelectionListener(e -> logTextPane.printAll());

		addTaskRepositoryListener(event -> {
			if (event.getState() != TASK_ADDED)
				return;

			var task = getTask(event.getId());

			task.getLog().addListener(new LogEntryListener() {

				@Override
				public void onLogFinished(Log log) {
					if (getSelectedTask() == task) {

						try {
							logTextPane.getUpdateExecutor().awaitTermination(10, MILLISECONDS);
						} catch (InterruptedException e) {
							err.println("Log not finished in time");
							e.printStackTrace();
						}

						logTextPane.printTimeTaken(log);

					}
				}

				@Override
				public void onNewEntry(LogEntry e) {
					if (getSelectedTask() == task)
						logTextPane.callUpdate();
				}

			}

			);

		});
	}

	public LogPane getLogTextPane() {
		return logTextPane;
	}

}