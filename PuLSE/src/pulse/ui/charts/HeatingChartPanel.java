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
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.tasks.Identifier;
import pulse.ui.frames.RangeSelectionFrame;

public class HeatingChartPanel extends JFXPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Identifier id;
	private ExperimentalData expCurve;
	private HeatingCurve curve;
	private XYChart<Number,Number> heatingChart;
	private RectangleSelection selection;
	double minXChart, maxXChart, maxYChart, minYChart;
	
	public HeatingChartPanel(Identifier id, ExperimentalData expCurve, HeatingCurve curve) {
		super();
		this.curve = curve;
		this.expCurve = expCurve;
		this.id = id;

    	ChoiceMenu choiceMenu = new ChoiceMenu();
    	choiceMenu.setEnabled(true);
		selection = new RectangleSelection();
		selection.setVisible(true);
		
		RangeSelectionFrame rangeSelectionFrame = new RangeSelectionFrame(this);
		HeatingChartPanel reference = this;

    	choiceMenu.getZoomItem().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Charting.zoomTo(heatingChart, minXChart, maxXChart, maxYChart, minYChart );
				choiceMenu.setVisible(false);
			}
    		
    	});
    	
    	choiceMenu.getFittingLimitsItem().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Charting.highlightSelection(heatingChart, minXChart, maxXChart);
            	rangeSelectionFrame.setRangeFields(minXChart, maxXChart);
            	rangeSelectionFrame.setLocationRelativeTo(reference);
            	rangeSelectionFrame.setVisible(true);
            	choiceMenu.setVisible(false);
			}
    		
    	});
		
		
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
	            choiceMenu.setVisible(false);
            	
            	if (SwingUtilities.isRightMouseButton(e))
	            	return;
            	
            	selection.setClickPoint(e.getPoint());
            	selection.setSelectionBounds(null);
            	Charting.clearSelection(heatingChart);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            	Rectangle selectionBounds = selection.getSelectionBounds();
	            
	            if (SwingUtilities.isRightMouseButton(e) || selectionBounds == null) {
	            	Charting.clearSelection(heatingChart);
	            	rangeSelectionFrame.setVisible(false);
	        		Charting.autoRange(heatingChart);
	        		return;
	            }
	            
            	double minX = selectionBounds.getMinX();
            	double maxX = selectionBounds.getMaxX();    	
            	minXChart = Charting.chartX(heatingChart, minX);
            	maxXChart = Charting.chartX(heatingChart, maxX);
        
            	double minY = selectionBounds.getMinY();
            	double maxY = selectionBounds.getMaxY();
            	minYChart = Charting.chartY(heatingChart, minY);
            	maxYChart = Charting.chartY(heatingChart, maxY);
            	
            	choiceMenu.setLocation(e.getLocationOnScreen());
            	choiceMenu.show(reference, (int)selectionBounds.getCenterX(), (int)selectionBounds.getCenterY() );
           
                selection.setSelectionBounds(null);
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
	
	public HeatingCurve getHeatingCurve() {
		return curve;
	}
	
	public void setHeatingCurve(HeatingCurve curve) {
		this.curve = curve;
	}
	
	public ExperimentalData getExperimentalCurve() {
		return expCurve;
	}
	
	public Identifier getIdentifier() {
		return id;
	}
	
	protected void setHeatingChart(XYChart<Number,Number> heatingChart) {
		this.heatingChart = heatingChart;
	}
	
	public XYChart<Number,Number> getHeatingChart() {
		return heatingChart;
	}
	
	public void clearChart() {
		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				curve = null;
				Series<Number,Number> s = null;
				ObservableList<Series<Number,Number>> list = getHeatingChart().getData();
				int size = list.size();
				
				for(int i = 0; i < size; i++) {
					s = list.get(i);
					if(s.getNode().getStyle().equals(Charting.STYLE_LINE_ONLY))
						getHeatingChart().getData().remove(s);					
				}
			}
			
		});
	}
	
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        selection.paintComponent(g);
    }
    
    public class RectangleSelection extends JPanel {

        /**
		 * 
		 */
		private static final long serialVersionUID = 6195826661405922987L;
		private Rectangle selectionBounds;
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
        
    }
    
    class ChoiceMenu extends JPopupMenu {
    	
    	/**
		 * 
		 */
		private static final long serialVersionUID = -3471882740905439544L;
		private JMenuItem zoom, fittingLimits;

    	public ChoiceMenu() {
    		super();
    		zoom = new JMenuItem("Zoom to Selection");
    		fittingLimits = new JMenuItem("Limit Fitting Range");
    		this.add(zoom);
    		this.add(fittingLimits);	
    	}
    	
    	JMenuItem getZoomItem() { 
    		return zoom;
    	}
    	
    	JMenuItem getFittingLimitsItem() {
    		return fittingLimits;
    	}
    	
    }
	
}
