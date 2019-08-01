package pulse.ui.components;

import java.awt.Dimension;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import pulse.search.math.LogEntry;
import pulse.tasks.Log;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.util.Saveable;

public class LogPane extends JEditorPane implements Saveable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8464602356332804481L;
	
	private boolean updating = false;

	public LogPane() {
		super();	
		this.setPreferredSize(new Dimension(500, 500));
		setContentType("text/html"); //$NON-NLS-1$
		setEditable(false);
		DefaultCaret c = (DefaultCaret)getCaret();
		c.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);		
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
	
	private void printTimeTaken(Log log) {
		long seconds = ChronoUnit.SECONDS.between(log.getStart(), log.getEnd());
		long ms = ChronoUnit.MILLIS.between(log.getStart(), log.getEnd()) - 1000L*seconds;
		
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.getString("LogPane.TimeTaken")); //$NON-NLS-1$
		sb.append(seconds + Messages.getString("LogPane.Seconds")); //$NON-NLS-1$
		sb.append(ms + Messages.getString("LogPane.Milliseconds")); //$NON-NLS-1$
		post(sb.toString());
	}
	
	public void callUpdate() {
		final int TIME_LIMIT = 5; //seconds
		
		ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
		Future<?> future = threadExecutor.submit(new Runnable() {

			@Override
			public void run() {
				update();
			}
			
		});
		
		try {
			future.get(TIME_LIMIT, TimeUnit.SECONDS);
		} 
		catch (InterruptedException e) {
			System.err.println("Log update was interrupted:");
			e.printStackTrace();
		} catch (ExecutionException e) {
			System.err.println("Execution exception (too many calls). Forcing LogPane.callUpdate() to stop...");
			updating = false;			
		} catch (TimeoutException e) {
			System.err.println("Log update timeout");
			future.cancel(true);
			e.printStackTrace();
		}
		
	}
	
	private void update() {
		if(updating)
			return;
					
		clear();
		
		SearchTask task = TaskManager.getSelectedTask();
		Log log = task.getLog();
		
		if(!log.isStarted()) {
			updating = false;
			return;			
		}
		
		updating = true;
		
		post(Messages.getString("LogPane.Init")); //$NON-NLS-1$
		
		for(LogEntry le : log.getLogEntries())
			post(le.toString());
		
		if(!log.isFinished()) {
			updating = false;
			return;
		}
			
		printTimeTaken(log);
		updating = false;
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
	
	public boolean isUpdating() {
		return updating;
	}
	
}
