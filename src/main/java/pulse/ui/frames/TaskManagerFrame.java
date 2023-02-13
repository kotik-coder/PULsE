package pulse.ui.frames;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.PAGE_START;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;

import pulse.tasks.TaskManager;
import pulse.ui.components.TaskTable;
import pulse.ui.components.listeners.TaskActionListener;
import pulse.ui.components.models.TaskTableModel;
import pulse.ui.components.panels.TaskToolbar;

@SuppressWarnings("serial")
public class TaskManagerFrame extends JInternalFrame {

    private TaskTable taskTable;
    private TaskToolbar taskToolbar;

    public TaskManagerFrame() {
        super("Task Manager", true, false, true, true);
        initComponents();
        adjustEnabledControls();
        manageListeners();
        setVisible(true);
    }

    public void resetSession() {
        taskTable.resetSession();
        taskToolbar.resetSession();
        adjustEnabledControls();
    }

    private void manageListeners() {
        taskToolbar.addTaskActionListener(new TaskActionListener() {

            @Override
            public void onRemoveRequest() {
                taskTable.removeSelectedRows();
            }

            @Override
            public void onClearRequest() {
                TaskManager.getManagerInstance().clear();
            }

            @Override
            public void onResetRequest() {
                // no new actions
            }

            @Override
            public void onGraphRequest() {
                // no new actions
            }

        });
    }

    private void initComponents() {
        var taskScrollPane = new JScrollPane();
        taskTable = new TaskTable();
        taskScrollPane.setViewportView(taskTable);
        getContentPane().add(taskScrollPane, CENTER);
        taskToolbar = new TaskToolbar();
        getContentPane().add(taskToolbar, PAGE_START);
    }
    
    private void enableIfNeeded() {
        var ttm = (TaskTableModel) taskTable.getModel();

        boolean enabled = ttm.getRowCount() > 0;
        taskToolbar.setClearEnabled(enabled);
        taskToolbar.setResetEnabled(enabled);
        taskToolbar.setExecEnabled(enabled);
    }

    private void adjustEnabledControls() {
        var ttm = (TaskTableModel) taskTable.getModel();

        enableIfNeeded();
        ttm.addTableModelListener((TableModelEvent arg0) -> enableIfNeeded() );

        taskTable.getSelectionModel().addListSelectionListener((ListSelectionEvent arg0) -> {
            var selection = taskTable.getSelectedRows();
            if (taskTable.getSelectedRow() < 0) {
                taskToolbar.setRemoveEnabled(false);
                taskToolbar.setGraphEnabled(false);
            } else {
                if (selection.length > 1) {
                    taskToolbar.setRemoveEnabled(false);
                    taskToolbar.setGraphEnabled(false);
                } else if (selection.length > 0) {
                    taskToolbar.setRemoveEnabled(true);
                    taskToolbar.setGraphEnabled(true);
                }
            }
        });
    }

    public TaskToolbar getTaskToolbar() {
        return taskToolbar;
    }

}
