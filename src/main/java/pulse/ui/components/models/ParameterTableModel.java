package pulse.ui.components.models;

import static pulse.properties.NumericPropertyKeyword.IDENTIFIER;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import pulse.properties.Flag;
import pulse.properties.NumericProperties;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.direction.ActiveFlags;
import pulse.tasks.TaskManager;
import pulse.ui.Messages;

public class ParameterTableModel extends AbstractTableModel {

    protected List<NumericPropertyKeyword> elements;
    private final boolean extendedList;

    public ParameterTableModel(boolean extendedList) {
        super();
        this.elements = new ArrayList<>();
        this.extendedList = extendedList;
    }

    public final void populateWithAllProperties() {
        elements.clear();
        var set = ActiveFlags.availableProperties();
        set.stream().forEach(property -> elements.add(((Flag) property).getType()));
        if (extendedList) {
            elements.add(OPTIMISER_STATISTIC);
            elements.add(TEST_STATISTIC);
            elements.add(IDENTIFIER);
            elements.addAll(TaskManager.getManagerInstance().derivableProperties());
        }
    }

    @Override
    public int getRowCount() {
        return elements != null ? elements.size() : 0;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int i, int i1) {
        if (i > -1 && i < getRowCount() && i1 > -1 && i1 < getColumnCount()) {
            var p = NumericProperties.def(elements.get(i));
            return i1 == 0 ? p.getAbbreviation(true) : Messages.getString("TextWrap.2")
                    + p.getDescriptor(false) + Messages.getString("TextWrap.1");
        } else {
            return null;
        }
    }

    public boolean contains(NumericPropertyKeyword key) {
        return elements.contains(key);
    }

    public NumericPropertyKeyword getElementAt(int index) {
        return elements.get(index);
    }

    public List<NumericPropertyKeyword> getData() {
        return elements;
    }

}
