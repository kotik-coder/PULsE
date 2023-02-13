package pulse.ui.components;

import pulse.tasks.logs.AbstractLogger;
import static java.lang.System.err;
import static javax.swing.text.DefaultCaret.ALWAYS_UPDATE;
import static pulse.ui.Messages.getString;

import java.io.IOException;
import javax.swing.JComponent;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import pulse.tasks.logs.Log;
import pulse.tasks.logs.LogEntry;

@SuppressWarnings("serial")
public class TextLogPane extends AbstractLogger {

    private final JEditorPane editor;
    private final JScrollPane pane;

    public TextLogPane() {
        editor = new JEditorPane();
        editor.setContentType("text/html");
        editor.setEditable(false);
        ((DefaultCaret) editor.getCaret()).setUpdatePolicy(ALWAYS_UPDATE);
        pane = new JScrollPane();
        pane.setViewportView(editor);
    }

    @Override
    public void post(LogEntry logEntry) {
        post(logEntry.toString());
    }

    @Override
    public void post(String text) {

        final var doc = (HTMLDocument) editor.getDocument();
        final var kit = (HTMLEditorKit) editor.getEditorKit();
        try {
            kit.insertHTML(doc, doc.getLength(), text, 0, 0, null);
        } catch (BadLocationException e) {
            err.println(getString("LogPane.InsertError")); //$NON-NLS-1$
        } catch (IOException e) {
            err.println(getString("LogPane.PrintError")); //$NON-NLS-1$
        }

    }

    public void printTimeTaken(Log log) {
        var time = log.timeTaken();
        var sb = new StringBuilder();
        sb.append(getString("LogPane.TimeTaken")); //$NON-NLS-1$
        sb.append(time[0]).append(getString("LogPane.Seconds")); //$NON-NLS-1$
        sb.append(time[1]).append(getString("LogPane.Milliseconds")); //$NON-NLS-1$
        post(sb.toString());
    }

    @Override
    public void clear() {
        try {
            editor.getDocument().remove(0, editor.getDocument().getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public JComponent getGUIComponent() {
        return pane;
    }

    @Override
    public boolean isEmpty() {
        return editor.getDocument().getLength() < 1;
    }

}
