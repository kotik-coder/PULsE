package pulse.ui.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
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
import pulse.tasks.Identifier;
import pulse.ui.Messages;
import pulse.ui.components.PlotType;
import pulse.ui.frames.RangeSelectionFrame;

public class Chart extends JFXPanel {
	
	private static final long serialVersionUID = 1L;

	private XYChart<Number,Number> chart;

	private RectangleSelection selection;
	private RangeSelectionFrame rangeSelectionFrame;
	private ChoiceMenu choiceMenu;		
	
	private Identifier identifier;	
	
	private final static int FONT_SIZE = 20;
	private final static Font TOOL_TIP_FONT = Font.font(Messages.getString("Charting.FonName"), 26);
	
	private final static java.awt.Font MENU_FONT = new java.awt.Font(Messages.getString("Charting.FonName"), java.awt.Font.PLAIN, 20);
	
	private TimeAxisSpecs timeAxisSpecs;
	
	public Chart() {
		super();

    	choiceMenu			= new ChoiceMenu(this);
		rangeSelectionFrame = new RangeSelectionFrame(this);
		selection			= new RectangleSelection();
		
        addMouseHandler();
		
	}
	
	private void addMouseHandler() {
		MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
	            choiceMenu.setVisible(false);	            
            	
            	if (SwingUtilities.isRightMouseButton(e)) {
            		zoomOut();
            		return;
            	}
            		            	                      	
            	selection.setClickPoint(e.getPoint());
            	undoHighlight();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            	
	            if (SwingUtilities.isRightMouseButton(e) || selection == null) {
	            	undoHighlight();
	            	rangeSelectionFrame.setVisible(false);
	        		return;
	            }
	            
	            Rectangle bounds = selection.getSelectionBounds();
	            
	            if(bounds == null)
	            	return;
	                	
            	double minXChart = convertToChartX(bounds.getMinX() );
            	double maxXChart = convertToChartX(bounds.getMaxX() );        
            	double minYChart = converToChartY(bounds.getMinY() );
            	double maxYChart = converToChartY(bounds.getMaxY() );
            	
            	selection.setLocalCoordinatesSelection(new pulse.util.geom.Rectangle(minXChart, maxXChart, maxYChart, minYChart));
            	
            	choiceMenu.setLocation(e.getLocationOnScreen());
            	choiceMenu.show(e.getComponent(), (int)bounds.getCenterX(), (int)bounds.getCenterY() );
                           
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
	            if (SwingUtilities.isRightMouseButton(e))
	            	return;

                Point dragPoint = e.getPoint();                
                
                int x = Math.min(selection.getClickPoint().x, dragPoint.x);
                int y = Math.min(selection.getClickPoint().y, dragPoint.y);
                int width = Math.max(selection.getClickPoint().x - dragPoint.x, dragPoint.x - selection.getClickPoint().x);
                int height = Math.max(selection.getClickPoint().y - dragPoint.y, dragPoint.y - selection.getClickPoint().y);
                
                selection.setSelectionBounds(new Rectangle(x, y, width, height));
   
                repaint();
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
	}
	
	public static void setup() {
		Platform.setImplicitExit(false);
	}
	
	public void plot(HeatingCurve curve, PlotType type, boolean extendedCurve) {
		if(curve.arraySize() < 1)
			return;
		
		Platform.runLater(() -> {
			
				if(type == PlotType.EXPERIMENTAL_DATA) {
					int realCount = curve.arraySize();
					if(curve.timeAt(realCount - 1) < 0.1) 
				    	timeAxisSpecs = TimeAxisSpecs.MILLIS;
				    else
				    	timeAxisSpecs = TimeAxisSpecs.SECONDS;
				    
				    chart.getXAxis().setLabel(timeAxisSpecs.getTitle());
				}
			
				chart.getData().add(series(curve, extendedCurve));
												
				Set<Node> nodes = chart.lookupAll(".series" + type.getChartIndex()); //$NON-NLS-1$               
				
				for (Node n : nodes) 
					n.setStyle(type.getStyle());	
					
               if(type == PlotType.EXPERIMENTAL_DATA)
            	   addTooltips();
               
               }
				
		);      	
	
	}
	
	public void clear() {
		
		Platform.runLater(() -> {
				ObservableList<Series<Number,Number>> data = chart.getData();
			
				if(data.isEmpty())
					return;

				data.clear();
			    				
            }
				
		); 
		
	}
	
