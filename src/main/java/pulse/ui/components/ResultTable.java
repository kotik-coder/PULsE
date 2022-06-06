package pulse.ui.components;

import static javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
import static javax.swing.SortOrder.ASCENDING;
import static javax.swing.SwingConstants.TOP;
import static javax.swing.SwingUtilities.invokeLater;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import pulse.properties.NumericProperty;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.processing.Result;
import pulse.tasks.processing.ResultFormat;
import pulse.ui.components.controllers.NumericPropertyRenderer;
import pulse.ui.components.models.ResultTableModel;
import pulse.util.Descriptive;

@SuppressWarnings("serial")
public class ResultTable extends JTable implements Descriptive {

    private final static int ROW_HEIGHT = 25;
    private final static int RESULTS_HEADER_HEIGHT = 50;

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

        setSelectionMode(MULTIPLE_INTERVAL_SELECTION);
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
            select(t);
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
                    var resultTableModel = (ResultTableModel) getModel();
                    Objects.requireNonNull(r, "Task finished with a null result!");
                    invokeLater(() -> resultTableModel.addRow(r));
                    break;
                case TASK_REMOVED:
                case TASK_RESET:
                    ((ResultTableModel) getModel()).removeAll(e.getId());
                    getSelectionModel().clearSelection();
                    break;
                case BEST_MODEL_SELECTED:
                    for (var c : t.getStoredCalculations()) {
                        if (c.getResult() != null && c != t.getCurrentCalculation()) {
                            ((ResultTableModel) getModel()).remove(c.getResult());
                        }
                    }
                    this.select(t.getCurrentCalculation().getResult());
                    break;
                case TASK_MODEL_SWITCH:
                    var c = t.getCurrentCalculation();
                    this.getSelectionModel().clearSelection();
                    if (c != null && c.getResult() != null) {
                        select(c.getResult());
                    }
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
                        * property.getDimensionFactor().doubleValue() + property.getDimensionDelta().doubleValue();
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

        if (value instanceof NumericProperty) {
            return renderer;
        }

        return super.getCellRenderer(row, column);

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
        return "Summary_" + TaskManager.getManagerInstance().describe();
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

            if (selection.length < 0) {
                return;
            }

            for (var i = selection.length - 1; i >= 0; i--) {
                rtm.remove(rtm.getResults().get(convertRowIndexToModel(selection[i])));
            }
        });

    }

    public void select(Result r) {
        var results = ((ResultTableModel) getModel()).getResults();
        int modelIndex = results.indexOf(r);
        if (modelIndex > -1) {
            int jj = convertRowIndexToView(modelIndex);
            getSelectionModel().addSelectionInterval(jj, jj);
            scrollToSelection(jj);
        }
    }

    public void select(SearchTask t) {
        t.getStoredCalculations().stream().forEach(c -> {
            if (c.getResult() != null) {
                select(c.getResult());
            }
        });
    }

    public void undo() {
        var dtm = (ResultTableModel) getModel();

        for (var i = dtm.getRowCount() - 1; i >= 0; i--) {
            dtm.remove(dtm.getResults().get(convertRowIndexToModel(i)));
        }

        var instance = TaskManager.getManagerInstance();
        instance.getTaskList().stream().map(t -> t.getStoredCalculations()).flatMap(list -> list.stream())
                .filter(Objects::nonNull)
                .forEach(c -> dtm.addRow(c.getResult()));
    }

}
