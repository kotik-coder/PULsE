package pulse.ui.components.panels;

import static java.awt.Color.red;
import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.swing.SwingConstants.CENTER;
import static javax.swing.SwingConstants.LEFT;
import static javax.swing.SwingConstants.RIGHT;
import static pulse.ui.Launcher.cpuUsage;
import static pulse.ui.Launcher.getMemoryUsage;
import static pulse.ui.Launcher.threadsAvailable;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

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
		var defColor = UIManager.getColor("Label.foreground");
		
		Runnable periodicTask = () -> {
			var cpuUsage = cpuUsage();
			var memoryUsage = getMemoryUsage();
			var cpuString = format("CPU usage: %3.1f%%", cpuUsage);
			cpuLabel.setText(cpuString);
			var memoryString = format("Memory usage: %3.1f%%", memoryUsage);
			memoryLabel.setText(memoryString);
			
			cpuLabel.setForeground(blend(defColor, red, (float)cpuUsage/100));
			memoryLabel.setForeground(blend(defColor, red, (float)memoryUsage/100));

		};

		executor.scheduleAtFixedRate(periodicTask, 0, 2, SECONDS);
	}
	
	private Color blend( Color c1, Color c2, float ratio ) {
	    if ( ratio > 1f ) ratio = 1f;
	    else if ( ratio < 0f ) ratio = 0f;
	    float iRatio = 1.0f - ratio;

	    int i1 = c1.getRGB();
	    int i2 = c2.getRGB();

	    int a1 = (i1 >> 24 & 0xff);
	    int r1 = ((i1 & 0xff0000) >> 16);
	    int g1 = ((i1 & 0xff00) >> 8);
	    int b1 = (i1 & 0xff);

	    int a2 = (i2 >> 24 & 0xff);
	    int r2 = ((i2 & 0xff0000) >> 16);
	    int g2 = ((i2 & 0xff00) >> 8);
	    int b2 = (i2 & 0xff);

	    int a = (int)((a1 * iRatio) + (a2 * ratio));
	    int r = (int)((r1 * iRatio) + (r2 * ratio));
	    int g = (int)((g1 * iRatio) + (g2 * ratio));
	    int b = (int)((b1 * iRatio) + (b2 * ratio));

	    return new Color( a << 24 | r << 16 | g << 8 | b );
	}

}