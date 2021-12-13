package pulse.ui.frames;

import static java.awt.BorderLayout.CENTER;

import javax.swing.JInternalFrame;

import pulse.ui.components.AuxPlotter;

@SuppressWarnings("serial")
public class InternalGraphFrame<T> extends JInternalFrame {

    private AuxPlotter<T> chart;

    public InternalGraphFrame(String name, AuxPlotter<T> chart) {
        super(name, true, false, true, true);
        this.chart = chart;
        initComponents(chart);
        setVisible(true);
    }

    private void initComponents(AuxPlotter<T> chart) {
        var chartPanel = chart.getChartPanel();
        getContentPane().add(chartPanel, CENTER);

        chartPanel.setMaximumDrawHeight(2000);
        chartPanel.setMaximumDrawWidth(2000);
        chartPanel.setMinimumDrawWidth(10);
        chartPanel.setMinimumDrawHeight(10);
    }

    public void plot(T t) {
        chart.plot(t);
    }

    public AuxPlotter<T> getChart() {
        return chart;
    }

}
