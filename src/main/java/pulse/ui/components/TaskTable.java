package pulse.ui.components;

import static javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION;
import static javax.swing.SortOrder.ASCENDING;
import static javax.swing.SwingUtilities.isRightMouseButton;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericPropertyKeyword.IDENTIFIER;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;

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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import pulse.properties.NumericProperty;
import pulse.tasks.Identifier;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.logs.Status;
import pulse.ui.components.controllers.TaskTableRenderer;
import pulse.ui.components.models.TaskTableModel;

@SuppressWarnings("serial")
public class TaskTable extends JTable {

	private final static int ROW_HEIGHT = 35;
	private final static int HEADER_HEIGHT = 30;

	private TaskTableRenderer taskTableRenderer;
	private TaskPopupMenu menu;

	private Comparator<NumericProperty> numericComparator = (i1, i2) -> i1.compareTo(i2);
	private Comparator<Status> statusComparator = (s1, s2) -> s1.compareTo(s2);

	public TaskTable() {
		super();
		setDefaultEditor(Object.class, null);
		taskTableRenderer = new TaskTableRenderer();
		this.setRowSelectionAllowed(true);
		setRowHeight(ROW_HEIGHT);

		setFillsViewportHeight(true);
		setSelectionMode(SINGLE_INTERVAL_SELECTION);
		setShowHorizontalLines(false);

		var model = new TaskTableModel();
		setModel(model);

		var th = new TableHeader(getColumnModel());

		setTableHeader(th);

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
		
		var instance = TaskManager.getManagerInstance();
		
		/*
		 * mouse listener
		 */

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {

				if (rowAtPoint(e.getPoint()) >= 0 && rowAtPoint(e.getPoint()) == getSelectedRow() && isRightMouseButton(e)) {
					var task = instance.getSelectedTask();
					menu.getItemViewStored().setEnabled(task.getStoredCalculations().size() > 0);
					menu.show(e.getComponent(), e.getX(), e.getY());
				}

			}

		});

		/*
		 * selection listener
		 */

		var lsm = getSelectionModel();
		var reference = this;
		
		lsm.addListSelectionListener((ListSelectionEvent e) -> {
			if (!lsm.getValueIsAdjusting() && !lsm.isSelectionEmpty()) {
				var id = (Identifier) getValueAt(lsm.getMinSelectionIndex(), 0);
				instance.selectTask(id, reference);
			}
		});

		instance.addSelectionListener((TaskSelectionEvent e) -> {
			// simply ignore call if event is triggered by taskTable
			if (e.getSource() instanceof TaskTable)
				return;
			var id = instance.getSelectedTask().getIdentifier();
			Identifier idFromTable = null;
			int i = 0;
			
			for (i = 0; i < getRowCount() && !id.equals(idFromTable); i++) 
				idFromTable = (Identifier) getValueAt(i, 0);
			
			if(i < getRowCount())
				setRowSelectionInterval(i, i);
			clearSelection();
		});

	}

	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		return taskTableRenderer;
	}

	public void removeSelectedRows() {
		var rows = getSelectedRows();
		Identifier id;

		var instance = TaskManager.getManagerInstance();
		
		for (var i = rows.length - 1; i >= 0; i--) {
			id = (Identifier) getValueAt(rows[i], 0);
			instance.removeTask(instance.getTask(id));
		}

		clearSelection();
	}

	private class TableHeader extends JTableHeader {

		private String[] tooltips;

		public TableHeader(TableColumnModel columnModel) {
			super(columnModel);// do everything a normal JTableHeader does
			tooltips = new String[] { def(IDENTIFIER).getDescriptor(true),
					def(TEST_TEMPERATURE).getDescriptor(true), def(OPTIMISER_STATISTIC).getDescriptor(true),
					def(TEST_STATISTIC).getDescriptor(true), ("Task status")};
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