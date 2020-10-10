package pulse.ui.components;

import static java.lang.Math.abs;
import static java.util.stream.Collectors.toList;
import static javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION;
import static javax.swing.SortOrder.ASCENDING;
import static javax.swing.SwingConstants.TOP;
import static javax.swing.SwingUtilities.invokeLater;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.processing.AbstractResult;
import pulse.tasks.processing.AverageResult;
import pulse.tasks.processing.Result;
import pulse.tasks.processing.ResultFormat;
import pulse.ui.components.controllers.NumericPropertyRenderer;
import pulse.ui.components.models.ResultTableModel;
import pulse.util.Descriptive;

@SuppressWarnings("serial")
public class ResultTable extends JTable implements Descriptive {

	private final static int ROW_HEIGHT = 25;
	private final static int RESULTS_HEADER_HEIGHT = 30;

	private NumericPropertyRenderer renderer;

	public ResultTable(ResultFormat fmt) {
		super();

		renderer = new NumericPropertyRenderer();
		renderer.setVerticalAlignment(TOP);

		var model = new ResultTableModel(fmt);
		setModel(model);
		setRowSorter(sorter());

		model.addListener(event -> setRowSorter(sorter()));

		this.setRowHeight(ROW_HEIGHT);
		setShowHorizontalLines(false);
		setFillsViewportHeight(true);

		setSelectionMode(SINGLE_INTERVAL_SELECTION);
		setRowSelectionAllowed(true);
		setColumnSelectionAllowed(false);

		var headersSize = getPreferredSize();
		headersSize.height = RESULTS_HEADER_HEIGHT;
		getTableHeader().setPreferredSize(headersSize);

		/*
		 * Listen to TaskTable and select appropriate results when task selection
		 * changes
		 */

		var instance = TaskManager.getManagerInstance();

		instance.addSelectionListener((TaskSelectionEvent e) -> {
			var t = instance.getSelectedTask();
			getSelectionModel().clearSelection();
			select( t.getCurrentCalculation().getResult() );
		});

		/*
		 * Automatically add finished tasks to this result table Automatically remove
		 * results if corresponding task is removed
		 */

		TaskManager.getManagerInstance().addTaskRepositoryListener((TaskRepositoryEvent e) -> {
			var t = instance.getTask(e.getId());
			switch (e.getState()) {
			case TASK_FINISHED:
				var r = t.getCurrentCalculation().getResult();
				invokeLater(() -> ((ResultTableModel) getModel()).addRow(r));
				break;
			case TASK_REMOVED:
			case TASK_RESET:
				((ResultTableModel) getModel()).removeAll(e.getId());
				getSelectionModel().clearSelection();
				break;
			case BEST_MODEL_SELECTED :
				for(var c : t.getStoredCalculations())
					if(c.getResult() != null && c != t.getCurrentCalculation())
						((ResultTableModel) getModel()).remove(c.getResult());
				this.select(t.getCurrentCalculation().getResult());
				break;
			case TASK_MODEL_SWITCH :
				var c = t.getCurrentCalculation();
				this.getSelectionModel().clearSelection();
				if(c != null && c.getResult() != null) 
					select(c.getResult());
				break;
			default:
				break;
			}
		});

	}

	public void clear() {
		var model = (ResultTableModel) getModel();
		model.clear();
	}

	private TableRowSorter<ResultTableModel> sorter() {
		var sorter = new TableRowSorter<ResultTableModel>((ResultTableModel) getModel());
		var list = new ArrayList<RowSorter.SortKey>();
		Comparator<NumericProperty> numericComparator = (i1, i2) -> i1.compareTo(i2);

		for (var i = 0; i < getColumnCount(); i++) {
			list.add(new RowSorter.SortKey(i, ASCENDING));
			sorter.setComparator(i, numericComparator);
		}

		sorter.setSortKeys(list);
		sorter.sort();
		return sorter;
	}

