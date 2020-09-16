package pulse.tasks.logs;

import java.awt.Color;

/**
 * An enum that represents the different states in which a {@code SearchTask}
 * can be.
 *
 */

public enum Status {

	/**
	 * Not all necessary details have been uploaded to a {@code SearchTask} and that
	 * it cannot be executed yet.
	 */

	INCOMPLETE(Color.RED),

	/**
	 * Everything seems to be in order and the task can now be executed.
	 */

	READY(Color.MAGENTA),

	/**
	 * The task is being executed.
	 */

	IN_PROGRESS(Color.DARK_GRAY),

	/**
	 * Task successfully finished.
	 */

	DONE(Color.BLUE),

	/**
	 * An error has occurred during execution.
	 */

	EXECUTION_ERROR(Color.red),

	/**
	 * The task has been terminated by the user.
	 */

	TERMINATED(Color.DARK_GRAY),

	/**
	 * Task has been queued and is waiting to be executed.
	 */

	QUEUED(Color.GREEN),

	/**
	 * Task has finished, but the results cannot be considered reliable (perhaps,
	 * due to large scatter of data points).
	 */

	AMBIGUOUS(Color.GRAY),

	/**
	 * The iteration limit has been reached and the task aborted.
	 */

	TIMEOUT(Color.RED),

	/**
	 * Task has finished without errors, however failing to meet a statistical
	 * criterion.
	 */

	FAILED(Color.RED);

	private final Color clr;
	private Details details = Details.NONE;

	Status(Color clr) {
		this.clr = clr;
	}

	public final Color getColor() {
		return clr;
	}

	public Details getDetails() {
		return details;
	}

	public void setDetails(Details details) {
		this.details = details;
	}

	static String parse(String str) {
		var tokens = str.split("_");
		var sb = new StringBuilder();
		final var BLANK_SPACE = ' ';
		for (var t : tokens) {
			sb.append(t.toLowerCase());
			sb.append(BLANK_SPACE);
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return parse(super.toString());
	}

	public String getMessage() {
		var sb = new StringBuilder();
		sb.append(toString());
		if (details != null)
			sb.append(" : ").append(details.toString());
		return sb.toString();
	}

}