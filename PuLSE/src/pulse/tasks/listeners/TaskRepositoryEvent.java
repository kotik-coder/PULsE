package pulse.tasks.listeners;

import pulse.tasks.Identifier;

public class TaskRepositoryEvent {
	
	private State state;
	private Identifier id;
	
	public TaskRepositoryEvent(State state, Identifier id) {
		this.state = state;
		this.id = id;
	}
	
	public State getState() {
		return state;
	}
	
	public Identifier getId() {
		return id;
	}
	
	public enum State {
		TASK_ADDED, TASK_REMOVED, ALL_TASKS_FINISHED, SINGLE_TASK_SUBMITTED, MULTIPLE_TASKS_SUBMITTED, TASK_FINISHED, TASK_RESET;
	}
	
}
