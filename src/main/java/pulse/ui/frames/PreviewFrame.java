package pulse.ui.frames;

import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.JOIN_ROUND;
import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.Color.BLUE;
import static java.awt.Color.RED;
import static org.jfree.chart.ChartFactory.createScatterPlot;
import static org.jfree.chart.plot.PlotOrientation.VERTICAL;
import static pulse.properties.NumericPropertyKeyword.DIFFUSIVITY;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;
import static pulse.util.ImageUtils.loadIcon;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.exception.NonMonotonicSequenceException;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import pulse.tasks.processing.ResultFormat;

@SuppressWarnings("serial")
public class PreviewFrame extends JInternalFrame {

    private final static int FRAME_WIDTH = 640;
    private final static int FRAME_HEIGHT = 480;

    private List<String> propertyNames;
    private JComboBox<String> selectXBox, selectYBox;

    private static String xLabel, yLabel;

    private double[][][] data;
    private static JFreeChart chart;

    private final static Color RESULT_COLOR = BLUE;
    private final static Color SMOOTH_COLOR = RED;

    private static boolean drawSmooth = true;

    private final static int ICON_SIZE = 24;
    private final static int MARKER_SIZE = 6;
    private final static int SPLINE_SAMPLES = 100;

    public PreviewFrame() {
        super("Preview Plotting", true, true, true, true);
        init();
    }

