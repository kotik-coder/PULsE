package pulse.ui.components;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskRepositoryListener;
import pulse.ui.Messages;

public class ExecutionButton extends ToolBarButton {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6317905957478587768L;

	private ExecutionState state = ExecutionState.EXECUTE;
	
	public ExecutionButton() {
		super();
		setBackground(state.getColor());
		setText(state.getMessage());
		
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
		this.setText(state.getMessage());
		this.setBackground(state.getColor());
	}
	
	
	public ExecutionState getExecutionState() {
		return state;
	}
	
}