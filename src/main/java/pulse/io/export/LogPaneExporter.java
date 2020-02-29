package pulse.io.export;

import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLEditorKit;

import pulse.ui.components.LogPane;

public class LogPaneExporter implements Exporter<LogPane> {

	private static LogPaneExporter instance = new LogPaneExporter();
	
	private LogPaneExporter() {
		//intentionally blank
	}

	@Override
	public void printToStream(LogPane pane, FileOutputStream fos, Extension extension) {
		HTMLEditorKit kit = (HTMLEditorKit) pane.getEditorKit();
		try {
			kit.write(fos, pane.getDocument(), 0, pane.getDocument().getLength());
		} catch (IOException | BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static LogPaneExporter getInstance() {
		return instance;
	}

	@Override
	public Class<LogPane> target() {
		return LogPane.class;
	}
	
	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] {Extension.HTML};
	}
	
}