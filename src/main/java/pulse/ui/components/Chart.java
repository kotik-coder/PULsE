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
import java.awt.event.MouseEvent;
import java.io.Serializable;
import javax.swing.SwingUtilities;

import javax.swing.UIManager;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.block.BlockBorder;
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
import pulse.input.Range;
import pulse.input.listeners.DataEvent;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.NumericProperties;
import static pulse.properties.NumericPropertyKeyword.LOWER_BOUND;
import static pulse.properties.NumericPropertyKeyword.UPPER_BOUND;
import pulse.tasks.Calculation;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.ui.components.listeners.MouseOnMarkerListener;

public class Chart implements Serializable {

    private final ChartPanel chartPanel;
    private final JFreeChart chart;
    private XYPlot plot;

    private float opacity = 0.15f;
    private boolean residualsShown = true;
    private boolean zeroApproximationShown = false;

    private final static double TO_MILLIS = 1E3;
    private final static double RANGE_THRESHOLD = 1E-1;
    private double factor;

    private MovableValueMarker lowerMarker;
    private MovableValueMarker upperMarker;

    public Chart() {
        chart = createScatterPlot("", getString("Charting.TimeAxisLabel"), (getString("Charting.TemperatureAxisLabel")),
                null, VERTICAL, true, true, false);

        plot = chart.getXYPlot();
        setRenderers();
        setBackgroundAndGrid();
        setLegendTitle();
        setFonts();

        final TaskManager instance = TaskManager.getManagerInstance();

        chart.removeLegend();
        chart.setBackgroundPaint(UIManager.getColor("TextPane.background"));
        chartPanel = new ChartPanel(chart) {

            @Override
            public void mouseDragged(MouseEvent e) {
                if (lowerMarker == null || upperMarker == null) {
                    super.mouseDragged(e);
                }

                SwingUtilities.invokeLater(() -> {

                    //process dragged events        
                    Range range = ((ExperimentalData) (instance.getSelectedTask().getInput())).getRange();
                    double value = xCoord(e) / factor;  //convert to seconds back from ms -- if needed

                    if (lowerMarker.getState() != MovableValueMarker.State.IDLE) {
                        if (range.boundLimits(false).contains(value)) {
                            range.setLowerBound(NumericProperties.derive(LOWER_BOUND, value));
                        }
                    } else if (upperMarker.getState() != MovableValueMarker.State.IDLE) {
                        if (range.boundLimits(true).contains(value)) {
                            range.setUpperBound(NumericProperties.derive(UPPER_BOUND, value));
                        }
                    } else {
                        super.mouseDragged(e);
                    }

                });

            }

        };

        instance.addTaskRepositoryListener((TaskRepositoryEvent e) -> {
            //for each new task
            var eventTask = instance.getTask(e.getId());
            if (e.getState() == TaskRepositoryEvent.State.TASK_ADDED) {
                var data = (ExperimentalData) eventTask.getInput();
                //add passive data listener
                data.addDataListener((DataEvent e1) -> {
                    //that will be triggered only when this task is selected
                    if (instance.getSelectedTask() == eventTask) {
                        //update marker values
                        var segment = data.getRange().getSegment();
                        lowerMarker.setValue(segment.getMinimum() * factor); //convert to ms -- if needed
                        upperMarker.setValue(segment.getMaximum() * factor); //convert to ms -- if needed
                    }
                });
            } //tasks that have been finihed
            else if (e.getState() == TaskRepositoryEvent.State.TASK_FINISHED
                    && instance.getSelectedTask() == eventTask) {
                //add passive data listener
                plot(eventTask, false);
            }
        });

    }

    public double xCoord(MouseEvent e) {
        double xVirtual = e.getX();
        return plot.getDomainAxis().java2DToValue(xVirtual,
                chartPanel.getScreenDataArea(), plot.getDomainAxisEdge());
    }

