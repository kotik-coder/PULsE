package pulse.ui.frames;

import static java.awt.BorderLayout.CENTER;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import pulse.ui.components.AuxPlotter;

@SuppressWarnings("serial")
public class ExternalGraphFrame<T> extends JFrame {

    private AuxPlotter<T> chart;

    public ExternalGraphFrame(String name, AuxPlotter<T> chart, final int width, final int height) {
        super(name);
        this.chart = chart;
        initComponents(chart);
        this.setSize(new Dimension(width, height));
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    }

    private void initComponents(AuxPlotter<T> chart) {
        var chartPanel = chart.getChartPanel();
        getContentPane().add(chartPanel, CENTER);
    }

    public void plot(T t) {
        chart.plot(t);
    }

    public AuxPlotter<T> getChart() {
        return chart;
    }

}
