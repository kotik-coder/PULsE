package pulse.ui.components;

import static java.awt.Color.RED;
import static java.awt.Color.black;
import static java.awt.Font.PLAIN;
import static java.util.Objects.requireNonNull;
import static org.jfree.chart.plot.PlotOrientation.VERTICAL;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import pulse.problem.statements.Problem;
import pulse.problem.statements.Pulse;
import pulse.tasks.Calculation;

public class PulseChart extends AuxPlotter<Calculation> {

    private final static double TO_MILLIS = 1E3;

    public PulseChart(String xLabel, String yLabel) {
        super(xLabel, yLabel);
        setRenderer();
        setLegendTitle();
    }

    private void setRenderer() {
        var rendererPulse = new XYDifferenceRenderer(new Color(0.0f, 0.2f, 0.8f, 0.1f), Color.red, false);
        rendererPulse.setSeriesPaint(0, RED);
        rendererPulse.setSeriesStroke(0, new BasicStroke(3.0f));
        getPlot().setRenderer(rendererPulse);
    }

    private void setLegendTitle() {
        var plot = getPlot();
        var lt = new LegendTitle(plot);
        lt.setItemFont(new Font("Dialog", PLAIN, 16));
        lt.setBackgroundPaint(new Color(200, 200, 255, 100));
        lt.setFrame(new BlockBorder(black));
        lt.setPosition(RectangleEdge.RIGHT);
        var ta = new XYTitleAnnotation(0.5, 0.2, lt, RectangleAnchor.CENTER);
        ta.setMaxWidth(0.58);
        plot.addAnnotation(ta);
    }

    @Override
    public void plot(Calculation c) {
        requireNonNull(c);

        Problem problem = c.getProblem();

        double startTime = (double) problem.getHeatingCurve().getTimeShift().getValue();

        var pulseDataset = new XYSeriesCollection();
         
        pulseDataset.addSeries(series(problem.getPulse(), c.getScheme().getGrid().getTimeStep(), 
                problem.getProperties().timeFactor(), startTime));

        getPlot().setDataset(0, pulseDataset);
    }

    private static XYSeries series(Pulse pulse, double dx, double timeFactor, double startTime) {
        var series = new XYSeries(pulse.getPulseShape().toString());
        var pulseShape = pulse.getPulseShape();
        
        double timeLimit = pulseShape.getPulseWidth();
        double x = startTime/timeFactor;

        series.add(TO_MILLIS * (startTime - dx * timeFactor / 10.), 0.0);
        series.add(TO_MILLIS * (startTime + timeFactor*(timeLimit + dx / 10.)), 0.0);

        for (int i = 0, numPoints = (int) (timeLimit/dx); i < numPoints; i++) {
            series.add(x * timeFactor * TO_MILLIS, pulseShape.evaluateAt(x - startTime/timeFactor));
            x += dx;
        }

        return series;
    }

}
