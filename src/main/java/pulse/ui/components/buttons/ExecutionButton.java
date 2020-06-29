package pulse.ui.components.buttons;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.tasks.Log.setVerbose;
import static pulse.tasks.Status.INCOMPLETE;
import static pulse.tasks.TaskManager.addTaskRepositoryListener;
import static pulse.tasks.TaskManager.cancelAllTasks;
import static pulse.tasks.TaskManager.execute;
import static pulse.tasks.TaskManager.executeAll;
import static pulse.tasks.TaskManager.getSelectedTask;
import static pulse.tasks.TaskManager.getTaskList;
import static pulse.tasks.TaskManager.isTaskQueueEmpty;
import static pulse.ui.Messages.getString;
import static pulse.ui.components.buttons.ExecutionButton.ExecutionState.EXECUTE;
import static pulse.ui.components.buttons.ExecutionButton.ExecutionState.STOP;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.ui.Launcher;

@SuppressWarnings("serial")
public class ExecutionButton extends JButton {

	private ExecutionState state = EXECUTE;

	public ExecutionButton() {
		super();
		setIcon(state.getIcon());
		setToolTipText(state.getMessage());

		this.addActionListener((ActionEvent e) -> {
            /*
             * STOP PRESSED?
             */
            if (state == STOP) {
                cancelAllTasks();
                return;
            }
            /*
             * EXECUTE PRESSED?
             */
            if (getTaskList().isEmpty()) {
                showMessageDialog(getWindowAncestor((Component) e.getSource()), getString("TaskControlFrame.PleaseLoadData"), //$NON-NLS-1$
                        "No Tasks", //$NON-NLS-1$
                ERROR_MESSAGE);
                return;
            }
            java.util.Optional<pulse.tasks.SearchTask> problematicTask = getTaskList().stream().filter((t) -> t.checkProblems() == INCOMPLETE).findFirst();
            if (problematicTask.isPresent()) {
                var t = problematicTask.get();
                showMessageDialog(getWindowAncestor((Component) e.getSource()), t + " is " + t.getStatus().getMessage(), "Problems found", ERROR_MESSAGE);
            } else {
                if (getTaskList().stream().anyMatch(t -> !t.getProblem().isBatchProcessingEnabled())) {
                    execute(getSelectedTask());
                } else {
                    executeAll();
                    setVerbose(true);
                }
            }
        });

		addTaskRepositoryListener((TaskRepositoryEvent e) -> {
            switch (e.getState()) {
                case TASK_SUBMITTED:
                    setExecutionState(STOP);
                    break;
                case TASK_FINISHED:
                    if (isTaskQueueEmpty()) {
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
		EXECUTE("Execute All Tasks", Launcher.loadIcon("execute.png", 24)),
		STOP("Terminate All Running Tasks", Launcher.loadIcon("stop.png", 24));

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