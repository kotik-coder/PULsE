package pulse.ui.charts;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.leores.plot.JGnuplot;
import org.leores.plot.JGnuplot.Plot;
import org.leores.util.data.DataTableSet;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Bloom;
import javafx.scene.text.Font;
import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.problem.statements.Problem;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.util.geom.Point2D;
import pulse.util.geom.Rectangle;

public class Charting {

	protected final static double SCALE_INCREMENT  = 0.02;
	
	public final static String STYLE_LINE_ONLY 	= ((new StringBuilder()).
															append(Messages.getString("Charting.0")). //$NON-NLS-1$
															append(Messages.getString("Charting.1")). //$NON-NLS-1$
															append(Messages.getString("Charting.2"))).toString(); //$NON-NLS-1$
	public final static String STYLE_SCATTER_ONLY	= Messages.getString("Charting.3"); //$NON-NLS-1$
	public final static String STYLE_DASH_DOT		=  ((new StringBuilder()).
															append(Messages.getString("Charting.4")). //$NON-NLS-1$
															append(Messages.getString("Charting.5")). //$NON-NLS-1$
															append(Messages.getString("Charting.6")). //$NON-NLS-1$
															append(Messages.getString("Charting.7")) //$NON-NLS-1$
														).toString();
	
	private static Point2D center;
	
	private final static int FONT_SIZE = 20;
	private final static Font toolTipFont = Font.font(Messages.getString("Charting.FonName"), 26);
	
	private final static int PREVIEW_PLOT_WIDTH = 640;
	private final static int PREVIEW_PLOT_HEIGHT = 480;	
	
	private Charting() {}
	
	public static void plotUsing(HeatingChartPanel panel) {
		List<HeatingCurve> curves = new ArrayList<HeatingCurve>();
		if(panel.getExperimentalCurve() != null)
			curves.add(panel.getExperimentalCurve()); 
		if(panel.getHeatingCurve() != null) 
			curves.add(panel.getHeatingCurve());
		
		if(Platform.isImplicitExit())
			Platform.setImplicitExit(false);
		
		Platform.runLater(new Runnable() { 
			public void run() {

				makeHeatingChart(panel);
				XYChart<Number,Number> chart = panel.getHeatingChart();
				chart.applyCss();
				
				for(HeatingCurve curve : curves) {
					
					if(curve.realCount() < 1)
						continue;
					
					chart.getData().add(series(curve));
								
					int index = chart.getData().size() - 1;
					Set<Node> nodes = chart.lookupAll(".series" + index);                     //$NON-NLS-1$
                
					String style = curve instanceof ExperimentalData ? STYLE_SCATTER_ONLY : STYLE_LINE_ONLY; 
                		
					for (Node n : nodes) 
						n.setStyle(style);                                 
				}

				panel.setScene(new Scene(chart));	
					
                updateRange(chart);							
				
			}
			
		}); 
		
	}
	
	public static void highlightSelection(XYChart<Number,Number> chart, double xMin, double xMax) {
		final int EXP_CURVE_INDEX = 0;
		
		Series<Number,Number> expSeries = chart.getData().get(EXP_CURVE_INDEX);
		
		Bloom effect = new Bloom(0.1);
		
		double x = 0;
		
		for(Data<Number,Number> d : expSeries.getData()) {
			
			x = (double) d.getXValue();
			
			if(x < xMin)
				continue;
			
			if(x > xMax)
				continue;
			
			d.getNode().setEffect(effect);
			
		}
		
	}
	
	public static void zoomTo(XYChart<Number,Number> chart, double xMin, double xMax, double yMin, double yMax) {
	
		NumberAxis xAxis = (NumberAxis) chart.getXAxis();
		NumberAxis yAxis = (NumberAxis) chart.getYAxis();
		
		xAxis.setAutoRanging(false);
		yAxis.setAutoRanging(false);
		
		xAxis.setLowerBound(xMin);
		xAxis.setUpperBound(xMax);
		yAxis.setLowerBound(yMin);
		yAxis.setUpperBound(yMax);
		
	}
	
	public static void autoRange(XYChart<Number,Number> chart) {
		if(chart == null) return;
		chart.getYAxis().setAutoRanging(true);
		chart.getXAxis().setAutoRanging(true);
	}
	
