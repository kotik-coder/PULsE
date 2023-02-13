package pulse.io.export;

import static pulse.io.export.Extension.HTML;

import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JEditorPane;

import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLEditorKit;

import pulse.ui.components.TextLogPane;

/**
 * Similar to a {@code LogExporter}, except that it works only on the contents
 * of a {@code LogPane} currently being displayed to the user.
 *
 */
public class TextLogPaneExporter implements Exporter<TextLogPane> {

    private static TextLogPaneExporter instance = new TextLogPaneExporter();

    private TextLogPaneExporter() {
        // intentionally blank
    }

    /**
     * This will write all contents of {@code pane}, which are accessed using an
     * {@code HTMLEditorKit} directly to {@code fos}. The {@code extension}
     * argument is ignored. After exporting, the stream is explicitly closed.
     */
    @Override
    public void printToStream(TextLogPane pane, FileOutputStream fos, Extension extension) {
        var editorPane = (JEditorPane) pane.getGUIComponent();
        var kit = (HTMLEditorKit) editorPane.getEditorKit();
        try {
            kit.write(fos, editorPane.getDocument(), 0, editorPane.getDocument().getLength());
        } catch (IOException | BadLocationException e) {
            System.err.println("Could not export the log pane!");
            e.printStackTrace();
        }
        try {
            fos.close();
        } catch (IOException e) {
            System.err.println("Unable to close stream");
            e.printStackTrace();
        }
    }

    /**
     * Gets the only static instance of this subclass.
     *
     * @return an instance of{@code LogPaneExporter}.
     */
    public static TextLogPaneExporter getInstance() {
        return instance;
    }

    /**
     * @return {@code LogPane.class}.
     */
    @Override
    public Class<TextLogPane> target() {
        return TextLogPane.class;
    }

    /**
     * Only html is currently supported by this exporter.
     */
    @Override
    public Extension[] getSupportedExtensions() {
        return new Extension[]{HTML};
    }

}
