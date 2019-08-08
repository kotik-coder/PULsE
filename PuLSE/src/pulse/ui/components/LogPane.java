package pulse.ui.components;

import java.awt.Dimension;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import pulse.util.Saveable;

public class LogPane extends JEditorPane implements Saveable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8464602356332804481L;
	private ExecutorService updateExecutor = Executors.newSingleThreadExecutor();	

	public LogPane() {
		super();	
		this.setPreferredSize(new Dimension(500, 500));
		setContentType("text/html"); //$NON-NLS-1$
		setEditable(false);
		DefaultCaret c = (DefaultCaret)getCaret();
		c.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);	
	}		
	
	private void post(LogEntry logEntry) {
		post(logEntry.toString());
	}

	private void post(String text) {
		
		final HTMLDocument doc	= (HTMLDocument) getDocument();
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
		long ms = ChronoUnit.MILLIS.between(log.getStart(), log.getEnd()) - 1000L*seconds;
		
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.getString("LogPane.TimeTaken")); //$NON-NLS-1$
		sb.append(seconds + Messages.getString("LogPane.Seconds")); //$NON-NLS-1$
		sb.append(ms + Messages.getString("LogPane.Milliseconds")); //$NON-NLS-1$
		post(sb.toString());
	}
	
	public void callUpdate() {
		updateExecutor.submit(() -> update());							
	}
	
	public void callPrintAll() {
		try {
			updateExecutor.awaitTermination(10, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		printAll();
	}
	
	private void printAll() {
		clear();
		
		SearchTask task = TaskManager.getSelectedTask();
		
		if(task == null)
			return;
					
		Log log = task.getLog();
		
		if(!log.isStarted()) 
			return;											
		
		log.getLogEntries().stream().forEach(entry -> post(entry));
		
		if(task.getStatus() == Status.DONE)
			printTimeTaken(log);
			
	}
	
	private void update() {				
		SearchTask task = TaskManager.getSelectedTask();
		
		if(task == null)
			return;
					
		Log log = task.getLog();
		
		if(!log.isStarted()) 
			return;											
		
		post( log.lastEntry() );
	}
	
	public void clear() {		
		try {
			getDocument().remove(0, getDocument().getLength());
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
	
	@Override 
	public void printData(FileOutputStream fos) {
		HTMLEditorKit kit = (HTMLEditorKit) this.getEditorKit();
		try {
			kit.write(fos, this.getDocument(), 0, this.getDocument().getLength());
		} catch (IOException | BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ExecutorService getUpdateExecutor() {
		return updateExecutor;
	}
	
}
