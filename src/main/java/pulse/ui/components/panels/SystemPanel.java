package pulse.ui.components.panels;

import static java.awt.Color.black;
import static java.awt.Color.red;
import static java.awt.Color.yellow;
import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.swing.SwingConstants.CENTER;
import static javax.swing.SwingConstants.LEFT;
import static javax.swing.SwingConstants.RIGHT;
import static pulse.ui.Launcher.cpuUsage;
import static pulse.ui.Launcher.getMemoryUsage;
import static pulse.ui.Launcher.threadsAvailable;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class SystemPanel extends JPanel {

	private JLabel coresLabel;
	private JLabel cpuLabel;
	private JLabel memoryLabel;

	public SystemPanel() {
		initComponents();
		startSystemMonitors();
	}

	private void initComponents() {
		coresLabel = new JLabel();
		cpuLabel = new JLabel();
		memoryLabel = new JLabel();

		setLayout(new GridBagLayout());
		var gridBagConstraints = new GridBagConstraints();

		cpuLabel.setHorizontalAlignment(LEFT);
		cpuLabel.setText("CPU:");
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.weightx = 2.5;
		add(cpuLabel, gridBagConstraints);

		memoryLabel.setHorizontalAlignment(CENTER);
		memoryLabel.setText("Memory:");
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.weightx = 2.5;
		add(memoryLabel, gridBagConstraints);

		coresLabel.setHorizontalAlignment(RIGHT);
		coresLabel.setText("{n cores} ");
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.weightx = 2.5;
		add(coresLabel, gridBagConstraints);
	}

	private void startSystemMonitors() {
		var coresAvailable = format("{" + (threadsAvailable() + 1) + " cores}");
		coresLabel.setText(coresAvailable);

		var executor = newSingleThreadScheduledExecutor();

		Runnable periodicTask = () -> {
            var cpuUsage = cpuUsage();
            var memoryUsage = getMemoryUsage();
            var cpuString = format("CPU usage: %3.1f%%", cpuUsage);
            cpuLabel.setText(cpuString);
            var memoryString = format("Memory usage: %3.1f%%", memoryUsage);
            memoryLabel.setText(memoryString);
            if (cpuUsage > 75) {
                cpuLabel.setForeground(red);
            } else if (cpuUsage > 50) {
                cpuLabel.setForeground(yellow);
            } else {
                cpuLabel.setForeground(black);
            }
            /*
             *
             */
            if (memoryUsage > 75) {
                memoryLabel.setForeground(red);
            } else if (memoryUsage > 50) {
                memoryLabel.setForeground(yellow);
            } else {
                memoryLabel.setForeground(black);
            }
        };

		executor.scheduleAtFixedRate(periodicTask, 0, 2, SECONDS);
	}

}