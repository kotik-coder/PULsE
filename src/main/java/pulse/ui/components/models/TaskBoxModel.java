package pulse.ui.components.models;

import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_ADDED;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_REMOVED;
import static pulse.ui.Messages.getString;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import pulse.tasks.Identifier;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;

/*
 * BASED ON DefaultComboBoxModel
 */

public class TaskBoxModel extends AbstractListModel<SearchTask> implements ComboBoxModel<SearchTask> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5394433933807306979L;
	protected SearchTask selectedTask;

	public TaskBoxModel() {
		var instance = TaskManager.getInstance();
		selectedTask = instance.getSelectedTask();

		instance.addTaskRepositoryListener((TaskRepositoryEvent e) -> {
			if (e.getState() == TASK_ADDED) {
				notifyTaskAdded(e.getId());
			}
			if (e.getState() == TASK_REMOVED) {
				notifyTaskRemoved(e.getId());
			}
		});

	}

	@Override
	public int getSize() {
		return TaskManager.getInstance().numberOfTasks();
	}

	@Override
	public SearchTask getElementAt(int index) {
		return TaskManager.getInstance().getTaskList().get(index);
	}

	@Override
	public void setSelectedItem(Object anItem) {
		// No item is selected and object is null, so no change required.
		if (selectedTask == null && anItem == null)
			return;

		if (!(anItem instanceof SearchTask))
			throw new IllegalArgumentException(getString("TaskBoxModel.WrongClassError")); //$NON-NLS-1$

		// object is already selected so no change required.
		if (selectedTask != null && selectedTask.equals(anItem))
			return;

		// Simply return if object is not in the list.
		if (selectedTask != null && !TaskManager.getInstance().getTaskList().contains(anItem))
			return;

		// Here we know that object is either an item in the list or null.
		// Handle the three change cases: selectedItem is null, object is
		// non-null; selectedItem is non-null, object is null;
		// selectedItem is non-null, object is non-null and they're not
		// equal.
		selectedTask = (SearchTask) anItem;
		fireContentsChanged(this, -1, -1);
	}

	public int getSelectedIndex() {
		return TaskManager.getInstance().getTaskList().indexOf(selectedTask);
	}

	@Override
	public Object getSelectedItem() {
		return selectedTask;
	}

	private void notifyTaskAdded(Identifier id) {
		var index = (int) id.getValue();
		fireIntervalAdded(this, index, index);
	}

	private void notifyTaskRemoved(Identifier id) {
		var index = (int) id.getValue();
		fireIntervalRemoved(this, index, index);
	}

}