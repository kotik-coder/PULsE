package pulse.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Provides the {@code describe()} functionality.
 * @see pulse.util.Saveable
 */

public interface Describable {

	/**
	 * Creates a {@code String} 'describing' this object, usually for exporting purposes.
	 * @return by default, this will return the name of the implementing class and the date 
	 * of the calculation.
	 */
	
	public default String describe() {
		
		return getClass().getSimpleName() + "_" +
				DateTimeFormatter.ISO_DATE.format( LocalDateTime.now() );
		
	};
	
}