    private void init() {
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(createEmptyPanel(), CENTER);

        var toolbar = new JToolBar(); 
        toolbar.setFloatable(false);
        toolbar.setLayout(new GridLayout());

        getContentPane().add(toolbar, SOUTH);

        var selectX = new JLabel("Bottom axis: ");
        toolbar.add(selectX);

        selectXBox = new JComboBox<>();
        selectXBox.setFont(selectXBox.getFont().deriveFont(11));

        toolbar.add(selectXBox);
        toolbar.add(new JSeparator());

        var selectY = new JLabel("Vertical axis:");
        toolbar.add(selectY);

        selectYBox = new JComboBox<>();
        selectYBox.setFont(selectYBox.getFont().deriveFont(11));
        toolbar.add(selectYBox);

        var drawSmoothBtn = new JToggleButton();
        drawSmoothBtn.setToolTipText("Smooth with cubic normal splines");
        drawSmoothBtn.setIcon(loadIcon("spline.png", ICON_SIZE));
        drawSmoothBtn.setSelected(true);
        toolbar.add(drawSmoothBtn);

        drawSmoothBtn.addActionListener(e -> {
            drawSmooth = drawSmoothBtn.isSelected();
            replot(chart);
        });

        selectXBox.addItemListener(e -> replot(chart));
        selectYBox.addItemListener(e -> replot(chart));
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);                               
    }

    private void replot(JFreeChart chart) {
        int selectedX = selectXBox.getSelectedIndex();
        int selectedY = selectYBox.getSelectedIndex();

        if (selectedX < 0 || selectedY < 0) {
            return;
        }

        var plot = chart.getXYPlot();

        plot.setDataset(0, null);
        plot.setDataset(1, null);

        var dataset = new XYIntervalSeriesCollection();

        if (data == null) {
            return;
        }

        dataset.addSeries(series(data[selectedX][0], data[selectedX][1], data[selectedY][0], data[selectedY][1]));
        plot.setDataset(0, dataset);

        if (drawSmooth) {
            drawSmooth(plot, selectedX, selectedY);
        }
        
    }

    private void drawSmooth(XYPlot plot, int selectedX, int selectedY) {
        PolynomialSplineFunction interpolation = null;

        try {
            //LOESS interpolator for monotonic x sequence (average results)
            //usually works when number of points is large
            var interpolator = new LoessInterpolator();
            interpolation = interpolator.interpolate(data[selectedX][0], data[selectedY][0]);
        } catch (DimensionMismatchException | NumberIsTooSmallException e) {
            //Akima spline for small number of points
            var interpolator = new AkimaSplineInterpolator();
            interpolation = interpolator.interpolate(data[selectedX][0], data[selectedY][0]);
        } catch( NonMonotonicSequenceException e) {
            //do not draw if points not strictly increasing
            return;
        }

        double[] x = new double[SPLINE_SAMPLES];
        double[] y = new double[SPLINE_SAMPLES];

        double dx = (data[selectedX][0][data[selectedX][0].length - 1] - data[selectedX][0][0]) / (SPLINE_SAMPLES - 1);

        for (int i = 0; i < SPLINE_SAMPLES; i++) {
            x[i] = data[selectedX][0][0] + dx * i;
            try {
                y[i] = interpolation.value(x[i]);
            } catch(OutOfRangeException e) {
                y[i] = Double.NaN;
            }
        }

        var datasetSmooth = new XYSeriesCollection();
        datasetSmooth.addSeries(series(x, y));
        plot.setDataset(1, datasetSmooth);
    }

    public void update(ResultFormat fmt, double[][][] data) {
        this.data = data;
        var descriptors = fmt.descriptors();
        var size = descriptors.size();

        propertyNames = new ArrayList<>(size);
        String tmp;
        
        selectXBox.removeAllItems();
        selectYBox.removeAllItems();

        for (var s : descriptors) {
            selectXBox.addItem(s);
            selectYBox.addItem(s);
        }        

        selectXBox.setSelectedIndex(fmt.indexOf(TEST_TEMPERATURE));
        selectYBox.setSelectedIndex(fmt.indexOf(DIFFUSIVITY));
    }

    /*
	 * 
     */
    private static ChartPanel createEmptyPanel() {
        chart = createScatterPlot("", xLabel, yLabel, null, VERTICAL, true, true, false);

        var renderer = new XYErrorRenderer();
        renderer.setSeriesPaint(0, RESULT_COLOR);
        renderer.setDefaultShapesFilled(false);

        var rendererLine = new XYLineAndShapeRenderer();
        rendererLine.setDefaultShapesVisible(false);
        rendererLine.setSeriesPaint(0, SMOOTH_COLOR);
        rendererLine.setSeriesStroke(0,
                new BasicStroke(2.0f, CAP_ROUND, JOIN_ROUND, 1.0f, new float[]{6.0f, 6.0f}, 0.0f));

        var plot = chart.getXYPlot();

        plot.setRenderer(0, renderer);
        plot.setRenderer(1, rendererLine);

        //plot.setRangeGridlinesVisible(false);
        //plot.setDomainGridlinesVisible(false);        

        plot.getRenderer(1).setSeriesPaint(1, SMOOTH_COLOR);
        plot.getRenderer(0).setSeriesPaint(0, RESULT_COLOR);
        plot.getRenderer(0).setSeriesShape(0, 
                new Rectangle(-MARKER_SIZE/2, -MARKER_SIZE/2, MARKER_SIZE, MARKER_SIZE));

        chart.removeLegend();

        var cp = new ChartPanel(chart);

        cp.setMaximumDrawHeight(2000);
        cp.setMaximumDrawWidth(2000);
        cp.setMinimumDrawWidth(10);
        cp.setMinimumDrawHeight(10);

        chart.setBackgroundPaint(UIManager.getColor("Panel.background"));
        
        return cp;
    }

    /*
	 * 
     */
    private static XYIntervalSeries series(double[] x, double[] xerr, double[] y, double[] yerr) {
        var series = new XYIntervalSeries("Preview");

        for (var i = 0; i < x.length; i++) {
            series.add(x[i], x[i] - xerr[i], x[i] + xerr[i], y[i], y[i] - yerr[i], y[i] + yerr[i]);
        }

        return series;
    }

    /*
	 * 
     */
    private static XYSeries series(double[] x, double[] y) {
        var series = new XYSeries("Preview");

        for (var i = 0; i < x.length; i++) {
            series.add(x[i], y[i]);
        }

        return series;
    }

    public boolean isDrawSmooth() {
        return drawSmooth;
    }

    public void setDrawSmooth(boolean drawSmooth) {
        PreviewFrame.drawSmooth = drawSmooth;
    }

}
