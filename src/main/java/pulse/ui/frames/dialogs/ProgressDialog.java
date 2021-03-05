package pulse.ui.frames.dialogs;

import static java.lang.Thread.sleep;
import static javax.swing.SwingConstants.HORIZONTAL;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

@SuppressWarnings("serial")
public class ProgressDialog extends JDialog implements PropertyChangeListener {

	private JProgressBar progressBar;
	private int progress;

	public ProgressDialog() {
		super();
		initComponents();
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setTitle("Please wait...");
		setPreferredSize(new Dimension(400, 75));
		pack();
	}

	/**
	 * Invoked when task's progress property changes.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("progress")) {
			int progress = (Integer) evt.getNewValue();
			progressBar.setValue(progress);
		}
	}

	private void initComponents() {
		progressBar = new JProgressBar(HORIZONTAL);
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

		var progressWorker = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() {
				setProgress(0);
				while (!reachedCapacity()) {
					try {
						sleep(50);
					} catch (InterruptedException ignore) {
					}
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