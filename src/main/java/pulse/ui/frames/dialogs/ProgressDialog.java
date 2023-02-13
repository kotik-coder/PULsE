package pulse.ui.frames.dialogs;

import static java.lang.Thread.sleep;
import static javax.swing.SwingConstants.HORIZONTAL;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import pulse.util.Serializer;
import static pulse.util.Serializer.deserialize;

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

    public interface ProgressWorker {

        public default void work() {
            var dialog = new ProgressDialog();
            dialog.setLocationRelativeTo(null);
            dialog.trackProgress(1);
            CompletableFuture.runAsync(() -> {
                try {
                    action();
                } catch (Exception ex) {
                    Logger.getLogger(Serializer.class.getName()).log(Level.SEVERE, "Failed to load session", ex);
                    System.err.println("Failed to load session.");
                }
            })
                    .thenRun(() -> dialog.incrementProgress());
        }

        public void action();

    }

}