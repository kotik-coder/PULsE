package pulse.input.listeners;

/**
 * <p>This is an enum type that is used to store some information about the type of the {@code DataEvent}s 
 * occurring with an {@code ExperimentalData} object. Currently, only one type of events is tracked.</p> 
 *
 */

public enum DataEventType {
	/**
	 * <p>The {@code TRUNCATED} {@code DataEventType} indicates 
	 * a part of the {@code ExperimentalData} has been truncated,
	 * i.e. permanently removed from the respective {@code time} and
	 * {@code temperature} {@code List}s.
	 * @see pulse.input.ExperimentalData.truncate() 
	 */
	
	TRUNCATED, 
	
	
	CHANGE_OF_ORIGIN;
	
}
