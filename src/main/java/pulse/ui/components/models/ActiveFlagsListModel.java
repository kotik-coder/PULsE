package pulse.ui.components.models;

import static pulse.tasks.processing.ResultFormat.getMinimalArray;

import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;

import pulse.properties.NumericPropertyKeyword;

public class ActiveFlagsListModel extends DefaultListModel<NumericPropertyKeyword> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private List<NumericPropertyKeyword> elements = new ArrayList<NumericPropertyKeyword>();
    private final List<NumericPropertyKeyword> referenceList;
    private NumericPropertyKeyword[] mandatorySelection;

    public ActiveFlagsListModel(List<NumericPropertyKeyword> keys, NumericPropertyKeyword[] mandatorySelection) {
        super();
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
    public int getSize() {
        return elements.size();
    }

    @Override
    public NumericPropertyKeyword getElementAt(int i) {
        return elements.get(i);
    }

    @Override
    public void addElement(NumericPropertyKeyword key) {
        elements.add(key);
        var size = this.getSize();
        this.fireIntervalAdded(key, size, size);
    }

    @Override
    public boolean removeElement(Object obj) {
        if (!(obj instanceof NumericPropertyKeyword)) {
            return false;
        }

        var key = (NumericPropertyKeyword) obj;

        if (!elements.contains(key)) {
            return false;
        }

        for (var keyMin : mandatorySelection) {
            if (key == keyMin) {
                return false;
            }
        }
        var index = elements.indexOf(key);
        elements.remove(key);
        this.fireIntervalRemoved(key, index, index);
        return true;
    }

    @Override
    public boolean contains(Object obj) {
        if (!(obj instanceof NumericPropertyKeyword)) {
            return false;
        }

        var key = (NumericPropertyKeyword) obj;
        return elements.contains(key);
    }

    public List<NumericPropertyKeyword> getData() {
        return elements;
    }

}
