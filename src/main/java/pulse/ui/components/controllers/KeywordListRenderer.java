package pulse.ui.components.controllers;

import static pulse.properties.NumericProperties.def;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import pulse.properties.NumericPropertyKeyword;

public class KeywordListRenderer extends DefaultListCellRenderer {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public KeywordListRenderer() {
        super();
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {

        var renderer = super.getListCellRendererComponent(list,
                (def((NumericPropertyKeyword) value).getDescriptor(true)), index, cellHasFocus, cellHasFocus);

        return renderer;

    }

}
