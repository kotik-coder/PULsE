package pulse.ui.components.models;

import static javax.swing.SwingUtilities.invokeLater;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericPropertyKeyword.IDENTIFIER;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_ADDED;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_REMOVED;
import static pulse.ui.Messages.getString;

import javax.swing.table.DefaultTableModel;

import pulse.tasks.Identifier;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.logs.LogEntry;
import pulse.tasks.logs.StateEntry;

@SuppressWarnings("serial")
public class TaskTableModel extends DefaultTableModel {

	public static final int SEARCH_STATISTIC_COLUMN = 2;
	public static final int TEST_STATISTIC_COLUMN = 3;
	public static final int STATUS_COLUMN = 4;

	public TaskTableModel() {

		super(new Object[][] {},
				new String[] { def(IDENTIFIER).getAbbreviation(true), def(TEST_TEMPERATURE).getAbbreviation(true),
						def(OPTIMISER_STATISTIC).getAbbreviation(true), def(TEST_STATISTIC).getAbbreviation(true),
						getString("TaskTable.Status") });

		var instance = TaskManager.getManagerInstance();

		/*
		 * task removed/added listener
		 */

		instance.addTaskRepositoryListener((TaskRepositoryEvent e) -> {
			if (e.getState() == TASK_REMOVED)
				removeTask(e.getId());
			else if (e.getState() == TASK_ADDED)
				addTask(instance.getTask(e.getId()));
		});

	}

	public void addTask(SearchTask t) {
		var temperature = t.getExperimentalCurve().getMetadata().numericProperty(TEST_TEMPERATURE);
		var data = new Object[] { t.getIdentifier(), temperature, t.getCurrentCalculation().getOptimiserStatistic().getStatistic(),
				t.getNormalityTest().getStatistic(), t.getCurrentCalculation().getStatus() };

		invokeLater(() -> super.addRow(data));

		t.addStatusChangeListener((StateEntry e) -> {
			setValueAt(e.getState(), searchRow(t.getIdentifier()), STATUS_COLUMN);
			if (t.getNormalityTest() != null)
				setValueAt(t.getNormalityTest().getStatistic(), searchRow(t.getIdentifier()), TEST_STATISTIC_COLUMN);
		});

		t.addTaskListener((LogEntry e) -> {
			setValueAt(t.getCurrentCalculation().getOptimiserStatistic().getStatistic(), searchRow(t.getIdentifier()), SEARCH_STATISTIC_COLUMN);
		});

	}

	public void removeTask(Identifier id) {
		var index = searchRow(id);

		if (index > -1)
			invokeLater(() -> super.removeRow(index));

	}

	public int searchRow(Identifier id) {
		var data = this.getDataVector();
		var v = dataVector.stream().filter(row -> ((Identifier) row.get(0)).equals(id)).findFirst();
		return v.isPresent() ? data.indexOf(v.get()) : -1;
	}

}