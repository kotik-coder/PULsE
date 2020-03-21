package pulse.ui.frames.dialogs;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

@SuppressWarnings("serial")
public class ProgressDialog extends JDialog implements PropertyChangeListener {

	private JProgressBar progressBar;
	private int progress;
	
	public ProgressDialog() {
		super();
		initComponents();
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setTitle("Please wait...");
		setPreferredSize(new Dimension(400, 75));
		pack();        
	}
	
    /**
     * Invoked when task's progress property changes.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            int progress = (Integer) evt.getNewValue();
            progressBar.setValue(progress);
        } 
    }
 
	private void initComponents() {
		progressBar = new JProgressBar(SwingConstants.HORIZONTAL);
		progressBar.setMinimum(0);
		progressBar.setStringPainted(true);
		getContentPane().add(progressBar);
	}

	public void incrementProgress() { 
		progress++;
	}
	
	private boolean reachedCapacity() {
		return progress >= progressBar.getMaximum();
	}
	
	public void trackProgress(int maximum) {
		progressBar.setMaximum(maximum);
		setVisible(true);
    	progress = 0;

	    var progressWorker = new SwingWorker<Void,Void>() {

	        @Override
	        protected Void doInBackground() {
	        	setProgress(0);
	        	while(!reachedCapacity()) { 
	                try {
	                    Thread.sleep(50);
	                } catch (InterruptedException ignore) {}
	                setProgress(progress);
	            }
	            return null;
	        		            
	        }
	        
	        @Override
	        protected void done() {
	            setVisible(false);     
	        }
	        
	    };
	    
	    progressWorker.addPropertyChangeListener(this);
    	progressWorker.execute();
	}
	
}