package pulse.ui.components.panels;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import pulse.ui.components.Chart;
import pulse.ui.components.listeners.PlotRequestListener;

@SuppressWarnings("serial")
public class OpacitySlider extends JSlider {

	private List<PlotRequestListener> listeners;
	
	private final static float SLIDER_A_COEF = 0.01f;
    private final static float SLIDER_B_COEF = 0.04605f;
	
	public OpacitySlider() {
		initComponents();
        listeners = new ArrayList<PlotRequestListener>();
	}
	
	public void initComponents() {
        setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        setOrientation(SwingConstants.VERTICAL);
        setToolTipText("Slide to change the dataset opacity");
        
        addChangeListener(e -> {	
        	Chart.setOpacity( (float) (SLIDER_A_COEF*Math.exp(SLIDER_B_COEF*getValue())) );
	        notifyPlot();
        });
	}
	
	public void addPlotRequestListener(PlotRequestListener plotRequestListener) {
		listeners.add(plotRequestListener);
	}
	
	private void notifyPlot() {
		listeners.stream().forEach(l -> l.onPlotRequest());
	}

}