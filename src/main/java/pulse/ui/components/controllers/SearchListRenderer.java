package pulse.ui.components.controllers;

import static javax.swing.BorderFactory.createEmptyBorder;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;

@SuppressWarnings("serial")
public class SearchListRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {

        var renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        ((JComponent) renderer).setBorder(createEmptyBorder(10, 10, 10, 10));

        return renderer;

    }

}
