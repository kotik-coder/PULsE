package pulse.ui.components.panels;

import static java.lang.Math.exp;
import static pulse.ui.frames.MainGraphFrame.*;
import static javax.swing.BorderFactory.createEmptyBorder;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JSlider;

import pulse.ui.components.listeners.PlotRequestListener;

@SuppressWarnings("serial")
public class OpacitySlider extends JSlider {

	private List<PlotRequestListener> listeners;

	private final static float SLIDER_A_COEF = 0.01f;
	private final static float SLIDER_B_COEF = 0.04605f;

	public OpacitySlider() {
		initComponents();
		listeners = new ArrayList<>();
	}

	public void initComponents() {
		setBorder(createEmptyBorder(5, 0, 5, 0));
		setOrientation(VERTICAL);
		setToolTipText("Slide to change the dataset opacity");

		addChangeListener(e -> {
			getChart().setOpacity((float) (SLIDER_A_COEF * exp(SLIDER_B_COEF * getValue())));
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