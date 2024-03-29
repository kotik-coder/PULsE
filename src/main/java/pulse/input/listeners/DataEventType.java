package pulse.input.listeners;

/**
 * An event type that is associated with an {@code ExperimentalData} object.
 *
 */
public enum DataEventType {
    /**
     * <p>
     * The {@code RANGE_CHANGED} {@code DataEventType} indicates the range of
     * the {@code ExperimentalData} has either been truncated or extended. Note
     * this means that only the range is affected and not the data itself.
     *
     * @see pulse.input.ExperimentalData.truncate()
     */

    RANGE_CHANGED,
    /**
     * All data points loaded and are ready for processing.
     */
    DATA_LOADED;

}
