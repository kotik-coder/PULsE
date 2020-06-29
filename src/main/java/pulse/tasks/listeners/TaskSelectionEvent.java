package pulse.tasks.listeners;

import static pulse.tasks.TaskManager.getSelectedTask;

import java.util.EventObject;

import pulse.tasks.SearchTask;

public class TaskSelectionEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4278832926994139917L;

	public TaskSelectionEvent(Object source) {
		super(source);
		// TODO Auto-generated constructor stub
	}

	public void setSource(Object source) {
		this.source = source;
	}

	public SearchTask getSelection() {
		return getSelectedTask();
	}

}
