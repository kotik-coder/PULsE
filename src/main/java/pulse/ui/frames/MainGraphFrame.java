package pulse.ui.frames;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.LINE_END;
import static java.awt.BorderLayout.PAGE_END;
import java.util.concurrent.Executors;

import javax.swing.JInternalFrame;

import pulse.tasks.TaskManager;
import pulse.tasks.logs.Status;
import pulse.ui.components.Chart;
import pulse.ui.components.panels.ChartToolbar;
import pulse.ui.components.panels.OpacitySlider;

@SuppressWarnings("serial")
public class MainGraphFrame extends JInternalFrame {

    private static Chart chart;
    private static MainGraphFrame instance = new MainGraphFrame();

    private MainGraphFrame() {
        super("Time-temperature profile(s)", true, false, true, true);
        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        chart = new Chart();
        var chartPanel = chart.getChartPanel();
        getContentPane().add(chartPanel, CENTER);

        chartPanel.setMaximumDrawHeight(2000);
        chartPanel.setMaximumDrawWidth(2000);
        chartPanel.setMinimumDrawWidth(10);
        chartPanel.setMinimumDrawHeight(10);

        var opacitySlider = new OpacitySlider();
        opacitySlider.addPlotRequestListener(() -> plot());
        getContentPane().add(opacitySlider, LINE_END);
        var chartToolbar = new ChartToolbar();
        chartToolbar.addPlotRequestListener(() -> plot());
        getContentPane().add(chartToolbar, PAGE_END);
    }

    public void plot() {
        var task = TaskManager.getManagerInstance().getSelectedTask();
        //do not plot tasks that are not finished
        if (task != null && task.getCurrentCalculation().getStatus() != Status.IN_PROGRESS) {
            Executors.newSingleThreadExecutor().submit(() -> chart.plot(task, false));
        }
    }

    public static Chart getChart() {
        return chart;
    }

    public static MainGraphFrame getInstance() {
        return instance;
    }

}
