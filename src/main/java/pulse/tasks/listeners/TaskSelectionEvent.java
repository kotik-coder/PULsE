package pulse.tasks.listeners;

import java.util.EventObject;

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

}
