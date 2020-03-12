package pulse.ui.frames;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import pulse.io.export.ExportManager;
import pulse.tasks.Log;
import pulse.tasks.LogEntry;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.LogEntryListener;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.ui.Messages;
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
		logTextPane	= new LogPane();
        var logScroller = new JScrollPane();               
        logScroller.setViewportView(logTextPane);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(logScroller, BorderLayout.CENTER);                		       
        
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;             
        
        getContentPane().add( 
        		new SystemPanel() 
        		, BorderLayout.PAGE_END);
        
        var logToolbar = new LogToolbar();
        logToolbar.addLogExportListener(() -> {
			if(logTextPane.getDocument().getLength() > 0)
				ExportManager.askToExport(logTextPane, 
						(JFrame)SwingUtilities.getWindowAncestor(this), 
						Messages.getString("LogToolBar.FileFormatDescriptor"));
		});
        getContentPane().add(logToolbar, BorderLayout.NORTH);        
        
	}
	
	
	private void scheduleLogEvents() {
		TaskManager.addSelectionListener(e -> logTextPane.printAll());
		
		TaskManager.addTaskRepositoryListener( event -> { 
			if(event.getState() != TaskRepositoryEvent.State.TASK_ADDED)
				return;
			
			SearchTask task = TaskManager.getTask(event.getId());
			
			task.getLog().addListener( new LogEntryListener() {

				@Override
				public void onLogFinished(Log log) {
					if(TaskManager.getSelectedTask() == task) {
						
						try {
							logTextPane.getUpdateExecutor().awaitTermination(10, TimeUnit.MILLISECONDS);
						} catch (InterruptedException e) {
							System.err.println("Log not finished in time");
							e.printStackTrace();
						}
						
						logTextPane.printTimeTaken(log);
						
					}
				}
				
				@Override
				public void onNewEntry(LogEntry e) {
					if(TaskManager.getSelectedTask() == task) 
						logTextPane.callUpdate();	
				}
				
			}
			
			);
			
			}
		);
	}

	public LogPane getLogTextPane() {
		return logTextPane;
	}
	
}