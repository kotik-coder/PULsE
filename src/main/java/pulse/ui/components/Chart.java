package pulse.ui.components;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.GRAY;
import static java.awt.Color.GREEN;
import static java.awt.Color.black;
import static java.awt.Color.white;
import static java.awt.Font.PLAIN;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.jfree.chart.ChartFactory.createScatterPlot;
import static org.jfree.chart.plot.PlotOrientation.VERTICAL;
import static pulse.HeatingCurve.classicSolution;
import static pulse.ui.Messages.getString;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import pulse.HeatingCurve;
import pulse.input.IndexRange;
import pulse.tasks.SearchTask;

public class Chart {

	private static JFreeChart chart;
	private static float opacity = 0.15f;
	private static boolean residualsShown = true;
	private static boolean zeroApproximationShown = false;

	public static ChartPanel createEmptyPanel() {
		chart = createScatterPlot("", getString("Charting.TimeAxisLabel"),
				(getString("Charting.TemperatureAxisLabel")), null, VERTICAL, true, true,
				false);

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
		rendererOld.setSeriesStroke(0,
				new BasicStroke(2.0f, CAP_BUTT, JOIN_MITER, 2.0f, new float[] { 10f }, 0));
		rendererOld.setSeriesShapesVisible(0, false);

		var plot = chart.getXYPlot();

		plot.setRenderer(0, renderer);
		plot.setRenderer(1, rendererLines);
		plot.setRenderer(2, rendererOld);
		plot.setRenderer(3, rendererResiduals);
		plot.setRenderer(4, rendererClassic);
		plot.setBackgroundPaint(white);

		plot.setRangeGridlinesVisible(true);
		plot.setRangeGridlinePaint(GRAY);

		plot.setDomainGridlinesVisible(true);
		plot.setDomainGridlinePaint(GRAY);

		var lt = new LegendTitle(plot);
		lt.setItemFont(new Font("Dialog", PLAIN, 14));
		lt.setBackgroundPaint(new Color(200, 200, 255, 100));
		lt.setFrame(new BlockBorder(black));
		lt.setPosition(RectangleEdge.RIGHT);
		var ta = new XYTitleAnnotation(0.98, 0.2, lt, RectangleAnchor.RIGHT);
		ta.setMaxWidth(0.58);
		plot.addAnnotation(ta);

		chart.removeLegend();
		return new ChartPanel(chart);
	}

	public static void plot(SearchTask task, boolean extendedCurve) {
		requireNonNull(task);

		var plot = chart.getXYPlot();

		plot.setDataset(0, null);
		plot.setDataset(1, null);
		plot.setDataset(2, null);
		plot.setDataset(3, null);
		plot.setDataset(4, null);

		var rawData = task.getExperimentalCurve();

		if (rawData == null)
			return;

		var rawDataset = new XYSeriesCollection();

		rawDataset.addSeries(series(rawData, "Raw data (" + task.getIdentifier() + ")", extendedCurve));
		plot.setDataset(0, rawDataset);

		plot.clearDomainMarkers();

		var lowerMarker = new ValueMarker(rawData.getRange().getSegment().getMinimum());

		Stroke dashed = new BasicStroke(1.5f, CAP_BUTT, JOIN_MITER, 5.0f, new float[] { 10f },
				0.0f);

		lowerMarker.setPaint(black);
		lowerMarker.setStroke(dashed);

		var upperMarker = new ValueMarker(rawData.getRange().getSegment().getMaximum());
		upperMarker.setPaint(black);
		upperMarker.setStroke(dashed);

		plot.addDomainMarker(upperMarker);
		plot.addDomainMarker(lowerMarker);

		if (task.getProblem() != null) {

			var solution = task.getProblem().getHeatingCurve();

			if (solution != null && task.getScheme() != null) {

				if (solution.adjustedSize() > 0) {

					var solutionDataset = new XYSeriesCollection();

					solutionDataset.addSeries(series(solution.extendedTo(rawData),
							"Solution with " + task.getScheme().getSimpleName(), extendedCurve));
					plot.setDataset(1, solutionDataset);

					/*
					 * plot residuals
					 */

					if (residualsShown)
						if (task.getResidualStatistic().getResiduals() != null) {
							var residualsDataset = new XYSeriesCollection();
							residualsDataset.addSeries(residuals(task));
							plot.setDataset(3, residualsDataset);
						}

				}

			}

			plot.getRenderer().setSeriesPaint(0, new Color(1.0f, 0.0f, 0.0f, opacity));

		}

		if (zeroApproximationShown) {
			var p = task.getProblem();
			var s = task.getScheme();

			if (p != null && s != null)
				plotSingle(classicSolution(p, (double) (s.getTimeLimit().getValue())));
		}

	}

	public static void plotSingle(HeatingCurve curve) {
		requireNonNull(curve);

		var plot = chart.getXYPlot();

		var classicDataset = new XYSeriesCollection();

		classicDataset.addSeries(series(curve, curve.getName(), false));

		plot.setDataset(4, classicDataset);
		plot.getRenderer(4).setSeriesPaint(0, black);
	}

	public static XYSeries series(HeatingCurve curve, String title, boolean extendedCurve) {
		var series = new XYSeries(title);

		var realCount = curve.adjustedSize();
		var startTime = (double) curve.getTimeShift().getValue();
		int iStart = IndexRange.closest(startTime, curve.getTimeSequence());

		for (var i = 0; i < iStart && extendedCurve; i++) 
			series.add( curve.timeAt(i), curve.signalAt(i));
		
		for (var i = iStart; i < realCount; i++)
			series.add( curve.timeAt(i), curve.signalAt(i));

		return series;
	}

	public static XYSeries residuals(SearchTask task) {
		var solution = task.getProblem().getHeatingCurve();
		final var span = solution.maxAdjustedSignal() - solution.getBaseline().valueAt(0);
		final var offset = solution.getBaseline().valueAt(0) - span / 2.0;

		var series = new XYSeries(format("Residuals (offset %3.2f)", offset));

		var residuals = task.getResidualStatistic().getResiduals();
		var size = residuals.size();

		for (var i = 0; i < size; i++) {
                    series.add(residuals.get(i)[0], (Number) (residuals.get(i)[1] + offset));
                }

		return series;
	}

	public static void setOpacity(float opacity) {
		Chart.opacity = opacity;
	}

	public static double getOpacity() {
		return opacity;
	}

	public static boolean isResidualsShown() {
		return residualsShown;
	}

	public static void setResidualsShown(boolean residualsShown) {
		Chart.residualsShown = residualsShown;
	}

	public static boolean isZeroApproximationShown() {
		return zeroApproximationShown;
	}

	public static void setZeroApproximationShown(boolean zeroApproximationShown) {
		Chart.zeroApproximationShown = zeroApproximationShown;
	}

    private Chart() {
    }

}