	public void makeChart() {
		Platform.runLater(() -> {
		 NumberAxis xAxis = new NumberAxis();
	     NumberAxis yAxis = new NumberAxis();
	     
	     xAxis.setLabel(Messages.getString("Charting.TimeAxisLabel")); //$NON-NLS-1$
	     yAxis.setLabel(Messages.getString("Charting.TemperatureAxisLabel")); //$NON-NLS-1$
	     
	     //creating the chart
	     
	     chart = new LineChart<Number,Number>(xAxis,yAxis);
	     chart.setAnimated(false);
	     
		 chart.setStyle("-fx-font-size: " + FONT_SIZE + "px;");
		 xAxis.tickLabelFontProperty().set(Font.font(FONT_SIZE));
		 yAxis.tickLabelFontProperty().set(Font.font(FONT_SIZE));	
		 
		 chart.applyCss();
		 
		 setScene(new Scene(chart));
		 
		xAxis.setAutoRanging(true);
		yAxis.setAutoRanging(true);
		 
		});
	}
	
	public Series<Number,Number> series(HeatingCurve curve, boolean extendedCurve) {
		Series<Number,Number> series = new Series<Number, Number>();		
		
		series.setName(curve.toString());
		
	    ObservableList<Data<Number,Number>> data = series.getData();	    
	    
	    int realCount = curve.arraySize();	    
	    double time = 0;
	    
	    for(int i = 0; i < realCount; i++) {
	    	time = curve.timeAt(i);
	    	if(time < 0) 
	    		if(!extendedCurve)
	    			continue;
	    	data.add(new Data<Number, Number>(time*timeAxisSpecs.getFactor(), curve.temperatureAt(i)));	    
	    }
	    	
	    return series;			
	}
	
	private void addTooltips() {		
		chart.getData().stream().forEach(series -> 
			series.getData().stream().forEach(data -> addTooltip(data)));
	}

	private static void addTooltip(Data<Number, Number> data) {
		Tooltip t = new Tooltip(
	            Messages.getString("Charting.XToolTip") + String.format(Messages.getString("Charting.XFormat"), data.getXValue()) + System.getProperty("line.separator") +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	            Messages.getString("Charting.YToolTip") + String.format(Messages.getString("Charting.YFormat"), data.getYValue()) ); //$NON-NLS-1$ //$NON-NLS-2$
		t.setFont(TOOL_TIP_FONT);
		Tooltip.install(data.getNode(), t);				  
	}
	
	public void undoHighlight() {					
		ObservableList<Series<Number,Number>> data = chart.getData();
		
		if(data.isEmpty()) return;
		
		Series<Number,Number> expSeries = data.get(PlotType.EXPERIMENTAL_DATA.getChartIndex());
		
		for(Data<Number,Number> d : expSeries.getData()) 	
			d.getNode().setEffect(null);
		
	}
	
	public void highlightPoints(pulse.util.geom.Rectangle rectangle) {		
		Series<Number,Number> expSeries = chart.getData().get(PlotType.EXPERIMENTAL_DATA.getChartIndex());
		
		Bloom effect = new Bloom(0.1);
		
		double x = 0;
		
		double xmin = rectangle.getXLower();
		double xmax = rectangle.getXUpper();
		
		for(Data<Number,Number> d : expSeries.getData()) {
			
			x = (double) d.getXValue();
			
			if(x < xmin)
				continue;
			
			if(x > xmax)
				continue;
			
			d.getNode().setEffect(effect);
			
		}
		
	}
	
	public void zoomTo(pulse.util.geom.Rectangle rectangle) {
		
		NumberAxis xAxis = (NumberAxis) chart.getXAxis();
		NumberAxis yAxis = (NumberAxis) chart.getYAxis();
		
		xAxis.setAutoRanging(false);
		yAxis.setAutoRanging(false);
		
		xAxis.setLowerBound(rectangle.getXLower());
		xAxis.setUpperBound(rectangle.getXUpper());
		yAxis.setLowerBound(rectangle.getYLower());
		yAxis.setUpperBound(rectangle.getYUpper());
		
	}
	
