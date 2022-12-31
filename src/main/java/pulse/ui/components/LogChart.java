package pulse.ui.components;

import static java.util.Objects.requireNonNull;

import java.awt.BasicStroke;
import java.awt.Color;
import static java.awt.Color.WHITE;
import static java.awt.Color.black;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import pulse.Response;
import pulse.math.ParameterIdentifier;

import static pulse.properties.NumericPropertyKeyword.ITERATION;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.logs.DataLogEntry;
import pulse.tasks.logs.Log;
import pulse.tasks.logs.Status;
import pulse.tasks.processing.Buffer;
import pulse.ui.ColorGenerator;

public class LogChart extends AuxPlotter<Log> {

    private final Map<ParameterIdentifier, XYPlot> plots;
    private Color[] colors;
    private static final ColorGenerator cg = new ColorGenerator();
    private Response r;

    public LogChart() {
        var plot = new CombinedDomainXYPlot(new NumberAxis("Iteration"));
        plot.setGap(10.0);
        plot.setOrientation(PlotOrientation.VERTICAL);
        var chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        setChart(chart);
        plots = new HashMap<>();
        getChart().removeLegend();
    }

    public final void clear() {
        var p = (CombinedDomainXYPlot) getPlot();
        p.getDomainAxis().setAutoRange(true);
        if (p != null) {
            plots.values().stream().forEach(pp -> p.remove(pp));
        }
        plots.clear();
        colors = new Color[0];
        r = null;
    }

    private void setLegendTitle(Plot plot) {
        var lt = new LegendTitle(plot);
        lt.setBackgroundPaint(new Color(200, 200, 255, 100));
        lt.setFrame(new BlockBorder(black));
        lt.setPosition(RectangleEdge.RIGHT);
        var ta = new XYTitleAnnotation(0.0, 0.8, lt, RectangleAnchor.LEFT);
        ta.setMaxWidth(0.58);
        ((XYPlot) plot).addAnnotation(ta);
    }

    public final void add(ParameterIdentifier key, int no) {
        var plot = new XYPlot();
        var axis = new NumberAxis();
        axis.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(axis);

        plot.setBackgroundPaint(getChart().getBackgroundPaint());

        plots.put(key, plot);
        ((CombinedDomainXYPlot) getPlot()).add(plot);

        var dataset = new XYSeriesCollection();
        var series = new XYSeries(key.toString());

        dataset.addSeries(series);
        dataset.addSeries(new XYSeries("Running average"));
        plot.setDataset(dataset);
        setLegendTitle(plot);

        setRenderer(plot, colors[no]);
        setFonts();
    }

    private void setRenderer(XYPlot plt, Color clr) {
        var renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, clr);
        renderer.setSeriesPaint(1, WHITE);
        var dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f);
        renderer.setSeriesStroke(1, dashed);
        renderer.setSeriesVisibleInLegend(1, Boolean.FALSE);
        plt.setRenderer(renderer);
    }

    public void changeAxis(boolean iterationMode) {
        var domainAxis = (NumberAxis) getPlot().getDomainAxis();
        domainAxis.setLabel(iterationMode ? "Iteration" : "Time (ms)");
        domainAxis.setAutoRange(!iterationMode);
        if(iterationMode) {
            domainAxis.setTickUnit(new NumberTickUnit(1));
        } else {
            domainAxis.setAutoTickUnitSelection(true);
        }
    }

    @Override
    public void plot(Log l) {
        requireNonNull(l);

        l.getLogEntries().stream()
                .filter(le -> le instanceof DataLogEntry)
                .forEach(d -> plot((DataLogEntry) d,
                Duration.between(l.getStart(), d.getTime()).toMillis()));
    }

    private static void adjustRange(XYPlot pl, int iteration, int bufSize) {
        int lower = (iteration / bufSize) * bufSize;

        var domainAxis = pl.getDomainAxis();
        var r = domainAxis.getRange();
        var newR = new Range(lower, lower + bufSize);
        
        if (!r.equals(newR) && iteration > lower) {
            ((XYPlot) pl).getDomainAxis().setRange(lower, lower + bufSize);
        }
    }

    public final void plot(DataLogEntry dle, double iterationOrTime) {
        requireNonNull(dle);

        var data = dle.getData();
        int size = data.size();

        if (colors == null || colors.length < size) {
            colors = cg.random(size - 1);
        }

        SearchTask task = TaskManager.getManagerInstance().getTask(dle.getIdentifier());
        Buffer buf = task.getBuffer();
        final int bufSize = buf.getData().length;

        for (int i = 0, j = 0; i < size; i++) {
            var p = data.get(i);
            var np = p.getIdentifier();

            if (np.getKeyword() == ITERATION) {
                continue;
            }

            double value = p.getApparentValue();

            if (!plots.containsKey(np)) {
                add(np, j++);
            }

            Plot pl = plots.get(np);

            var dataset = (XYSeriesCollection) ((XYPlot) pl).getDataset();
            XYSeries series = (XYSeries) dataset.getSeries(0);
            series.add(iterationOrTime, value);

            if (task.getStatus() == Status.IN_PROGRESS) {

                XYSeries runningAverage = dataset.getSeries(1);
                if (iterationOrTime > buf.getData().length - 1) {
                    runningAverage.add(iterationOrTime, buf.average(np.getKeyword()));
                }

                SwingUtilities.invokeLater(() -> adjustRange((XYPlot)pl, (int)iterationOrTime, bufSize));

            } else {
                var domainAxis = ((XYPlot) pl).getDomainAxis();
                domainAxis.setAutoRange(true);
            }

        }

    }

}