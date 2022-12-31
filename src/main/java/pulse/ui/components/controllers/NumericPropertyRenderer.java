package pulse.ui.components.controllers;

import java.awt.Component;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyFormatter;

@SuppressWarnings("serial")
public class NumericPropertyRenderer extends DefaultTableCellRenderer {

    public NumericPropertyRenderer() {
        super();
    }

    @Override

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        Component result = null;

        if (value instanceof NumericProperty) {
            var jtf = initTextField((NumericProperty) value, table.isRowSelected(row));
            if (table.getEditorComponent() != null) {
                result = jtf;
            } else {
                result = (JLabel) super.getTableCellRendererComponent(table,
                        jtf.getText(), isSelected, hasFocus, row, column);
                ((JLabel) result).setHorizontalAlignment(RIGHT);
            }
        } else {
            var superRenderer = (JLabel) super.getTableCellRendererComponent(table,
                    value, isSelected, hasFocus, row, column);
            superRenderer.setHorizontalAlignment(JLabel.LEFT);
            result = superRenderer;
        }

        
        result.setForeground(UIManager.getColor("List.foreground"));
        return result;

    }

    private static JFormattedTextField initTextField(NumericProperty np, boolean rowSelected) {
        var jtf = new JFormattedTextField(new NumericPropertyFormatter(np, true, true));
        jtf.setValue(np);
        jtf.setHorizontalAlignment(RIGHT);
        return jtf;
    }

}
