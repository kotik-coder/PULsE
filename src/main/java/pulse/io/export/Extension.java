package pulse.io.export;

/**
 * Describes the available extensions for the exported files. Each extensions is 
 * associated with the inherent format. The subclasses of the {@code Exporter} class
 * are responsible for observing adherence to that format.
 *
 */

public enum Extension {

	/**
	 * The result will be a document with html tags that can be viewed in any web
	 * browser. Useful for complex formatting, but not for data manipulation, as the
	 * tags and special symbols do not allow simple parsing through the file.
	 */

	HTML,

	/**
	 * The result will be a tab-delimited CSV document. Useful for data
	 * manipulations and plotting with external tools, e.g. gnuplot or LaTeX.
	 */

	CSV;

	/**
	 * This will return the lower-case string with the name of the extension (e.g., html or csv).
	 */

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}

}