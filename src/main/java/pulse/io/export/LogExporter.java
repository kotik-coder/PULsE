package pulse.io.export;

import static pulse.io.export.Extension.HTML;

import java.io.FileOutputStream;
import java.io.PrintStream;

import pulse.tasks.Log;

/**
 * A singleton {@code LogExporter} works on {@code Log} objects to write html files 
 * containing the full contents of the {@code Log}. Note csv output is not 
 * supported.
 *
 */

public class LogExporter implements Exporter<Log> {

	private static LogExporter instance = new LogExporter();

	private LogExporter() {
		//intentionally blank
	}

	/**
	 * Gets the only static instance of this subclass.
	 * @return an instance of{@code LogExporter}.
	 */
	
	public static LogExporter getInstance() {
		return instance;
	}

	/**
	 * Prints all the data contained in this {@code Log} using {@code fos}. By
	 * default, this will output all data in an {@code html} format. Note this implementation
	 * ignores the {@code extension} parameter.
	 * @param a log to be exported
	 * @see pulse.tasks.Log.toString()
	 */

	@Override
	public void printToStream(Log log, FileOutputStream fos, Extension extension) {
		var stream = new PrintStream(fos);
		stream.print(log.toString());
	}
	
	/**
	 * @return {@code Log.class}. 
	 */

	@Override
	public Class<Log> target() {
		return Log.class;
	}
	
	/**
	 * Only html is currently supported by this exporter.
	 */

	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] { HTML};
	}

}