package pulse.ui.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.util.List;

import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.tasks.SearchTask;
import pulse.ui.Messages;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class Chart {

	private TimeAxisSpecs timeAxisSpecs;    
    private static JFreeChart chart;
    private static float opacity = 0.15f;
    private static boolean residualsShown = true;
    private static boolean zeroApproximationShown = false;
    
    public static ChartPanel createEmptyPanel() {    	
        chart = ChartFactory.createScatterPlot(
        		"",
        		Messages.getString("Charting.TimeAxisLabel"),
        		(Messages.getString("Charting.TemperatureAxisLabel")),
                null,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        
        var renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
        renderer.setDefaultShapesFilled(false); 
        renderer.setUseFillPaint(false);        
        renderer.setSeriesPaint(0, new Color(1.0f, 0.0f, 0.0f, opacity));
        
        var rendererLines = new XYLineAndShapeRenderer();               
        rendererLines.setSeriesPaint(0, Color.BLUE);
        rendererLines.setSeriesStroke(0, new BasicStroke(2.0f));
        rendererLines.setSeriesShapesVisible(0, false);
        
        var rendererResiduals = new XYLineAndShapeRenderer();               
        rendererResiduals.setSeriesPaint(0, Color.GREEN);
        rendererResiduals.setSeriesShapesVisible(0, false);
        
        var rendererClassic = new XYLineAndShapeRenderer();               
        rendererClassic.setSeriesPaint(0, Color.BLACK);
        rendererClassic.setSeriesShapesVisible(0, false);
        
        var rendererOld = new XYLineAndShapeRenderer();               
        rendererOld.setSeriesPaint(0, Color.BLUE);
        rendererOld.setSeriesStroke(0, 
        		new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
        		        BasicStroke.JOIN_MITER, 2.0f, new float[]{10f}, 0) );
        rendererOld.setSeriesShapesVisible(0, false);
        
        XYPlot plot = chart.getXYPlot();
        
        plot.setRenderer(0, renderer);
        plot.setRenderer(1, rendererLines);
        plot.setRenderer(2, rendererOld);
        plot.setRenderer(3, rendererResiduals);
        plot.setRenderer(4, rendererClassic);
        plot.setBackgroundPaint(Color.white);

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.GRAY);

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.GRAY);

        LegendTitle lt = new LegendTitle(plot);
        lt.setItemFont(new Font("Dialog", Font.PLAIN, 14));
        lt.setBackgroundPaint(new Color(200, 200, 255, 100));
        lt.setFrame(new BlockBorder(Color.black));
        lt.setPosition(RectangleEdge.RIGHT);
        XYTitleAnnotation ta = new XYTitleAnnotation(0.98, 0.2, lt,RectangleAnchor.RIGHT);
        ta.setMaxWidth(0.58);
        plot.addAnnotation(ta);
        
        chart.removeLegend();
        
        return new ChartPanel(chart);  
    }
    
    public static void plot(SearchTask task, boolean extendedCurve) {
    	if(task == null)
    		return;
    	
    	XYPlot plot = chart.getXYPlot();
    	
    	plot.setDataset(0, null);
    	plot.setDataset(1, null); 
    	plot.setDataset(2, null);
    	plot.setDataset(3, null);
    	plot.setDataset(4, null);
    	
        ExperimentalData rawData = task.getExperimentalCurve();
        
        if(rawData == null)
        	return;
        
        var rawDataset = new XYSeriesCollection();
        
        rawDataset.addSeries(series(rawData,"Raw data (" + task.getIdentifier() + ")",extendedCurve));
	    plot.setDataset(0, rawDataset);
	    
	    plot.clearDomainMarkers();
	    
	    ValueMarker lowerMarker = new ValueMarker(rawData.timeAt(rawData.getFittingStartIndex()));
	    
	    Stroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
	            BasicStroke.JOIN_MITER, 5.0f, new float[] {10f}, 0.0f);	    
	    
	    lowerMarker.setPaint(Color.black);	    
	    lowerMarker.setStroke(dashed);
	    
	    ValueMarker upperMarker = new ValueMarker(rawData.timeAt(rawData.getFittingEndIndex()));
	    upperMarker.setPaint(Color.black);
	    upperMarker.setStroke(dashed);
	    
	    //marker.setLabel("here"); // see JavaDoc for labels, colors, strokes
	    plot.addDomainMarker(upperMarker);
	    plot.addDomainMarker(lowerMarker);
        
        if(task.getProblem() != null) {
        	HeatingCurve solution = task.getProblem().getHeatingCurve();
            if(solution != null)        
            	if(task.getScheme() != null) {            		
            		var solutionDataset = new XYSeriesCollection();
            		solutionDataset.addSeries(series(task.getProblem().getHeatingCurve(),
            				"Solution with " + task.getScheme().getSimpleName(),
            				extendedCurve));            		            		            		
            		plot.setDataset(1, solutionDataset);
            		
            		/*
            		 * plot old solutions
            		 */
            		
            		var oldDataset = new XYSeriesCollection();
            		int i = 0;
            		
            		/*
            		HeatingCurve hc = task.getStoredSolution();
            	
            		if(hc != null)
            			oldDataset.addSeries(series(hc, "Stored solution", extendedCurve));		            		            
            		
        			plot.setDataset(2, oldDataset);
            		*/
            		/*
            		 * plot residuals
            		 */
            		
            		if(residualsShown)
            		if(solution.getResiduals() != null) {            			
            		
	            	    final NumberAxis axis2 = new NumberAxis("Residuals (a.u.)");
	            	    plot.setRangeAxis(1, axis2);
	            		
	            		var residualsDataset = new XYSeriesCollection();
	            		residualsDataset.addSeries(residuals(solution));
	            	    plot.setDataset(3, residualsDataset);
	            	    plot.mapDatasetToRangeAxis(2, 1);
	            	    
	            	    double shift = (double)(task.getProblem().getMaximumTemperature().getValue());	            	    
	            	    axis2.setRange(Range.expandToInclude(axis2.getRange(),shift));	            	    	            	    
	            	    
            		} 
            		
            	}
        
        plot.getRenderer().setSeriesPaint(0, new Color(1.0f, 0.0f, 0.0f, opacity));
        
        }
        
        if(zeroApproximationShown) {
        	var p = task.getProblem();
        	var s = task.getScheme();
        	
        	if(p != null)
        		if(s != null)
		        	plotSingle(HeatingCurve.classicSolution(p, 
						(double)(s.getTimeLimit().getValue())));
        }
        
    }
    
    public static void plotSingle(HeatingCurve curve) {
    	if(curve == null)
    		return;
    	
    	XYPlot plot = chart.getXYPlot();
    	
        var classicDataset = new XYSeriesCollection();
        
        classicDataset.addSeries(series(curve,curve.getName(), false));
        
	    plot.setDataset(4, classicDataset);
        plot.getRenderer(4).setSeriesPaint(0, Color.black);   
    }

	public static XYSeries series(HeatingCurve curve, String title, boolean extendedCurve) {		
        var series = new XYSeries(title);
		
	    int realCount = curve.arraySize();
	    double startTime = (double)curve.getStartTime().getValue();
	    double time = 0;	   
	    
	    for(int i = 0; i < realCount; i++) {
	    	time = curve.timeAt(i);
	    	if(time < startTime) 
	    		if(!extendedCurve)
	    			continue;
	    	series.add(time*1.0//timeAxisSpecs.getFactor()
	    			, curve.temperatureAt(i));	 
	    }
	    	    	  
	    return series;
	}
	
	public static XYSeries residuals(HeatingCurve solution) {		
        var series = new XYSeries("Residuals");
		
	    List<Double[]> residuals = solution.getResiduals();
	    int size = residuals.size();
	    
	    for(int i = 0; i < size; i++) 
	    	series.add(residuals.get(i)[0], residuals.get(i)[1]); 	    
	    	    	  
	    return series;
	}

    public TimeAxisSpecs getTimeAxisSpecs( ) {
    	return timeAxisSpecs;
    }
    
    /*
     * 
     */
    
    public enum PlotType {

    	EXPERIMENTAL_DATA(0, Messages.getString("Charting.0")), 
    	SOLUTION(1, Messages.getString("Charting.1")), 
    	CLASSIC_SOLUTION(2, Messages.getString("Charting.2"));
    	
    	private int index;
    	private String style;
    		
    	private PlotType(int i, String style) {
    		this.index = i;
    		this.style = style;
    	}

    	public int getChartIndex() {
    		return index;
    	}

    	public String getStyle() {
    		return style;
    	}
    	
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
	
}