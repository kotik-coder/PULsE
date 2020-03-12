package pulse.ui.components.panels;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import pulse.ui.Launcher;

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
		
		cpuLabel.setHorizontalAlignment(SwingConstants.LEFT);
        cpuLabel.setText("CPU:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.weightx = 2.5;
        add(cpuLabel, gridBagConstraints);

        memoryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        memoryLabel.setText("Memory:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.weightx = 2.5;
        add(memoryLabel, gridBagConstraints);

        coresLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        coresLabel.setText("{n cores} ");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.weightx = 2.5;
        add(coresLabel, gridBagConstraints);
	}
	
	private void startSystemMonitors() {		
		String coresAvailable = String.format("{" + (Launcher.threadsAvailable()+1) + " cores}");
		coresLabel.setText(coresAvailable);
		
		ScheduledExecutorService executor =
			    Executors.newSingleThreadScheduledExecutor();

			Runnable periodicTask = new Runnable() {
			    @Override
				public void run() {
			    	double cpuUsage = Launcher.cpuUsage();
			    	double memoryUsage = Launcher.getMemoryUsage();			    
			    	
					String cpuString = String.format("CPU usage: %3.1f%%", cpuUsage);
					cpuLabel.setText(cpuString);
					String memoryString = String.format("Memory usage: %3.1f%%", memoryUsage);
					memoryLabel.setText(memoryString);
			        
			        if(cpuUsage > 75)
			        	cpuLabel.setForeground(Color.red);
			        else if(cpuUsage > 50) 			        	
			        	cpuLabel.setForeground(Color.yellow);
			        else
			        	cpuLabel.setForeground(Color.black);
			        
			        
			        /*
			         * 
			         */
			        
			        if(memoryUsage > 75)
			        	memoryLabel.setForeground(Color.red);
			        else if(memoryUsage > 50) 			        	
			        	memoryLabel.setForeground(Color.yellow);
			        else
			        	memoryLabel.setForeground(Color.black);
			        
			    }
			};
			
		executor.scheduleAtFixedRate(periodicTask, 0, 2, TimeUnit.SECONDS);
	}

}