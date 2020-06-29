package pulse.ui.frames;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.LINE_END;
import static java.awt.BorderLayout.PAGE_END;
import static pulse.tasks.TaskManager.getSelectedTask;
import static pulse.ui.components.Chart.createEmptyPanel;

import javax.swing.JInternalFrame;

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
		var chart = createEmptyPanel();
		getContentPane().add(chart, CENTER);

		chart.setMaximumDrawHeight(2000);
		chart.setMaximumDrawWidth(2000);
		chart.setMinimumDrawWidth(10);
		chart.setMinimumDrawHeight(10);

		var opacitySlider = new OpacitySlider();
		opacitySlider.addPlotRequestListener(() -> plot());
		getContentPane().add(opacitySlider, LINE_END);
		var chartToolbar = new ChartToolbar();
		chartToolbar.addPlotRequestListener(() -> plot());
		getContentPane().add(chartToolbar, PAGE_END);
	}

	public void plot() {
		var task = getSelectedTask();
		if (task != null)
			Chart.plot(task, false);
	}

}