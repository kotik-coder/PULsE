package pulse.ui.components;

import static java.util.Objects.requireNonNull;
import org.jfree.chart.ChartFactory;
import static org.jfree.chart.plot.PlotOrientation.VERTICAL;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import pulse.search.statistics.ResidualStatistic;

public class ResidualsChart extends AuxPlotter<ResidualStatistic> {

    private int binCount;

    public ResidualsChart(String xLabel, String yLabel) {
        setChart(ChartFactory.createHistogram("", xLabel, yLabel, null, VERTICAL, true, true, false));
        setPlot(getChart().getXYPlot());
        getChart().removeLegend();
        setFonts();
        binCount = 32;
    }

    @Override
    public void plot(ResidualStatistic stat) {
        requireNonNull(stat);

        var pulseDataset = new HistogramDataset();
        pulseDataset.setType(HistogramType.RELATIVE_FREQUENCY);

        var residuals = stat.residualsArray();

        if (residuals.length > 0) {
            pulseDataset.addSeries("H1", residuals, binCount);
        }

        getPlot().setDataset(0, pulseDataset);
    }

    public int getBinCount() {
        return binCount;
    }

    public void setBinCount(int binCount) {
        this.binCount = binCount;
    }

}
