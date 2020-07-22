package pulse.ui.components;

import static java.awt.Color.RED;
import static java.awt.Color.black;
import static java.awt.Color.white;
import static java.awt.Font.PLAIN;
import static java.util.Objects.requireNonNull;
import static org.jfree.chart.plot.PlotOrientation.VERTICAL;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import pulse.problem.statements.Problem;

public class AuxChart {

	private ChartPanel chartPanel;
	private JFreeChart chart;
	private XYPlot plot;
	
	private final static int NUM_PULSE_POINTS = 100;

	private final static double TO_MILLIS = 1E3;
	
	public AuxChart() {
		chart = ChartFactory.createScatterPlot("", "Time (ms)", "Laser Power (a. u.)",
				null, VERTICAL, true, true, false);

		plot = chart.getXYPlot();
		plot.setBackgroundPaint(white);
		setFonts();
		setRenderer();
		setLegendTitle();

		chart.removeLegend();
		chartPanel = new ChartPanel(chart);
	}
	
	private void setRenderer() {
		var rendererPulse = new XYDifferenceRenderer(new Color(0.0f, 0.2f, 0.8f, 0.1f), Color.red, false);
		rendererPulse.setSeriesPaint(0, RED);
		rendererPulse.setSeriesStroke(0, new BasicStroke(3.0f));
		plot.setRenderer(rendererPulse);
	}
	
	private void setFonts() {
		var fontLabel = new Font("Arial", Font.PLAIN, 20);
		var fontTicks = new Font("Arial", Font.PLAIN, 16);
		plot.getDomainAxis().setLabelFont( fontLabel );
		plot.getDomainAxis().setTickLabelFont( fontTicks );
		plot.getRangeAxis().setLabelFont( fontLabel );
		plot.getRangeAxis().setTickLabelFont( fontTicks );
	}

	private void setLegendTitle() {
		var lt = new LegendTitle(plot);
		lt.setItemFont(new Font("Dialog", PLAIN, 16));
		lt.setBackgroundPaint(new Color(200, 200, 255, 100));
		lt.setFrame(new BlockBorder(black));
		lt.setPosition(RectangleEdge.RIGHT);
		var ta = new XYTitleAnnotation(0.5, 0.2, lt, RectangleAnchor.CENTER);
		ta.setMaxWidth(0.58);
		plot.addAnnotation(ta);
	}
	
	public void plot(Problem problem) {
		requireNonNull(problem);

		double startTime = (double)problem.getHeatingCurve().getTimeShift().getValue();

		var pulseDataset = new XYSeriesCollection();

		pulseDataset.addSeries(series(problem, startTime ));

		plot.setDataset(0, pulseDataset);
	}

	private static XYSeries series(Problem problem, double startTime) {
		var pulse = problem.getPulse();
		var shape = pulse.getPulseShape();

		var series = new XYSeries(pulse.getPulseShape().toString());

		double timeLimit = shape.getPulseWidth();
		double dx = timeLimit / (NUM_PULSE_POINTS - 1);
		double x = startTime;
		
		final double timeFactor = problem.timeFactor() * TO_MILLIS;
		
		series.add( (startTime - dx/10. ) * timeFactor, 0.0);
		series.add( (timeLimit + dx/10. ) * timeFactor, 0.0);
		
		for (var i = 0; i < NUM_PULSE_POINTS; i++) {
			series.add(x * timeFactor, shape.evaluateAt(x));
			x += dx;
		}
			
		return series;
	}

	public ChartPanel getChartPanel() {
		return chartPanel;
	}
	
}