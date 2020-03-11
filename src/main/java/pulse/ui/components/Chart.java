package pulse.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.util.List;
import java.util.Objects;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.tasks.SearchTask;
import pulse.ui.Messages;

public class Chart {
   
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
    	Objects.requireNonNull(task);    	
    	
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
	    
	    ValueMarker lowerMarker = new ValueMarker(rawData.getRange().getSegment().getMinimum());
	    
	    Stroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
	            BasicStroke.JOIN_MITER, 5.0f, new float[] {10f}, 0.0f);	    
	    
	    lowerMarker.setPaint(Color.black);	    
	    lowerMarker.setStroke(dashed);
	    
	    ValueMarker upperMarker = new ValueMarker(rawData.getRange().getSegment().getMaximum());
	    upperMarker.setPaint(Color.black);
	    upperMarker.setStroke(dashed);
	    
	    plot.addDomainMarker(upperMarker);
	    plot.addDomainMarker(lowerMarker);
	    
        if(task.getProblem() != null) {
        	        
        	HeatingCurve solution = task.getProblem().getHeatingCurve();
            
        	if(solution != null && task.getScheme() != null) {
            	
            		if(solution.arraySize() > 0) {
            			
	            		var solutionDataset = new XYSeriesCollection();            		
	            		
	            		solutionDataset.addSeries(series(task.getProblem().getHeatingCurve(),
	            				"Solution with " + task.getScheme().getSimpleName(),
	            				extendedCurve));            		            		            		
	            		plot.setDataset(1, solutionDataset);
	            		
	            		/*
	            		 * plot residuals
	            		 */
	            		
	            		if(residualsShown)
	            		if(task.getResidualStatistic().getResiduals() != null) {            			
	            			var residualsDataset = new XYSeriesCollection();
		            		residualsDataset.addSeries(residuals(task));
		            	    plot.setDataset(3, residualsDataset);
	            		}
	            		
            		}
            		
            	}
        
        plot.getRenderer().setSeriesPaint(0, new Color(1.0f, 0.0f, 0.0f, opacity));
        
        }
        
        if(zeroApproximationShown) {
        	var p = task.getProblem();
        	var s = task.getScheme();
        	
        	if(p != null && s != null)
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
	    double startTime = (double)curve.getTimeShift().getValue();
	    double time = 0;	   
	    
	    for(int i = 0; i < realCount; i++) {
	    	
	    	time = curve.timeAt(i);
	    	
	    	if(time < startTime && !extendedCurve) 
	    		continue;
	    	
	    	series.add(time*1.0//timeAxisSpecs.getFactor()
	    			, curve.temperatureAt(i));	 
	    }
	    	    	  
	    return series;
	}
	
	public static XYSeries residuals(SearchTask task) {
		HeatingCurve solution = task.getProblem().getHeatingCurve();		
		
        final double span = solution.maxTemperature() - solution.getBaseline().valueAt(0); 
	    final double offset = solution.getBaseline().valueAt(0) - span/2.0;	            	    
		
	    var series = new XYSeries(String.format("Residuals (offset %3.2f)", offset)); 
	    
	    List<double[]> residuals = task.getResidualStatistic().getResiduals();
	    int size = residuals.size();
	    
	    for(int i = 0; i < size; i++) 
	    	series.add(residuals.get(i)[0], (Number)(residuals.get(i)[1] + offset)); 	    
	    	    	  
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
	
}