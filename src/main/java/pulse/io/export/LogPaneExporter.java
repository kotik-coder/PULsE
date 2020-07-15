package pulse.io.export;

import static pulse.io.export.Extension.HTML;

import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLEditorKit;

import pulse.ui.components.LogPane;

/**
 * Similar to a {@code LogExporter}, except that it works only on the contents of 
 * a {@code LogPane} currently being displayed to the user.
 *
 */

public class LogPaneExporter implements Exporter<LogPane> {

	private static LogPaneExporter instance = new LogPaneExporter();

	private LogPaneExporter() {
		// intentionally blank
	}
	
	/**
	 * This will write all contents of {@code pane}, which are accessed using an {@code HTMLEditorKit}
	 * directly to {@code fos}. The {@code extension} argument is ignored.
	 */
	
	@Override
	public void printToStream(LogPane pane, FileOutputStream fos, Extension extension) {
		var kit = (HTMLEditorKit) pane.getEditorKit();
		try {
			kit.write(fos, pane.getDocument(), 0, pane.getDocument().getLength());
		} catch (IOException | BadLocationException e) {
			System.err.println("Could not export the log pane!");
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets the only static instance of this subclass.
	 * @return an instance of{@code LogPaneExporter}.
	 */

	public static LogPaneExporter getInstance() {
		return instance;
	}
	
	/**
	 * @return {@code LogPane.class}. 
	 */

	@Override
	public Class<LogPane> target() {
		return LogPane.class;
	}
	
	/**
	 * Only html is currently supported by this exporter.
	 */

	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] { HTML};
	}

}