package pulse.ui.components;

import static java.lang.String.valueOf;
import static java.lang.System.err;
import static java.lang.System.setErr;
import static java.lang.System.setOut;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static javax.swing.text.DefaultCaret.ALWAYS_UPDATE;
import static pulse.tasks.Status.DONE;
import static pulse.tasks.TaskManager.getSelectedTask;
import static pulse.ui.Messages.getString;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;

import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import pulse.tasks.Log;
import pulse.tasks.LogEntry;
import pulse.util.Descriptive;

@SuppressWarnings("serial")
public class LogPane extends JEditorPane implements Descriptive {

	private ExecutorService updateExecutor = newSingleThreadExecutor();

	private final static boolean DEBUG = true;

	private PrintStream outStream, errStream;

	public LogPane() {
		super();
		setContentType("text/html");
		setEditable(false);
		var c = (DefaultCaret) getCaret();
		c.setUpdatePolicy(ALWAYS_UPDATE);

		OutputStream out = new OutputStream() {
			@Override
			public void write(final int b) throws IOException {
				postError(valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				postError(new String(b, off, len));
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};

		if (!DEBUG) {
			setOut(outStream = new PrintStream(out, true));
			setErr(errStream = new PrintStream(out, true));
		}

	}

	private void post(LogEntry logEntry) {
		post(logEntry.toString());
	}

	private void postError(String text) {
		var sb = new StringBuilder();
		sb.append(getString("DataLogEntry.FontTagError"));
		sb.append(text);
		sb.append(getString("DataLogEntry.FontTagClose"));
		post(sb.toString());
	}

	private void post(String text) {

		final var doc = (HTMLDocument) getDocument();
		final var kit = (HTMLEditorKit) this.getEditorKit();
		try {
			kit.insertHTML(doc, doc.getLength(), text, 0, 0, null);
		} catch (BadLocationException e) {
			err.println(getString("LogPane.InsertError")); //$NON-NLS-1$
			e.printStackTrace();
		} catch (IOException e) {
			err.println(getString("LogPane.PrintError")); //$NON-NLS-1$
			e.printStackTrace();
		}

	}

	public void printTimeTaken(Log log) {
		var seconds = SECONDS.between(log.getStart(), log.getEnd());
		var ms = MILLIS.between(log.getStart(), log.getEnd()) - 1000L * seconds;
		var sb = new StringBuilder();
		sb.append(getString("LogPane.TimeTaken")); //$NON-NLS-1$
		sb.append(seconds + getString("LogPane.Seconds")); //$NON-NLS-1$
		sb.append(ms + getString("LogPane.Milliseconds")); //$NON-NLS-1$
		post(sb.toString());
	}

	public synchronized void callUpdate() {
		updateExecutor.submit(() -> update());
	}

	public void printAll() {
		clear();

		var task = getSelectedTask();

		if (task != null) {

			var log = task.getLog();

			if (log.isStarted()) {

				log.getLogEntries().stream().forEach(entry -> post(entry));

				if (task.getStatus() == DONE)
					printTimeTaken(log);

			}

		}

	}

	private synchronized void update() {
		var task = getSelectedTask();

		if (task == null)
			return;

		var log = task.getLog();

		if (!log.isStarted())
			return;

		post(log.lastEntry());
	}

	public void clear() {
		try {
			getDocument().remove(0, getDocument().getLength());
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void finalize() {
		outStream.close();
		errStream.close();
	}

	public ExecutorService getUpdateExecutor() {
		return updateExecutor;
	}

	@Override
	public String describe() {
		return "Log_" + getSelectedTask().getIdentifier().getValue();
	}

}