	public double[][][] data() {
		var data = new double[getColumnCount()][2][getRowCount()];
		NumericProperty property = null;

		for (var i = 0; i < data.length; i++) {
			for (var j = 0; j < data[0][0].length; j++) {
				property = (NumericProperty) getValueAt(j, i);
				data[i][0][j] = ((Number) property.getValue()).doubleValue()
						* property.getDimensionFactor().doubleValue();
				data[i][1][j] = property.getError() == null ? 0
						: property.getError().doubleValue() * property.getDimensionFactor().doubleValue();
			}
		}
		return data;
	}

	private void scrollToSelection(int rowIndex) {
		scrollRectToVisible(getCellRect(rowIndex, rowIndex, true));
	}

	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		var value = getValueAt(row, column);

		if (value instanceof NumericProperty)
			return renderer;

		return super.getCellRenderer(row, column);

	}

	/*
	 * Merges data withing a temperature interval
	 */

	public void merge(double temperatureDelta) {
		var model = (ResultTableModel) this.getModel();
		var temperatureIndex = model.getFormat().indexOf(TEST_TEMPERATURE);

		if (temperatureIndex < 0)
			return;

		List<AbstractResult> newRows = new LinkedList<>();
		List<Integer> skipList = new ArrayList<>();

		for (var i = 0; i < this.getRowCount(); i++) {
			if (skipList.contains(convertRowIndexToModel(i)))
				continue; // check if value is independent (does not belong to a group)

			var val = ((Number) ((Property) this.getValueAt(i, temperatureIndex)).getValue());

			var indices = group(val.doubleValue(), temperatureIndex, temperatureDelta); // get indices of results in table
			skipList.addAll(indices); // skip those indices if they refer to the same group

			if (indices.size() < 2)
				newRows.add(model.getResults().get(indices.get(0)));
			else
				newRows.add(new AverageResult(indices.stream().map(model.getResults()::get).collect(toList()),
						model.getFormat()));

		}

		invokeLater(() -> {
			model.setRowCount(0);
			model.getResults().clear();
			newRows.stream().forEach(r -> model.addRow(r));
		});

	}

	public List<Integer> group(double val, int index, double precision) {

		List<Integer> selection = new ArrayList<>();

		for (var i = 0; i < getRowCount(); i++) {

			var valNumber = (Number) ((Property) getValueAt(i, index)).getValue();

			if (abs(valNumber.doubleValue() - val) < precision)
				selection.add(convertRowIndexToModel(i));

		}

		return selection;

	}

	// Implement table header tool tips.
	@Override
	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {
			@Override
			public String getToolTipText(MouseEvent e) {
				var index = columnModel.getColumnIndexAtX(e.getPoint().x);
				var realIndex = columnModel.getColumn(index).getModelIndex();
				return ((ResultTableModel) getModel()).getTooltips().get(realIndex);
			}
		};
	}

	@Override
	public String describe() {
		return "SummaryTable";
	}

	public boolean isSelectionEmpty() {
		return getSelectedRows().length < 1;
	}

	public boolean hasEnoughElements(int elements) {
		return getRowCount() >= elements;
	}

	public void deleteSelected() {

		invokeLater(() -> {
		var rtm = (ResultTableModel) getModel();
		var selection = getSelectedRows();

		if (selection.length < 0)
			return;

		for (var i = selection.length - 1; i >= 0; i--) {
			rtm.remove(rtm.getResults().get(convertRowIndexToModel(selection[i])));
		}});

	}
	
	public void select(Result r) {  
		invokeLater(() -> {
		var results = ((ResultTableModel) getModel()).getResults();
		if(results.contains(r)) {
			
			int jj = convertRowIndexToView(results.indexOf(r));

			if (jj > -1) {
				getSelectionModel().addSelectionInterval(jj, jj);
				scrollToSelection(jj);
			}
			
		}});
	}

	public void undo() {
		invokeLater(() -> {
		var dtm = (ResultTableModel) getModel();

		for (var i = dtm.getRowCount() - 1; i >= 0; i--) {
			dtm.remove(dtm.getResults().get(convertRowIndexToModel(i)));
		}

		var instance = TaskManager.getManagerInstance();
		instance.getTaskList().stream().map(t -> t.getCurrentCalculation().getResult()).forEach(r -> dtm.addRow(r));
	});

	}
	
}