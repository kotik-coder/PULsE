package pulse.ui.frames;

import java.awt.BorderLayout;

import javax.swing.JInternalFrame;

import pulse.tasks.TaskManager;
import pulse.ui.components.Chart;
import pulse.ui.components.panels.ChartToolbar;
import pulse.ui.components.panels.OpacitySlider;

@SuppressWarnings("serial")
public class GraphFrame extends JInternalFrame {

	public GraphFrame() {
		super("Time-temperature profile(s)", true, false, true, true);
		initComponents();
		setVisible(true);
	}

	private void initComponents() {
		var chart = Chart.createEmptyPanel();
		getContentPane().add(chart, BorderLayout.CENTER);

		chart.setMaximumDrawHeight(2000);
		chart.setMaximumDrawWidth(2000);
		chart.setMinimumDrawWidth(10);
		chart.setMinimumDrawHeight(10);

		var opacitySlider = new OpacitySlider();
		opacitySlider.addPlotRequestListener(() -> plot());
		getContentPane().add(opacitySlider, BorderLayout.LINE_END);
		var chartToolbar = new ChartToolbar();
		chartToolbar.addPlotRequestListener(() -> plot());
		getContentPane().add(chartToolbar, BorderLayout.PAGE_END);
	}

	public void plot() {
		var task = TaskManager.getSelectedTask();
		if (task != null)
			Chart.plot(task, false);
	}

}