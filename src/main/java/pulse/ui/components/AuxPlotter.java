package pulse.ui.components;

import static java.awt.Color.white;

import java.awt.Font;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

public abstract class AuxPlotter<T> {

	private ChartPanel chartPanel;
	private JFreeChart chart;
	private XYPlot plot;
	
	public AuxPlotter(String xLabel, String yLabel) {
		createChart(xLabel, yLabel);

		plot = chart.getXYPlot();
		plot.setBackgroundPaint(white);
		setFonts();

		chart.removeLegend();
		chartPanel = new ChartPanel(chart);
	}
	
	public void setFonts() {
		var fontLabel = new Font("Arial", Font.PLAIN, 20);
		var fontTicks = new Font("Arial", Font.PLAIN, 16);
		var plot = getPlot();
		plot.getDomainAxis().setLabelFont(fontLabel);
		plot.getDomainAxis().setTickLabelFont(fontTicks);
		plot.getRangeAxis().setLabelFont(fontLabel);
		plot.getRangeAxis().setTickLabelFont(fontTicks);
	}
	
	public abstract void createChart(String xLabel, String yLabel);
	public abstract void plot(T t);

	public ChartPanel getChartPanel() {
		return chartPanel;
	}

	public JFreeChart getChart() {
		return chart;
	}

	public XYPlot getPlot() {
		return plot;
	}

	public void setChart(JFreeChart chart) {
		this.chart = chart;
	} 
	
}