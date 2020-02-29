package pulse.io.export;

import java.io.FileOutputStream;
import java.io.PrintStream;

import pulse.tasks.Log;

public class LogExporter implements Exporter<Log> {

	private static LogExporter instance = new LogExporter();
	
	private LogExporter() {
		// TODO Auto-generated constructor stub
	}

	public static LogExporter getInstance() {
		return instance;
	}
	
	/**
	 * Prints all the data contained in this {@code Log} using {@code fos}. By default, this will
	 * output all data in an {@code html} format.
	 */

	@Override
	public void printToStream(Log log, FileOutputStream fos, Extension extension) {
		PrintStream stream = new PrintStream(fos);
		stream.print(log.toString());
	}

	@Override
	public Class<Log> target() {
		return Log.class;
	}
	
	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] {Extension.HTML};
	}
	
}