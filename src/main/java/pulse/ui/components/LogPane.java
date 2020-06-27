package pulse.ui.components;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import pulse.tasks.Log;
import pulse.tasks.LogEntry;
import pulse.tasks.SearchTask;
import pulse.tasks.Status;
import pulse.tasks.TaskManager;
import pulse.ui.Messages;
import pulse.util.Descriptive;

@SuppressWarnings("serial")
public class LogPane extends JEditorPane implements Descriptive {

	private ExecutorService updateExecutor = Executors.newSingleThreadExecutor();

	private final static boolean DEBUG = false;

	private PrintStream outStream, errStream;

	public LogPane() {
		super();
		setContentType("text/html");
		setEditable(false);
		DefaultCaret c = (DefaultCaret) getCaret();
		c.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		OutputStream out = new OutputStream() {
			@Override
			public void write(final int b) throws IOException {
				postError(String.valueOf((char) b));
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
			System.setOut(outStream = new PrintStream(out, true));
			System.setErr(errStream = new PrintStream(out, true));
		}

	}

	private void post(LogEntry logEntry) {
		post(logEntry.toString());
	}

	private void postError(String text) {
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.getString("DataLogEntry.FontTagError"));
		sb.append(text);
		sb.append(Messages.getString("DataLogEntry.FontTagClose"));
		post(sb.toString());
	}

	private void post(String text) {

		final HTMLDocument doc = (HTMLDocument) getDocument();
		final HTMLEditorKit kit = (HTMLEditorKit) this.getEditorKit();
		try {
			kit.insertHTML(doc, doc.getLength(), text, 0, 0, null);
		} catch (BadLocationException e) {
			System.err.println(Messages.getString("LogPane.InsertError")); //$NON-NLS-1$
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println(Messages.getString("LogPane.PrintError")); //$NON-NLS-1$
			e.printStackTrace();
		}

	}

	public void printTimeTaken(Log log) {
		long seconds = ChronoUnit.SECONDS.between(log.getStart(), log.getEnd());
		long ms = ChronoUnit.MILLIS.between(log.getStart(), log.getEnd()) - 1000L * seconds;

		StringBuilder sb = new StringBuilder();
		sb.append(Messages.getString("LogPane.TimeTaken")); //$NON-NLS-1$
		sb.append(seconds + Messages.getString("LogPane.Seconds")); //$NON-NLS-1$
		sb.append(ms + Messages.getString("LogPane.Milliseconds")); //$NON-NLS-1$
		post(sb.toString());
	}

	public synchronized void callUpdate() {
		updateExecutor.submit(() -> update());
	}

	public void printAll() {
		clear();

		SearchTask task = TaskManager.getSelectedTask();

		if (task != null) {

			var log = task.getLog();

			if (log.isStarted()) {

				log.getLogEntries().stream().forEach(entry -> post(entry));

				if (task.getStatus() == Status.DONE)
					printTimeTaken(log);

			}

		}

	}

	private synchronized void update() {
		SearchTask task = TaskManager.getSelectedTask();

		if (task == null)
			return;

		Log log = task.getLog();

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
		return "Log_" + TaskManager.getSelectedTask().getIdentifier().getValue();
	}

}