package pulse.ui.components;

import static java.util.Objects.requireNonNull;
import static org.jfree.chart.plot.PlotOrientation.VERTICAL;

import org.jfree.chart.ChartFactory;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import pulse.search.statistics.ResidualStatistic;

public class ResidualsChart extends AuxPlotter<ResidualStatistic> {
	
	private int binCount;

	public ResidualsChart(String xLabel, String yLabel) {
		super(xLabel, yLabel);
		binCount = 32;
	}
	
	@Override
	public void createChart(String xLabel, String yLabel) {
		setChart( ChartFactory.createHistogram("", xLabel, yLabel, null, VERTICAL, true, true, false) );
	}

	@Override
	public void plot(ResidualStatistic stat) {
		requireNonNull(stat);
		
		var pulseDataset = new HistogramDataset();
		pulseDataset.setType(HistogramType.RELATIVE_FREQUENCY);
		
		var residuals = stat.transformResiduals();
		
		if(residuals.length > 0)
			pulseDataset.addSeries("H1", stat.transformResiduals(), binCount);
		
		getPlot().setDataset(0, pulseDataset);
	}

	public int getBinCount() {
		return binCount;
	}

	public void setBinCount(int binCount) {
		this.binCount = binCount;
	}

}