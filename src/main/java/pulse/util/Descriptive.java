package pulse.util;

/**
 * Provides the {@code describe()} functionality.
 * 
 * @see pulse.io.export.Exporter
 */

public interface Descriptive {

	/**
	 * Creates a {@code String} 'describing' this object, usually for exporting
	 * purposes.
	 * 
	 * @return by default, this will return the name of the implementing class and
	 *         the date of the calculation.
	 */

	public default String describe() {

		return getClass().getSimpleName();

	}

}