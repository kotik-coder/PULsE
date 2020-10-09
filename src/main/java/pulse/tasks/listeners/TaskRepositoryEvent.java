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

		/**
		 * Indicates a task has been added to the repository.
		 */

		TASK_ADDED,

		/**
		 * A task has been removed from the repository.
		 */

		TASK_REMOVED,

		/**
		 * A task has been submitted for execution.
		 */

		TASK_SUBMITTED,

		/**
		 * A task has finished executing.
		 */

		TASK_FINISHED,

		/**
		 * A task has been reset.
		 */

		TASK_RESET,
		
		/**
		 * An external request has been received to browse previous calculations.
		 */
		
		TASK_BROWSING_REQUEST,
		
		/**
		 * The task has switched to a new model.
		 */
		
		TASK_MDOEL_SWITCH,

		/**
		 * The repository has been shut down/
		 */

		SHUTDOWN;

	}

}