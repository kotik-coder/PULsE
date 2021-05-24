package pulse.ui.components;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.GRAY;
import static java.awt.Color.GREEN;
import static java.awt.Color.black;
import static java.awt.Font.PLAIN;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.jfree.chart.ChartFactory.createScatterPlot;
import static org.jfree.chart.plot.PlotOrientation.VERTICAL;
import static pulse.problem.statements.AdiabaticSolution.classicSolution;
import static pulse.ui.Messages.getString;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;

import javax.swing.UIManager;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import pulse.AbstractData;
import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.input.IndexRange;
import pulse.tasks.Calculation;
import pulse.tasks.SearchTask;

public class Chart {

	private ChartPanel chartPanel;
	private JFreeChart chart;
	private XYPlot plot;

	private float opacity = 0.15f;
	private boolean residualsShown = true;
	private boolean zeroApproximationShown = false;

	private final static double TO_MILLIS = 1E3;
	private final static double RANGE_THRESHOLD = 1E-1;
	private double factor;

	public Chart() {
		chart = createScatterPlot("", getString("Charting.TimeAxisLabel"), (getString("Charting.TemperatureAxisLabel")),
				null, VERTICAL, true, true, false);

		plot = chart.getXYPlot();
		setRenderers();
		setBackgroundAndGrid();
		setLegendTitle();
		setFonts();

		chart.removeLegend();
		chart.setBackgroundPaint(UIManager.getColor("Panel.background"));
		chartPanel = new ChartPanel(chart);
	}

	private void setFonts() {
		var fontLabel = new Font("Arial", Font.PLAIN, 20);
		var fontTicks = new Font("Arial", Font.PLAIN, 14);
		plot.getDomainAxis().setLabelFont(fontLabel);
		plot.getDomainAxis().setTickLabelFont(fontTicks);
		plot.getRangeAxis().setLabelFont(fontLabel);
		plot.getRangeAxis().setTickLabelFont(fontTicks);
	}

	private void setBackgroundAndGrid() {
		// plot.setBackgroundPaint(UIManager.getColor("Panel.background"));
		plot.setRangeGridlinesVisible(true);
		plot.setRangeGridlinePaint(GRAY);

		plot.setDomainGridlinesVisible(true);
		plot.setDomainGridlinePaint(GRAY);
	}

	private void setLegendTitle() {
		var lt = new LegendTitle(plot);
		lt.setItemFont(new Font("Dialog", PLAIN, 14));
		lt.setBackgroundPaint(new Color(200, 200, 255, 100));
		lt.setFrame(new BlockBorder(black));
		lt.setPosition(RectangleEdge.RIGHT);
		var ta = new XYTitleAnnotation(0.98, 0.2, lt, RectangleAnchor.RIGHT);
		ta.setMaxWidth(0.58);
		plot.addAnnotation(ta);
	}

	private void setRenderers() {
		var renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
		renderer.setDefaultShapesFilled(false);
		renderer.setUseFillPaint(false);
		renderer.setSeriesPaint(0, new Color(1.0f, 0.0f, 0.0f, opacity));

		var rendererLines = new XYLineAndShapeRenderer();
		rendererLines.setSeriesPaint(0, BLUE);
		rendererLines.setSeriesStroke(0, new BasicStroke(2.0f));
		rendererLines.setSeriesShapesVisible(0, false);

		var rendererResiduals = new XYLineAndShapeRenderer();
		rendererResiduals.setSeriesPaint(0, GREEN);
		rendererResiduals.setSeriesShapesVisible(0, false);

		var rendererClassic = new XYLineAndShapeRenderer();
		rendererClassic.setSeriesPaint(0, BLACK);
		rendererClassic.setSeriesShapesVisible(0, false);

		var rendererOld = new XYLineAndShapeRenderer();
		rendererOld.setSeriesPaint(0, BLUE);
		rendererOld.setSeriesStroke(0, new BasicStroke(2.0f, CAP_BUTT, JOIN_MITER, 2.0f, new float[] { 10f }, 0));
		rendererOld.setSeriesShapesVisible(0, false);

		plot.setRenderer(0, rendererLines);
		plot.setRenderer(1, rendererResiduals);
		plot.setRenderer(2, rendererClassic);
		plot.setRenderer(3, renderer);

	}

	private void adjustAxisLabel(double maximum) {
		if (maximum < RANGE_THRESHOLD) {
			factor = TO_MILLIS;
			plot.getDomainAxis().setLabel("Time (ms)");
		} else {
			factor = 1.0;
			plot.getDomainAxis().setLabel("Time (s)");
		}
	}

