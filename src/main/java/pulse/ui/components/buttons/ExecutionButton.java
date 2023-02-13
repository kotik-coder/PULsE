package pulse.ui.components.buttons;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.tasks.logs.Status.INCOMPLETE;
import static pulse.ui.Messages.getString;
import static pulse.ui.components.buttons.ExecutionButton.ExecutionState.EXECUTE;
import static pulse.ui.components.buttons.ExecutionButton.ExecutionState.STOP;
import static pulse.util.ImageUtils.loadIcon;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;

@SuppressWarnings("serial")
public class ExecutionButton extends JButton {

    private ExecutionState state = EXECUTE;

    public ExecutionButton() {
        super();
        setIcon(state.getIcon());
        setToolTipText(state.getMessage());

        this.addActionListener((ActionEvent e) -> {
            var instance = TaskManager.getManagerInstance();

            /*
             * STOP PRESSED?
             */
            if (state == STOP) {
                instance.cancelAllTasks();
                return;
            }
            /*
             * EXECUTE PRESSED?
             */
            if (instance.getTaskList().isEmpty()) {
                showMessageDialog(getWindowAncestor((Component) e.getSource()),
                        getString("TaskControlFrame.PleaseLoadData"), //$NON-NLS-1$
                        "No Tasks", //$NON-NLS-1$
                        ERROR_MESSAGE);
                return;
            }
            var problematicTask = instance.getTaskList().stream().filter(t -> {
                t.checkProblems();
                return t.getStatus() == INCOMPLETE;
            }).findFirst();
            if (problematicTask.isPresent()) {
                var t = problematicTask.get();
                showMessageDialog(getWindowAncestor((Component) e.getSource()),
                        t + " is " + t.getStatus().getMessage(), "Problems found",
                        ERROR_MESSAGE);
            } else {
                instance.executeAll();
            }
        });

        resetSession();

    }
    
    public void resetSession() {
        var instance = TaskManager.getManagerInstance();
        instance.addTaskRepositoryListener((TaskRepositoryEvent e) -> {
            switch (e.getState()) {
                case TASK_SUBMITTED:
                    setExecutionState(STOP);
                    break;
                case TASK_FINISHED:
                    if (instance.isTaskQueueEmpty()) {
                        setExecutionState(EXECUTE);
                    } else {
                        setExecutionState(STOP);
                    }
                    break;
                case SHUTDOWN:
                    setExecutionState(EXECUTE);
                    break;
                default:
                    return;
            }
        });
    }

    public void setExecutionState(ExecutionState state) {
        this.state = state;
        this.setToolTipText(state.getMessage());
        this.setIcon(state.getIcon());
    }

    public ExecutionState getExecutionState() {
        return state;
    }

    public enum ExecutionState {
        EXECUTE("Execute All Tasks", loadIcon("execute.png", 24)),
        STOP("Terminate All Running Tasks", loadIcon("stop.png", 24));

        private String message;
        private ImageIcon icon;

        private ExecutionState(String message, ImageIcon icon) {
            this.icon = icon;
            this.message = message;
        }

        public ImageIcon getIcon() {
            return icon;
        }

        public String getMessage() {
            return message;
        }

    }

}
