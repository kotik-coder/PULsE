package pulse.ui.components;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static pulse.ui.frames.MainGraphFrame.getChart;

import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableCellRenderer;

import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.ui.components.controllers.TaskTableRenderer;
import pulse.ui.components.models.StoredCalculationTableModel;

@SuppressWarnings("serial")
public class CalculationTable extends JTable {

	private final static int ROW_HEIGHT = 70;
	private final static int HEADER_HEIGHT = 30;

	private TaskTableRenderer taskTableRenderer;

	public CalculationTable() {
		super();
		setDefaultEditor(Object.class, null);
		taskTableRenderer = new TaskTableRenderer();
		this.setRowSelectionAllowed(true);
		setRowHeight(ROW_HEIGHT);

		setFillsViewportHeight(true);
		setSelectionMode(SINGLE_SELECTION);
		setShowHorizontalLines(false);

		var model = new StoredCalculationTableModel();
		setModel(model);

		getTableHeader().setPreferredSize(new Dimension(50, HEADER_HEIGHT));

		setAutoCreateRowSorter(false);
		initListeners();

		var instance = TaskManager.getManagerInstance();
		instance.addTaskRepositoryListener(e -> {

			if (e.getState() == TaskRepositoryEvent.State.TASK_MODEL_SWITCH) {
				var t = instance.getTask(e.getId());
				identifySelection(t);
			}
			
			else if(e.getState() == TaskRepositoryEvent.State.TASK_CRITERION_SWITCH) {
				update(TaskManager.getManagerInstance().getSelectedTask());
			}

		});
		
	}

	public void update(SearchTask t) {
		if (t != null)
			SwingUtilities.invokeLater(() -> {
				((StoredCalculationTableModel) getModel()).update(t);
				identifySelection(t);
			});
	}

	public void identifySelection(SearchTask t) {
		int modelIndex = t.getStoredCalculations().indexOf(t.getCurrentCalculation());
		if (modelIndex > -1)
			this.getSelectionModel().setSelectionInterval(modelIndex, modelIndex);
	}

	public void initListeners() {

		/*
		 * selection listener
		 */

		var lsm = getSelectionModel();

		lsm.addListSelectionListener((ListSelectionEvent e) -> {
			var task = TaskManager.getManagerInstance().getSelectedTask();
			if (!lsm.getValueIsAdjusting() && !lsm.isSelectionEmpty()) {
				var id = convertRowIndexToModel(this.getSelectedRow());
				if (id < task.getStoredCalculations().size()) {
					task.switchTo(task.getStoredCalculations().get(id));
					getChart().plot(task, true);
				}
			}
		});

	}

	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		return taskTableRenderer;
	}

}