	public static void clearSelection(XYChart<Number,Number> chart) {
		if(chart == null) return;
		
		final int EXP_CURVE_INDEX = 0;		
		
		ObservableList<Series<Number,Number>> data = chart.getData();
		
		if(data.isEmpty()) return;
		
		Series<Number,Number> expSeries = data.get(EXP_CURVE_INDEX);
		
		for(Data<Number,Number> d : expSeries.getData()) 	
			d.getNode().setEffect(null);
		
	}
	
	public static double chartX(XYChart<Number,Number> chart, double x) {
    	Node chartPlotBackground = chart.lookup(".chart-plot-background"); //$NON-NLS-1$
    	double chartZeroX = chartPlotBackground.getLayoutX();
    	return ((double) chart.getXAxis().getValueForDisplay(x-chartZeroX));
	}
	
	public static double chartY(XYChart<Number,Number> chart, double y) {
    	Node chartPlotBackground = chart.lookup(".chart-plot-background"); //$NON-NLS-1$
    	double chartZeroY = chartPlotBackground.getLayoutY();
    	return ((double) chart.getYAxis().getValueForDisplay(y-chartZeroY));
	}
	
	public static boolean isSceneCreated(HeatingChartPanel panel) {
		XYChart<Number,Number> activeChart = panel.getHeatingChart();
		return activeChart == null ? false : (activeChart.getScene() != null);
	}
	
