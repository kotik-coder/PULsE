package pulse.io.export;

import static pulse.io.export.Extension.CSV;
import static pulse.io.export.Extension.HTML;

import java.io.FileOutputStream;
import java.io.PrintStream;

import pulse.tasks.Result;

/**
 * Provides export capabilities for instances of the {@code Result} class
 * both in the {@code csv} and {@code html} formats.
 *
 */

public class ResultExporter implements Exporter<Result> {

	private static ResultExporter instance = new ResultExporter();

	private ResultExporter() {
		// intentionally blank
	}

	/**
	 * Prints the data of this {@code Result} with {@code fos} either in a {@code html} or a {@code csv} file format.
	 */

	@Override
	public void printToStream(Result result, FileOutputStream fos, Extension extension) {
		switch (extension) {
		case HTML:
			printHTML(result, fos);
			break;
		case CSV:
			printCSV(result, fos);
			break;
		default:
			throw new IllegalArgumentException("Format not recognised: " + extension);
		}
	}

	private void printHTML(Result result, FileOutputStream fos) {
            try (var stream = new PrintStream(fos)) {
                stream.print("<table>");
                
                for (var p : result.getProperties()) {
                    stream.print("<tr>");
                    stream.print("<td>");
                    
                    stream.print(p.getDescriptor(true));
                    stream.print("</td><td>");
                    stream.print(p.formattedValueAndError(true));
                    
                    stream.print("</td>");
                    stream.println("</tr>");
                }
                
                stream.print("</table>");
            }
	}

	/**
	 * Currently the supported extensions include {@code .html} and {@code .csv}.
	 */

	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] { HTML, CSV};
	}

	private void printCSV(Result result, FileOutputStream fos) {
            try (var stream = new PrintStream(fos)) {
                stream.print("(Results)");
                
                for (var p : result.getProperties()) {
                    stream.printf("%n%-24.12s", p.getType());
                    stream.printf("\t%-24.12s", p.formattedValueAndError(true));
                }
            }
	}
	
	/**
	 * @return {@code Result.class}
	 */

	@Override
	public Class<Result> target() {
		return Result.class;
	}

	/**
	 * Returns the single static instance of this class.
	 * @return instance an instance of this class.
	 */
	
	public static ResultExporter getInstance() {
		return instance;
	}

}