	public void zoomOut() {
		
		NumberAxis xAxis = (NumberAxis) chart.getXAxis();
		NumberAxis yAxis = (NumberAxis) chart.getYAxis();
		
		xAxis.setAutoRanging(true);
		yAxis.setAutoRanging(true);
		
	}	
	
	public double convertToChartX(double x) {
    	Node chartPlotBackground = chart.lookup(".chart-plot-background"); //$NON-NLS-1$
    	double chartZeroX = chartPlotBackground.getLayoutX();
    	return ((double) chart.getXAxis().getValueForDisplay(x-chartZeroX));
	}
	
	public double converToChartY(double y) {
    	Node chartPlotBackground = chart.lookup(".chart-plot-background"); //$NON-NLS-1$
    	double chartZeroY = chartPlotBackground.getLayoutY();
    	return ((double) chart.getYAxis().getValueForDisplay(y-chartZeroY));
	}
	
	protected void setHeatingChart(LineChart<Number,Number> heatingChart) {
		this.chart = heatingChart;
	}
	
	public XYChart<Number,Number> getHeatingChart() {
		return chart;
	}
	
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        selection.paintComponent(g);
    }
    
    public Identifier getIdentifier() {
		return identifier;
	}

	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	public class RectangleSelection extends JPanel {

        /**
		 * 
		 */
		private static final long serialVersionUID = 6195826661405922987L;
		private Rectangle selectionBounds;
		private pulse.util.geom.Rectangle localCoordinatesSelection;		
        private Point clickPoint;
        private Stroke dashed;

        public RectangleSelection() {
        	super();
            setOpaque(false);
            dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(new Color(255, 255, 255, 128));

            Area fill = new Area(new Rectangle(new Point(0, 0), getSize()));
            if (selectionBounds != null) {
                fill.subtract(new Area(selectionBounds));
            }
            g2d.fill(fill);
            if (selectionBounds != null) {
                g2d.setColor(Color.blue);
                g2d.setStroke(dashed);
                g2d.draw(selectionBounds);
            }
            g2d.dispose();
        }

    	public Point getClickPoint() {
    		return clickPoint;
    	}

    	public void setClickPoint(Point clickPoint) {
    		this.clickPoint = clickPoint;
    	}

    	public Rectangle getSelectionBounds() {
    		return selectionBounds;
    	}

    	public void setSelectionBounds(Rectangle selectionBounds) {
    		this.selectionBounds = selectionBounds;
    	}

		public pulse.util.geom.Rectangle getLocalCoordinatesSelection() {
			return localCoordinatesSelection;
		}

		public void setLocalCoordinatesSelection(pulse.util.geom.Rectangle fxRectangleBounds) {
			this.localCoordinatesSelection = fxRectangleBounds;
		}
        
    }
    
    class ChoiceMenu extends JPopupMenu {
    	
    	/**
		 * 
		 */
		private static final long serialVersionUID = -3471882740905439544L;

    	public ChoiceMenu(Chart panel) {
    		super();
    		JMenuItem zoomItem = new JMenuItem("Zoom to Selection");
    		zoomItem.setFont(MENU_FONT);
    		JMenuItem fittingLimits = new JMenuItem("Limit Fitting Range");
    		fittingLimits.setFont(MENU_FONT);
    		this.add(zoomItem);
    		this.add(fittingLimits);
    		
    		setEnabled(true);
    	
        	zoomItem.addActionListener(new ActionListener() {

    				@Override
    				public void actionPerformed(ActionEvent e) {
    					selection.setSelectionBounds(null);
    					zoomTo(selection.getLocalCoordinatesSelection());
    					setVisible(false); 
    				}
    				
    			}
        		
        	);
        	
        	fittingLimits.addActionListener(new ActionListener() {

    			@Override
    			public void actionPerformed(ActionEvent e) {
    				pulse.util.geom.Rectangle local = selection.getLocalCoordinatesSelection();
    				highlightPoints(local);
                	rangeSelectionFrame.setRangeFields(local.getXLower(), local.getXUpper());
                	rangeSelectionFrame.setLocationRelativeTo(panel);
                	rangeSelectionFrame.setVisible(true);
                	selection.setSelectionBounds(null);
                	setVisible(false);
    			}
        		
        	});
    		
    	}
    	
    }
    
    public TimeAxisSpecs getTimeAxisSpecs( ) {
    	return timeAxisSpecs;
    }
	
}
