package pulse.ui.frames;

import static java.awt.BorderLayout.SOUTH;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;

import pulse.search.statistics.ResidualStatistic;
import pulse.tasks.TaskManager;
import pulse.ui.components.AuxPlotter;
import pulse.ui.components.ResidualsChart;

@SuppressWarnings("serial")
public class HistogramFrame extends ExternalGraphFrame<ResidualStatistic> {

	public HistogramFrame(AuxPlotter<ResidualStatistic> chart, int width, int height) {
		super("Residuals PDF", chart, width, height);
		this.getChart().getChartPanel().setBorder(BorderFactory.createRaisedSoftBevelBorder());
		var slider = new JSlider(8, 100, 20);
		var panel = new JPanel();
		var info = new JLabel("Number of bins: " + ((ResidualsChart)chart).getBinCount());
		panel.add(info);
		panel.add(new JSeparator());
		panel.add(slider);
		panel.setBorder(BorderFactory.createRaisedSoftBevelBorder());
		getContentPane().add(panel, SOUTH);
		slider.addChangeListener(e -> {
			((ResidualsChart)chart).setBinCount(slider.getValue());
			plot(TaskManager.getManagerInstance().getSelectedTask().getResidualStatistic() );
			info.setText("Number of bins: " + slider.getValue());
		});
	}

}