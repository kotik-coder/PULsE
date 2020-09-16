package pulse.ui.components;

import static javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION;
import static javax.swing.SortOrder.ASCENDING;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.SwingUtilities.isRightMouseButton;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericPropertyKeyword.IDENTIFIER;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_ADDED;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_REMOVED;
import static pulse.ui.Messages.getString;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import pulse.properties.NumericProperty;
import pulse.tasks.Identifier;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.logs.LogEntry;
import pulse.tasks.logs.StateEntry;
import pulse.tasks.logs.Status;
import pulse.ui.components.controllers.TaskTableRenderer;

@SuppressWarnings("serial")
public class TaskTable extends JTable {

	private final static int ROW_HEIGHT = 35;
	private final static int HEADER_HEIGHT = 30;

	private TaskTableRenderer taskTableRenderer;
	private TaskPopupMenu menu;

	private Comparator<NumericProperty> numericComparator = (i1, i2) -> i1.compareTo(i2);
	private Comparator<Status> statusComparator = (s1, s2) -> s1.compareTo(s2);

	private final static int FONT_SIZE = 14;

	public TaskTable() {
		super();
		taskTableRenderer = new TaskTableRenderer();
		this.setRowSelectionAllowed(true);
		setRowHeight(ROW_HEIGHT);

		setFillsViewportHeight(true);
		setSelectionMode(SINGLE_INTERVAL_SELECTION);
		setShowHorizontalLines(false);

		var model = new TaskTableModel();
		setModel(model);

		var th = new TableHeader(getColumnModel(), new String[] { def(IDENTIFIER).getDescriptor(true),
				def(TEST_TEMPERATURE).getDescriptor(true), def(OPTIMISER_STATISTIC).getDescriptor(true),
				def(TEST_STATISTIC).getDescriptor(true), ("Task status") });

		setTableHeader(th);

		var font = getTableHeader().getFont().deriveFont(FONT_SIZE);
		getTableHeader().setFont(font);
		getTableHeader().setPreferredSize(new Dimension(50, HEADER_HEIGHT));

		setAutoCreateRowSorter(true);
		var sorter = new TableRowSorter<DefaultTableModel>();
		sorter.setModel(model);
		var list = new ArrayList<RowSorter.SortKey>();

		for (var i = 0; i < this.getModel().getColumnCount(); i++) {
			list.add(new RowSorter.SortKey(i, ASCENDING));
			if (i == TaskTableModel.STATUS_COLUMN)
				sorter.setComparator(i, statusComparator);
			else
				sorter.setComparator(i, numericComparator);
		}

		sorter.setSortKeys(list);
		setRowSorter(sorter);

		initListeners();
		menu = new TaskPopupMenu();

	}

	public void initListeners() {

		var instance = TaskManager.getInstance();
		
		/*
		 * task removed/added listener
		 */

		instance.addTaskRepositoryListener((TaskRepositoryEvent e) -> {
			if (e.getState() == TASK_REMOVED) {
				((TaskTableModel) getModel()).removeTask(e.getId());
			} else if (e.getState() == TASK_ADDED) {
				((TaskTableModel) getModel()).addTask(instance.getTask(e.getId()));
			}
		});

		/*
		 * mouse listener
		 */

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {

				if (rowAtPoint(e.getPoint()) < 0)
					return;

				if (rowAtPoint(e.getPoint()) != getSelectedRow())
					return;

				if (isRightMouseButton(e))
					menu.show(e.getComponent(), e.getX(), e.getY());

			}

		});

		/*
		 * selection listener
		 */

		var lsm = getSelectionModel();
		var reference = this;

		lsm.addListSelectionListener((ListSelectionEvent e) -> {
			if (lsm.getValueIsAdjusting())
				return;
			if (lsm.isSelectionEmpty())
				return;
			var id = (Identifier) getValueAt(lsm.getMinSelectionIndex(), 0);
			instance.selectTask(id, reference);
		});

		instance.addSelectionListener((TaskSelectionEvent e) -> {
			// simply ignore call if event is triggered by taskTable
			if (e.getSource() instanceof TaskTable)
				return;
			var id = instance.getSelectedTask().getIdentifier();
			Identifier idFromTable = null;
			for (var i = 0; i < getRowCount(); i++) {
				idFromTable = (Identifier) getValueAt(i, 0);

				if (idFromTable.equals(id)) {
					setRowSelectionInterval(i, i);
					return;
				}
			}
			clearSelection();
		});

	}

	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		return null;
	}

	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		return taskTableRenderer;
	}

	public class TaskTableModel extends DefaultTableModel {

		private static final int SEARCH_STATISTIC_COLUMN = 2;
		private static final int TEST_STATISTIC_COLUMN = 3;
		private static final int STATUS_COLUMN = 4;

		public TaskTableModel() {

			super(new Object[][] {},
					new String[] { def(IDENTIFIER).getAbbreviation(true),
							def(TEST_TEMPERATURE).getAbbreviation(true),
							def(OPTIMISER_STATISTIC).getAbbreviation(true),
							def(TEST_STATISTIC).getAbbreviation(true), getString("TaskTable.Status") });

		}

		public void addTask(SearchTask t) {
			var temperature = t.getExperimentalCurve().getMetadata().numericProperty(TEST_TEMPERATURE);
			var data = new Object[] { 
					t.getIdentifier(), 
					temperature ,
					t.getResidualStatistic().getStatistic(), 
					t.getNormalityTest().getStatistic(), 
					t.getStatus() };

			invokeLater(() -> super.addRow(data));

			t.addStatusChangeListener((StateEntry e) -> {
				setValueAt(e.getState(), searchRow(t.getIdentifier()), STATUS_COLUMN);
				if (t.getNormalityTest() != null)
					setValueAt(t.getNormalityTest().getStatistic(), searchRow(t.getIdentifier()),
							TEST_STATISTIC_COLUMN);
			});

			t.addTaskListener((LogEntry e) -> {
				setValueAt(t.getResidualStatistic().getStatistic(), searchRow(t.getIdentifier()),
						SEARCH_STATISTIC_COLUMN);
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

			if (v.isPresent())
				return data.indexOf(v.get());
			else
				return -1;

		}

	}

	public void removeSelectedRows() {
		var rows = getSelectedRows();
		Identifier id;

		var instance = TaskManager.getInstance();
		
		for (var i = rows.length - 1; i >= 0; i--) {
			id = (Identifier) getValueAt(rows[i], 0);
			instance.removeTask(instance.getTask(id));
		}

		clearSelection();
	}

	private class TableHeader extends JTableHeader {

		private String[] tooltips;

		public TableHeader(TableColumnModel columnModel, String[] columnTooltips) {
			super(columnModel);// do everything a normal JTableHeader does
			this.tooltips = columnTooltips;// plus extra data
		}

		@Override
		public String getToolTipText(MouseEvent e) {
			var p = e.getPoint();
			var index = columnModel.getColumnIndexAtX(p.x);
			var realIndex = columnModel.getColumn(index).getModelIndex();
			return this.tooltips[realIndex];
		}

	}

}