    private void setFonts() {
        var foreColor = UIManager.getColor("Label.foreground");
        setAxisFontColor(plot.getDomainAxis(), foreColor);
        setAxisFontColor(plot.getRangeAxis(), foreColor);
    }

    public static void setAxisFontColor(Axis axis, Color color) {
        axis.setLabelPaint(color);
        axis.setTickLabelPaint(color);
    }

    private void setBackgroundAndGrid() {
        plot.setBackgroundPaint(UIManager.getColor("TextPane.background"));
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
        rendererOld.setSeriesStroke(0, new BasicStroke(2.0f, CAP_BUTT, JOIN_MITER, 2.0f, new float[]{10f}, 0));
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

        for (int i = 0; i < 6; i++) {
            plot.setDataset(i, null);
        }

        var rawData = (ExperimentalData) task.getInput();
        var segment = rawData.getRange().getSegment();

        adjustAxisLabel(segment.getMaximum());

        factor = segment.getMaximum() < RANGE_THRESHOLD ? TO_MILLIS : 1.0;

        var rawDataset = new XYSeriesCollection();

        rawDataset.addSeries(series(rawData, "Raw data (" + task.getIdentifier() + ")", extendedCurve));
        plot.setDataset(3, rawDataset);
        plot.getRenderer(3).setSeriesPaint(0, new Color(1.0f, 0.0f, 0.0f, opacity));

        plot.clearDomainMarkers();

        lowerMarker = new MovableValueMarker(segment.getMinimum() * factor);
        upperMarker = new MovableValueMarker(segment.getMaximum() * factor);

        final double margin = (lowerMarker.getValue() + upperMarker.getValue()) / 20.0;

        //add listener to handle range adjustment
        var lowerMarkerListener = new MouseOnMarkerListener(this, lowerMarker, upperMarker, margin);
        var upperMarkerListener = new MouseOnMarkerListener(this, upperMarker, upperMarker, margin);

        chartPanel.addChartMouseListener(lowerMarkerListener);
        chartPanel.addChartMouseListener(upperMarkerListener);

        plot.addDomainMarker(upperMarker);
        plot.addDomainMarker(lowerMarker);

        var calc = (Calculation) task.getResponse();
        var problem = calc.getProblem();

        if (problem != null) {

            var solution = problem.getHeatingCurve();
            var scheme = calc.getScheme();

            if (solution != null && !solution.isFull()) {
                try {
                    calc.process();
                } catch (SolverException ex) {
                    System.out.println("Could not plot solution! See details in debug.");
                    ex.printStackTrace();
                }
            }

            if (solution != null && scheme != null) {

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

            if (p != null && s != null) {
                plotSingle(classicSolution(p, (double) (s.getTimeLimit().getValue())));
            }
        }

    }

    public void plotSingle(HeatingCurve curve) {
        requireNonNull(curve);

        plot = chart.getXYPlot();

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

        for (var i = 0; i < iStart && extendedCurve; i++) {
            series.add(factor * curve.timeAt(i), curve.signalAt(i));
        }

        for (var i = iStart; i < realCount; i++) {
            series.add(factor * curve.timeAt(i), curve.signalAt(i));
        }

        return series;
    }

    public XYSeries residuals(Calculation calc) {
        var problem = calc.getProblem();
        var baseline = problem.getBaseline();

        var time = calc.getOptimiserStatistic().getTimeSequence();
        var residuals = calc.getOptimiserStatistic().getResiduals();
        var size = residuals.size();

        final var span = problem.getHeatingCurve().maxAdjustedSignal() - baseline.valueAt(0);
        final var offset = baseline.valueAt(0) - span / 2.0;

        var series = new XYSeries(format("Residuals (offset %3.2f)", offset));

        for (var i = 0; i < size; i++) {
            series.add(factor * time.get(i), (Number) (residuals.get(i) + offset));
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

    public XYPlot getChartPlot() {
        return plot;
    }

}
