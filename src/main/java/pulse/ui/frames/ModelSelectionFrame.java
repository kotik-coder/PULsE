package pulse.ui.frames;

import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_BROWSING_REQUEST;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;

import pulse.tasks.TaskManager;
import pulse.ui.components.CalculationTable;
import pulse.ui.components.panels.ModelToolbar;

@SuppressWarnings("serial")
public class ModelSelectionFrame extends JInternalFrame {

    private CalculationTable table;

    public ModelSelectionFrame() {
        super("Model Comparison", true, true, true, true);
        table = new CalculationTable();
        getContentPane().add(new JScrollPane(table));
        setSize(new Dimension(400, 400));
        setTitle("Stored Calculations");
        getContentPane().add(new ModelToolbar(), BorderLayout.SOUTH);
        var instance = TaskManager.getManagerInstance();
        instance.addTaskRepositoryListener(e -> {
            if (e.getState() == TASK_BROWSING_REQUEST) {
                table.update(instance.getTask(e.getId()));
            }
        });
        this.setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

}
