package pulse.ui.frames;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.ResultFormat;
import pulse.ui.Launcher;

public class PreviewFrame extends JInternalFrame {

	private final static int FRAME_WIDTH = 640;
	private final static int FRAME_HEIGHT = 480;
	
	private List<String> propertyNames;	
	private JComboBox<String> selectXBox, selectYBox;
	
	private static String xLabel, yLabel;
	
	private double[][][] data;
	private static JFreeChart chart;
	
	private final static Color RESULT_COLOR = Color.BLUE;
	private final static Color SMOOTH_COLOR = Color.RED;
	
	private static boolean drawSmooth = true;
	
	private final static int ICON_SIZE = 24;
	
	public PreviewFrame() {
		init();							
	}
	
	private void init() {
		setSize(FRAME_WIDTH, FRAME_HEIGHT);		
		setTitle("Preview Plotting");
		setClosable(true);	
		setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);
		
		getContentPane().setLayout(new BorderLayout());
		
		getContentPane().add(createEmptyPanel(), BorderLayout.CENTER);
		
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setLayout(new GridLayout());
		
		getContentPane().add(toolbar, BorderLayout.SOUTH);
		
		JLabel selectX = new JLabel("Bottom axis: ");
		toolbar.add(selectX);
		
		selectXBox = new JComboBox<String>();

		toolbar.add(selectXBox);
		toolbar.add(new JSeparator());
		
		JLabel selectY = new JLabel("Vertical axis:");
		toolbar.add(selectY);
		
		selectYBox = new JComboBox<String>();
		toolbar.add(selectYBox);	
		
		var drawSmoothBtn = new JToggleButton();
		drawSmoothBtn.setToolTipText("Smooth with cubic normal splines");
		drawSmoothBtn.setIcon(Launcher.loadIcon("spline.png", ICON_SIZE));
		drawSmoothBtn.setSelected(true);
		toolbar.add(drawSmoothBtn);
		
		drawSmoothBtn.addActionListener( e -> 
		{
			drawSmooth = drawSmoothBtn.isSelected();
			replot(chart);
		});
		
		selectXBox.addItemListener(e -> replot(chart));
		selectYBox.addItemListener(e -> replot(chart));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}		
	
	private void replot(JFreeChart chart) {
		int selectedX = selectXBox.getSelectedIndex();
		int selectedY = selectYBox.getSelectedIndex();
		
		if(selectedX < 0 || selectedY < 0)
			return;
		
		xLabel = propertyNames.get(selectedX);
		yLabel = propertyNames.get(selectedY);
		
    	XYPlot plot = chart.getXYPlot();     
    	
    	plot.setDataset(0, null);
    	plot.setDataset(1, null);
        
    	plot.getDomainAxis().setLabel(xLabel); 
    	plot.getRangeAxis().setLabel(yLabel);
    	
        var dataset = new XYIntervalSeriesCollection();
        var datasetSmooth = new XYSeriesCollection();
        
        if(data == null)
        	return;
        
        dataset.addSeries(series(data[selectedX][0], data[selectedX][1], data[selectedY][0], data[selectedY][1]));
	    plot.setDataset(0, dataset);
        
        if(drawSmooth) {
		    datasetSmooth.addSeries(series(data[selectedX][0], data[selectedY][0]));
			plot.setDataset(1, datasetSmooth);
		} 
        
	}
	
	public void update(ResultFormat fmt, double[][][] data) {
		this.data = data;
		List<String> descriptors = fmt.descriptors();
		List<String> htmlDescriptors = new ArrayList<String>();
		int size = descriptors.size();
		
		propertyNames = new ArrayList<String>(size);
		String tmp;
		
		for(int i = 0; i < size; i++) {
			tmp = descriptors.get(i).replaceAll("<.*?>" , " ").replaceAll("&.*?;" , "");
			htmlDescriptors.add("<html>" + descriptors.get(i) + "</html>");
			propertyNames.add(tmp);
		}
		
		selectXBox.removeAllItems();
		
		for(String s : htmlDescriptors)
			selectXBox.addItem(s);
		
		selectXBox.setSelectedIndex(fmt.indexOf(NumericPropertyKeyword.TEST_TEMPERATURE));
		
		selectYBox.removeAllItems();
		
		for(String s : htmlDescriptors)
			selectYBox.addItem(s);
		
		selectYBox.setSelectedIndex(fmt.indexOf(NumericPropertyKeyword.DIFFUSIVITY));
	}
	
	/*
	 * 
	 */

    private static ChartPanel createEmptyPanel() {    	
        chart = ChartFactory.createScatterPlot(
        		"",
        		xLabel,
        		yLabel,
                null,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
                
        var renderer = new XYErrorRenderer();
        renderer.setSeriesPaint(0, RESULT_COLOR);
        
        var rendererSpline = new XYSplineRenderer();
        rendererSpline.setSeriesPaint(0, SMOOTH_COLOR);
        rendererSpline.setSeriesStroke(
        	    0, 
        	    new BasicStroke(
        	        2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
        	        1.0f, new float[] {6.0f, 6.0f}, 0.0f
        	    ));
        
        double size = 6.0;
        double delta = size / 2.0;
        Shape shape1 = new Rectangle2D.Double(-delta, -delta, size, size);
        renderer.setSeriesShape(0, shape1);
        
        XYPlot plot = chart.getXYPlot();
        
        plot.setRenderer(0, renderer);
        plot.setRenderer(1, rendererSpline);
        plot.setBackgroundPaint(Color.white);

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.GRAY);

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.GRAY);

	    plot.getRenderer(1).setSeriesPaint(1, SMOOTH_COLOR);
        plot.getRenderer(0).setSeriesPaint(0, RESULT_COLOR);
	    
        chart.removeLegend();
        
        ChartPanel cp = new ChartPanel(chart);
        
        cp.setMaximumDrawHeight(2000);
        cp.setMaximumDrawWidth(2000);
        cp.setMinimumDrawWidth(10);
        cp.setMinimumDrawHeight(10);
        
        return cp;  
    }
    
    /*
     * 
     */
	
	private static XYIntervalSeries series(double[] x, double[] xerr, double[] y, double[] yerr) {		
        var series = new XYIntervalSeries("Preview");
			    
	    for(int i = 0; i < x.length; i++) 
	    	series.add(x[i], x[i] - xerr[i], x[i] + xerr[i], 
	    			y[i], y[i] - yerr[i], y[i] + yerr[i]);	    	    
	    	    	  
	    return series;
	}
	
    /*
     * 
     */
	
	private static XYSeries series(double[] x, double[] y) {		
        var series = new XYSeries("Preview");
			    
	    for(int i = 0; i < x.length; i++) 
	    	series.add(x[i], y[i]);	    	    
	    	    	  
	    return series;
	}

	public boolean isDrawSmooth() {
		return drawSmooth;
	}

	public void setDrawSmooth(boolean drawSmooth) {
		PreviewFrame.drawSmooth = drawSmooth;
	}
    
}