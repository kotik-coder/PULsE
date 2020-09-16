package pulse.tasks.processing;

import pulse.tasks.SearchTask;
import pulse.ui.Messages;

/**
 * The individual {@code Result} that is associated with a {@code SearchTask}.
 * The {@code Identifier} of the task is stored as a field value.
 * 
 * @see pulse.tasks.SearchTask
 * @see pulse.tasks.Identifier
 */

public class Result extends AbstractResult {

	/**
	 * Creates an individual {@code Result} related to the current state of
	 * {@code task} using the specified {@code format}.
	 * 
	 * @param task   a {@code SearchTask}, the properties of which that conform to
	 *               {@code ResultFormat} will form this {@code Result}
	 * @param format a {@code ResultFormat}
	 * @throws IllegalArgumentException if {@code task} is null
	 */

	public Result(SearchTask task, ResultFormat format) throws IllegalArgumentException {
		super(format);

		if (task == null)
			throw new IllegalArgumentException(Messages.getString("Result.NullTaskError"));

		setParent(task);

		format.getKeywords().stream().forEach(key -> addProperty(task.numericProperty(key)));

	}

}