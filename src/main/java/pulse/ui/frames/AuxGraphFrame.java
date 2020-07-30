package pulse.ui.frames;

import static java.awt.BorderLayout.CENTER;
import static pulse.tasks.TaskManager.getSelectedTask;

import javax.swing.JInternalFrame;

import pulse.ui.components.AuxChart;

@SuppressWarnings("serial")
public class AuxGraphFrame extends JInternalFrame {

	private static AuxChart chart;
	private static AuxGraphFrame instance = new AuxGraphFrame();

	private AuxGraphFrame() {
		super("Laser Pulse", true, false, true, true);
		initComponents();
		setVisible(true);
	}

	private void initComponents() {
		chart = new AuxChart();
		var chartPanel = chart.getChartPanel();
		getContentPane().add(chartPanel, CENTER);

		chartPanel.setMaximumDrawHeight(2000);
		chartPanel.setMaximumDrawWidth(2000);
		chartPanel.setMinimumDrawWidth(10);
		chartPanel.setMinimumDrawHeight(10);
	}

	public void plot() {
		var task = getSelectedTask();
		if (task != null)
			chart.plot(task.getProblem());
	}

	public static AuxChart getChart() {
		return chart;
	}

	public static AuxGraphFrame getInstance() {
		return instance;
	}

}