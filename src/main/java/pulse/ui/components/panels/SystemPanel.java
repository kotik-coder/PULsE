package pulse.ui.components.panels;

import static java.awt.Color.red;
import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.swing.SwingConstants.CENTER;
import static javax.swing.SwingConstants.LEFT;
import static javax.swing.SwingConstants.RIGHT;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import pulse.util.ImageUtils;
import pulse.util.ResourceMonitor;

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
		var monitor = ResourceMonitor.getInstance();
				
		var coresAvailable = format("{" + (monitor.getThreadsAvailable() + 1) + " cores}");
		coresLabel.setText(coresAvailable);

		var executor = newSingleThreadScheduledExecutor();
		var defColor = UIManager.getColor("Label.foreground");
		
		Runnable periodicTask = () -> {
			monitor.update();
			var cpuString = format("CPU usage: %3.1f%%", monitor.getCpuUsage());
			cpuLabel.setText(cpuString);
			var memoryString = format("Memory usage: %3.1f%%", monitor.getMemoryUsage());
			memoryLabel.setText(memoryString);
			
			cpuLabel.setForeground(ImageUtils.blend(defColor, red, (float)monitor.getCpuUsage()/100));
			memoryLabel.setForeground(ImageUtils.blend(defColor, red, (float)monitor.getMemoryUsage()/100));

		};

		executor.scheduleAtFixedRate(periodicTask, 0, 2, SECONDS);
	}
	
}