package pulse.ui.components.controllers;

import java.util.Comparator;

import pulse.properties.NumericProperty;

public class NumericPropertyComparator implements Comparator<NumericProperty> {

    protected NumericPropertyComparator() {

    }

    @Override
    public int compare(NumericProperty o1, NumericProperty o2) {
        var v1 = (Double) o1.getValue();
        var v2 = (Double) o2.getValue();

        return v1.compareTo(v2);
    }

}
