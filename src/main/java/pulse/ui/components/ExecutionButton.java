package pulse.ui.components;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskRepositoryListener;
import pulse.ui.Launcher;
import pulse.ui.Messages;

@SuppressWarnings("serial")
public class ExecutionButton extends JButton {

	private ExecutionState state = ExecutionState.EXECUTE;
	
	public ExecutionButton() {
		super();
		setIcon(state.getIcon());
        setToolTipText(state.getMessage());
		
		this.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				/*
				 * STOP PRESSED?
				 */
				
				if(state == ExecutionState.STOP) {
					TaskManager.cancelAllTasks();	
					return;
				}
				
				/*
				 * EXECUTE PRESSED?
				 */
				
				if(TaskManager.getTaskList().isEmpty()) {
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()),
						    Messages.getString("TaskControlFrame.PleaseLoadData"), //$NON-NLS-1$
						    "No Tasks", //$NON-NLS-1$
						    JOptionPane.ERROR_MESSAGE);			
					return;
				}
					
				for(SearchTask t : TaskManager.getTaskList())
					switch(t.checkProblems()) {
						case READY :
						case TERMINATED :
						case DONE :
							continue;
						default : 
							JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()),
									    t + " is " + t.getStatus().getMessage() , //$NON-NLS-1$
									    "Task Not Ready", //$NON-NLS-1$
									    JOptionPane.ERROR_MESSAGE);			
							return;
					}

				TaskManager.executeAll();
						
			}
			
		});
		
		TaskManager.addTaskRepositoryListener(new TaskRepositoryListener() {

			@Override
			public void onTaskListChanged(TaskRepositoryEvent e) {
				switch(e.getState()) {
				case TASK_SUBMITTED :
					setExecutionState(ExecutionState.STOP);
					break;
				case TASK_FINISHED :
					if(TaskManager.isTaskQueueEmpty()) 
						setExecutionState(ExecutionState.EXECUTE);
					else
						setExecutionState(ExecutionState.STOP);
					break;
				case SHUTDOWN :
					setExecutionState(ExecutionState.EXECUTE);
				default : 
					return;
				}
	
			}
			
		});
		
	}
	
	public void setExecutionState(ExecutionState state) {
		this.state = state;
		this.setToolTipText(state.getMessage());
		this.setIcon(state.getIcon());
	}
	
	
	public ExecutionState getExecutionState() {
		return state;
	}
	
	public enum ExecutionState {
		EXECUTE("Execute All Tasks", Launcher.loadIcon("execute.png", 24)), 
		STOP("Terminate All Running Tasks", Launcher.loadIcon("stop.png", 24));

		private String message;
		private ImageIcon icon;
		
		private ExecutionState(String message, ImageIcon icon) {
			this.icon = icon;
			this.message = message;
		}
		
		public ImageIcon getIcon() {
			return icon;
		}
		
		public String getMessage() {
			return message;
		}
		
	}
	
}