	public static void plotNumericSolution(XYChart<Number,Number> activeChart) {
		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				activeChart.getData().add( series( TaskManager.getSelectedTask().getProblem().getHeatingCurve() ) );
				int index = activeChart.getData().size() - 1;
                Set<Node> nodes = activeChart.lookupAll(".series" + index); //$NON-NLS-1$
                                
                for (Node n : nodes) 
                    n.setStyle(STYLE_LINE_ONLY);                
                
                updateRange(activeChart);
                
			}
			
		});
	}
	
	public static void plotClassicSolution(XYChart<Number,Number> activeChart) {
		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				Series<Number,Number> classicSeries = seriesForClassicSolution();
				
				if(classicSeries == null)
					return;
				
				activeChart.getData().add( classicSeries );				
				int index = activeChart.getData().size() - 1;
                Set<Node> nodes = activeChart.lookupAll(".series" + index); //$NON-NLS-1$
                                
                for (Node n : nodes) 
                    n.setStyle(STYLE_DASH_DOT);                
                
                updateRange(activeChart);
                
			}
			
		});
	}	
	
	private static Rectangle limits(XYChart<Number,Number> chart) {
		NumberAxis xAxis = (NumberAxis) chart.getXAxis();
		NumberAxis yAxis = (NumberAxis) chart.getYAxis();
		
		xAxis.setAutoRanging(false);
		yAxis.setAutoRanging(false);
		
		double lowerX = 100; double lowerY = 100;
		double upperX = 0;	 double upperY = 0;
		double xValue, yValue;
						
		for(Series<Number, Number> s : chart.getData())
			for(Data<Number, Number> data : s.getData()) {
				xValue = (double)data.getXValue();				
				yValue = (double)data.getYValue();
				
				if(xValue < lowerX)
					lowerX = xValue;
				if(xValue > upperX)
					upperX = xValue;
				if(yValue < lowerY)
					lowerY = yValue;
				if(yValue > upperY)
					upperY = yValue;													         
				
		}	
		
		return new Rectangle(lowerX, upperX, lowerY, upperY);
		
	}
	
	private static void addTooltips(XYChart<Number,Number> chart) {							
			for(Series<Number, Number> s : chart.getData())
				for(Data<Number, Number> data : s.getData()) 																        
					addTooltip(data);										
	}
	
	private static void addTooltip(Data<Number, Number> data) {
		Tooltip t = new Tooltip(
                Messages.getString("Charting.XToolTip") + String.format(Messages.getString("Charting.XFormat"), data.getXValue()) + System.getProperty("line.separator") +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                Messages.getString("Charting.YToolTip") + String.format(Messages.getString("Charting.YFormat"), data.getYValue()) ); //$NON-NLS-1$ //$NON-NLS-2$
		t.setFont(toolTipFont);
		Tooltip.install(data.getNode(), t);				  
	}
		
	public static void updateRange(XYChart<Number,Number> chart) {					
		Rectangle view = limits(chart);
		center = view.center();
		
		view.moveTo(center);	
		addTooltips(chart);
				
	}
	
	public static Series<Number,Number> seriesForClassicSolution() {
		Series<Number,Number> series = new Series<Number, Number>();		
		series.setName(Messages.getString("Charting.ClassicSolutionLabel")); //$NON-NLS-1$
		
		Problem p = TaskManager.getSelectedTask().getProblem();
		
	     //populating the series with data
		 
		 if(p == null) {
			JOptionPane.showMessageDialog(null, "Sample parameters required to process classic solution", "Problem Not Set", JOptionPane.ERROR_MESSAGE);
			return null;
		 } 	     
			
		 HeatingCurve curve = p.getHeatingCurve();
		 int maxIndex 		= (int)curve.getNumPoints().getValue();
	     final int N		= 30;
	     
	     for(int i = 0; i < maxIndex; i++) {
				series.getData().add(new Data<Number, Number>(curve.timeAt(i), 
										p.classicSolutionAt(curve.timeAt(i), N)
											*(double)p.getMaximumTemperature().getValue()
									));
		}	     	    	     	     	     
	     
	     return series;	
	}		
	
	public static Series<Number,Number> series(HeatingCurve curve) {
		Series<Number,Number> series = new Series<Number, Number>();		
		
		if(curve instanceof ExperimentalData)
			series.setName(curve.toString()); //$NON-NLS-1$ //$NON-NLS-2$
		else if(! curve.isEmpty() ){			
			SearchTask task = TaskManager.getSelectedTask(); 
			series.setName(task + " ; " + ( task.getScheme() ).shortName());	
		}
			
	     //populating the series with data
	     int maxIndex = (int)curve.getNumPoints().getValue();
	     
	     for(int i = 0; i < maxIndex; i++) 
	    	 series.getData().add(new Data<Number, Number>(curve.timeAt(i), curve.temperatureAt(i)));
	     
	     return series;			
	}	
	
	private static void makeHeatingChart(HeatingChartPanel panel) {
		 final NumberAxis xAxis = new NumberAxis();
	     final NumberAxis yAxis = new NumberAxis();
	     
	     xAxis.setLabel(Messages.getString("Charting.TimeAxisLabel")); //$NON-NLS-1$
	     yAxis.setLabel(Messages.getString("Charting.TemperatureAxisLabel")); //$NON-NLS-1$
	     
	     //creating the chart
	     
	     final LineChart<Number,Number> lineChart = 
	                new LineChart<Number,Number>(xAxis,yAxis);
	     
		 lineChart.setStyle("-fx-font-size: " + FONT_SIZE + "px;");
		 lineChart.getXAxis().tickLabelFontProperty().set(Font.font(FONT_SIZE));
		 lineChart.getYAxis().tickLabelFontProperty().set(Font.font(FONT_SIZE));	
		 
	     panel.setHeatingChart(lineChart);
	     
	}
	
	/*
	 * PREVIEW PLOTS USING JavaGnuplotHybrid
	 */
	
	public static void preview(String xLabel, String yLabel, double[] x, double[] y, double[] xerr, double[] yerr) {	
		JGnuplot jg = new JGnuplot() {
			{
				terminal = "wxt size " + PREVIEW_PLOT_WIDTH + "," + PREVIEW_PLOT_HEIGHT + " enhanced font 'Verdana,10' persist";
			}
		};
		
		Plot plot = new Plot( "Preview plot") {
			{
				xlabel = gnuplotLabelFromHTML(xLabel);
				ylabel = gnuplotLabelFromHTML(yLabel);
			}
		};
		
		jg.plotx = "$header$\n plot '-' title info2(1,1) with xyerrorbars pt 7 ps 2"
				+ ", '-' title info2(1,2) lw 1.5 smooth sbezier \n $data(1,2d)$";
		
		DataTableSet dts = plot.addNewDataTableSet("2D Plot");
		if((xerr != null) && (yerr != null))
			dts.addNewDataTable("Calculated Points (" + TaskManager.getSampleName() + ")", x, y, xerr, yerr);
		else
			dts.addNewDataTable("Calculated Points (" + TaskManager.getSampleName() + ")", x, y);
		dts.addNewDataTable("Bezier Approximation", x, y);
		
		Thread plottingThread = new Thread( new Runnable() {

			@Override
			public void run() {
				jg.execute(plot, jg.plotx);
			}
			
		});
		
		plottingThread.start();
		
	}
	
	private static String gnuplotLabelFromHTML(String propertyName) {
		String subReplace = propertyName.replace("<sub>", "_{");
		String subsubReplace = subReplace.replace("</sub>", "}");
		String supReplace = subsubReplace.replace("<sup>", "^{");
		String supsupReplace = supReplace.replace("</sup>", "}");
		return supsupReplace.replaceAll("\\<[^>]*>","");
	}
	
}
