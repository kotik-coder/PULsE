package pulse.input.listeners;

/**
 * An event type that is associated with an {@code ExperimentalData} object.
 *
 */

public enum DataEventType {
	/**
	 * <p>
	 * The {@code TRUNCATED} {@code DataEventType} indicates the range of the
	 * {@code ExperimentalData} has been truncated. Note this means that only the
	 * range is affected and not the data itself.
	 * 
	 * @see pulse.input.ExperimentalData.truncate()
	 */

	TRUNCATED

}