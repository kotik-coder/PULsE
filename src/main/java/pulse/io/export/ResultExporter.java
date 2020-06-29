package pulse.io.export;

import static pulse.io.export.Extension.CSV;
import static pulse.io.export.Extension.HTML;

import java.io.FileOutputStream;
import java.io.PrintStream;

import pulse.tasks.Result;

public class ResultExporter implements Exporter<Result> {

	private static ResultExporter instance = new ResultExporter();

	private ResultExporter() {
		// intentionally blank
	}

	/**
	 * Prints the data of this Result with {@code fos} in an html-format.
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
                    stream.print(p.formattedValue(true));
                    
                    stream.print("</td>");
                    stream.println("</tr>");
                }
                
                stream.print("</table>");
            }
	}

	/**
	 * The supported extensions for exporting the data contained in this object.
	 * Currently include {@code .html} and {@code .csv}.
	 */

	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] { HTML, CSV};
	}

	private void printCSV(Result result, FileOutputStream fos) {
            try (var stream = new PrintStream(fos)) {
                stream.print("(Results)");
                
                for (var p : result.getProperties()) {
                    stream.printf("%n%-20.10s", p.getType());
                    stream.printf("\t%-20.10s", p.formattedValue(true));
                }
            }
	}

	@Override
	public Class<Result> target() {
		return Result.class;
	}

	public static ResultExporter getInstance() {
		return instance;
	}

}