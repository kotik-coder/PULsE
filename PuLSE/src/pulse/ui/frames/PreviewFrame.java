package pulse.ui.frames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToolBar;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import pulse.tasks.ResultFormat;

public class PreviewFrame extends JFrame {

	private final static int FRAME_WIDTH = 640;
	private final static int FRAME_HEIGHT = 480;
	
	private List<String> propertyNames;	
	private JComboBox<String> selectXBox, selectYBox;
	
	private static String xLabel, yLabel;
	
	private double[][][] data;
	private static JFreeChart chart;
	
	private final static Color RESULT_COLOR = Color.BLUE;
	
	public PreviewFrame() {
		init();							
	}
	
	private void init() {
		setSize(FRAME_WIDTH, FRAME_HEIGHT);		
		setTitle("Preview Plotting");
				
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
        
    	plot.getDomainAxis().setLabel(xLabel); 
    	plot.getRangeAxis().setLabel(yLabel);
    	
        var dataset = new XYSeriesCollection();
      
        if(data == null)
        	return;
        
        dataset.addSeries(series(data[selectedX][0], data[selectedX][1], data[selectedY][0], data[selectedY][1]));
	    plot.setDataset(0, dataset);
        plot.getRenderer().setSeriesPaint(0, RESULT_COLOR);
	}
	
	public void update(ResultFormat fmt, double[][][] data) {
		this.data = data;
		List<String> descriptors = fmt.descriptors();
		int size = descriptors.size();
		
		propertyNames = new ArrayList<String>(size);
		String tmp;
		
		for(int i = 0; i < size; i++) {
			tmp = descriptors.get(i).replaceAll("<.*?>" , " ").replaceAll("&.*?;" , "");
			propertyNames.add(tmp);
		}
		
		selectXBox.removeAllItems();
		
		for(String s : descriptors)
			selectXBox.addItem(s);
		
		selectXBox.setSelectedIndex(0);
		
		selectYBox.removeAllItems();
		
		for(String s : descriptors)
			selectYBox.addItem(s);
		
		selectYBox.setSelectedIndex(1);	
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
        
        var renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
        renderer.setSeriesPaint(0, RESULT_COLOR);
        
        double size = 6.0;
        double delta = size / 2.0;
        Shape shape1 = new Rectangle2D.Double(-delta, -delta, size, size);
        renderer.setSeriesShape(0, shape1);
        
        XYPlot plot = chart.getXYPlot();
        
        plot.setRenderer(0, renderer);
        plot.setBackgroundPaint(Color.white);

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.GRAY);

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.GRAY);

        chart.removeLegend();
        
        return new ChartPanel(chart);  
    }
    
    /*
     * 
     */
	
	private static XYSeries series(double[] x, double[] xerr, double[] y, double[] yerr) {		
        var series = new XYSeries("Preview");
			    
	    for(int i = 0; i < x.length; i++) {
	    	series.add(x[i], y[i]);	    
	    }
	    	    	  
	    return series;
	}
    
}