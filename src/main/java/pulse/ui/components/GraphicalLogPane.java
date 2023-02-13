package pulse.ui.components;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import static pulse.properties.NumericPropertyKeyword.ITERATION;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.logs.AbstractLogger;
import pulse.tasks.logs.DataLogEntry;
import pulse.tasks.logs.Log;
import pulse.tasks.logs.LogEntry;
import static pulse.tasks.logs.Status.DONE;

@SuppressWarnings("serial")
public class GraphicalLogPane extends AbstractLogger {

    private final LogChart chart;
    private final JScrollPane pane;

    public GraphicalLogPane() {
        pane = new JScrollPane();
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        chart = new LogChart();
        pane.setViewportView(chart.getChartPanel());
        TaskManager.getManagerInstance().addTaskRepositoryListener(e -> {
            if (e.getState() == TaskRepositoryEvent.State.TASK_SUBMITTED) {
                chart.clear();
            }
        });
    }

    @Override
    public JComponent getGUIComponent() {
        return pane;
    }

    @Override
    public void printTimeTaken(Log log) {
        long[] time = log.timeTaken();
        StringBuilder sb = new StringBuilder("Finished in ");
        sb.append(time[0]).append(" s ").append(time[1]).append(" ms.");
    }

    @Override
    public void post(LogEntry logEntry) {
        if (logEntry instanceof DataLogEntry) {
            var dle = (DataLogEntry) logEntry;
            double iteration = dle.getData().stream()
                    .filter(p -> p.getIdentifier().getKeyword() == ITERATION)
                    .findAny().get().getApparentValue();

            chart.changeAxis(true);
            chart.plot((DataLogEntry) logEntry, iteration);

        }
    }

    @Override
    public void postAll() {
        clear();

        var task = TaskManager.getManagerInstance().getSelectedTask();

        if (task != null) {

            var log = task.getLog();

            if (log.isStarted() && log.isFinished()) {

                chart.clear();
                chart.changeAxis(false);
                chart.plot(log);

                if (task.getStatus() == DONE) {
                    printTimeTaken(log);
                }

            }

        }

    }

    @Override
    public void post(String text) {
        //not supported
    }

    @Override
    public void clear() {
        chart.clear();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

}