	public void plot(SearchTask task, boolean extendedCurve) {
		requireNonNull(task);

		var plot = chart.getXYPlot();

		for (int i = 0; i < 6; i++)
			plot.setDataset(i, null);

		var rawData = task.getExperimentalCurve();
		var segment = rawData.getRange().getSegment();

		adjustAxisLabel(segment.getMaximum());

		factor = segment.getMaximum() < RANGE_THRESHOLD ? TO_MILLIS : 1.0;

		var rawDataset = new XYSeriesCollection();

		rawDataset.addSeries(series(rawData, "Raw data (" + task.getIdentifier() + ")", extendedCurve));
		plot.setDataset(3, rawDataset);
		plot.getRenderer(3).setSeriesPaint(0, new Color(1.0f, 0.0f, 0.0f, opacity));

		plot.clearDomainMarkers();

		var lowerMarker = new ValueMarker(segment.getMinimum() * factor);

		Stroke dashed = new BasicStroke(1.5f, CAP_BUTT, JOIN_MITER, 5.0f, new float[] { 10f }, 0.0f);

		lowerMarker.setPaint(black);
		lowerMarker.setStroke(dashed);

		var upperMarker = new ValueMarker(segment.getMaximum() * factor);
		upperMarker.setPaint(black);
		upperMarker.setStroke(dashed);

		plot.addDomainMarker(upperMarker);
		plot.addDomainMarker(lowerMarker);

		var calc = task.getCurrentCalculation();
		var problem = calc.getProblem();

		if (problem != null) {

			var solution = problem.getHeatingCurve();
			var scheme = calc.getScheme();

			if (solution != null && scheme != null && !solution.isIncomplete()) {

				var solutionDataset = new XYSeriesCollection();
				var displayedCurve = extendedCurve ? solution.extendedTo(rawData, problem.getBaseline()) : solution;

				solutionDataset
						.addSeries(series(displayedCurve, "Solution with " + scheme.getSimpleName(), extendedCurve));
				plot.setDataset(0, solutionDataset);

				/*
				 * plot residuals
				 */

				if (residualsShown) {
					var residuals = calc.getOptimiserStatistic().getResiduals();
					if (residuals != null && residuals.size() > 0) {
						var residualsDataset = new XYSeriesCollection();
						residualsDataset.addSeries(residuals(calc));
						plot.setDataset(1, residualsDataset);
					}
				}

			}

		}

		if (zeroApproximationShown) {
			var p = calc.getProblem();
			var s = calc.getScheme();

			if (p != null && s != null)
				plotSingle(classicSolution(p, (double) (s.getTimeLimit().getValue())));
		}

	}

	public void plotSingle(HeatingCurve curve) {
		requireNonNull(curve);

		var plot = chart.getXYPlot();

		var classicDataset = new XYSeriesCollection();

		classicDataset.addSeries(series(curve, curve.getName(), false));

		plot.setDataset(2, classicDataset);
		plot.getRenderer(2).setSeriesPaint(0, black);
	}

	public XYSeries series(HeatingCurve curve, String title, boolean extendedCurve) {
		final int realCount = curve.getBaselineCorrectedData().size();
		final double startTime = (double) ((HeatingCurve) curve).getTimeShift().getValue();
		return series(curve, title, startTime, realCount, extendedCurve);
	}

	public XYSeries series(ExperimentalData curve, String title, boolean extendedCurve) {
		return series(curve, title, 0, curve.actualNumPoints(), extendedCurve);
	}

	private XYSeries series(AbstractData curve, String title, final double startTime, final int realCount,
			boolean extendedCurve) {
		var series = new XYSeries(title);

		int iStart = IndexRange.closestLeft(startTime < 0 ? startTime : 0, curve.getTimeSequence());

		for (var i = 0; i < iStart && extendedCurve; i++)
			series.add(factor * curve.timeAt(i), curve.signalAt(i));

		for (var i = iStart; i < realCount; i++)
			series.add(factor * curve.timeAt(i), curve.signalAt(i));

		return series;
	}

	public XYSeries residuals(Calculation calc) {
		var problem = calc.getProblem();
		var baseline = problem.getBaseline();

		var residuals = calc.getOptimiserStatistic().getResiduals();
		var size = residuals.size();

		final var span = problem.getHeatingCurve().maxAdjustedSignal() - baseline.valueAt(0);
		final var offset = baseline.valueAt(0) - span / 2.0;

		var series = new XYSeries(format("Residuals (offset %3.2f)", offset));

		for (var i = 0; i < size; i++) {
			series.add(factor * residuals.get(i)[0], (Number) (residuals.get(i)[1] + offset));
		}

		return series;
	}

	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}

	public double getOpacity() {
		return opacity;
	}

	public boolean isResidualsShown() {
		return residualsShown;
	}

	public void setResidualsShown(boolean residualsShown) {
		this.residualsShown = residualsShown;
	}

	public boolean isZeroApproximationShown() {
		return zeroApproximationShown;
	}

	public void setZeroApproximationShown(boolean zeroApproximationShown) {
		this.zeroApproximationShown = zeroApproximationShown;
	}

	public ChartPanel getChartPanel() {
		return chartPanel;
	}

}