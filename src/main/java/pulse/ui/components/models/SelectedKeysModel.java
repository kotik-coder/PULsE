package pulse.ui.components.models;


import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import pulse.properties.NumericProperties;

import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

public class SelectedKeysModel extends DefaultTableModel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private List<NumericPropertyKeyword> elements;
    private final List<NumericPropertyKeyword> referenceList;
    private final NumericPropertyKeyword[] mandatorySelection;

    public SelectedKeysModel(List<NumericPropertyKeyword> keys, NumericPropertyKeyword[] mandatorySelection) {
        super();
        this.elements = new ArrayList<>();
        this.mandatorySelection = mandatorySelection;
        referenceList = keys;
        update();
    }

    public void update() {
        update(referenceList);
    }

    public void update(List<NumericPropertyKeyword> keys) {
        elements.clear();
        elements.addAll(keys);
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
        if(i > -1 && i < getRowCount() && i1 > -1 && i1 < getColumnCount()) {
            var p = NumericProperties.def(elements.get(i));
            return i1 == 0 ? p.getAbbreviation(true) : Messages.getString("TextWrap.2") +
                        p.getDescriptor(false) + Messages.getString("TextWrap.1");
        }
        else
            return null;
    }
    
    public void addElement(NumericPropertyKeyword key) {
        elements.add(key);
        var e = NumericProperties.def(key);
        int index = elements.size() - 1;
        super.fireTableRowsInserted(index, index);
    }
    
    public boolean contains(NumericPropertyKeyword key) {
        return elements.contains(key);
    }
    
    public List<NumericPropertyKeyword> getData() {
        return elements;
    }

    public NumericPropertyKeyword getElementAt(int index) {
        return elements.get(index);
    }
    
    public boolean removeElement(NumericPropertyKeyword key) {

        if (!elements.contains(key)) {
            return false;
        }

        for (var keyMin : mandatorySelection) {
            if (key == keyMin) {
                return false;
            }
        }
        
        var index = elements.indexOf(key);
        super.fireTableRowsDeleted(index, index);
        elements.remove(key);
        return true;
    }

}