package pulse.ui.components.controllers;

import static java.awt.Color.RED;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;

import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.ui.components.buttons.IconCheckBox;
import pulse.util.PropertyHolder;

@SuppressWarnings("serial")
public class AccessibleTableRenderer extends NumericPropertyRenderer {

    public AccessibleTableRenderer() {
        super();
    }

    @Override

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        Component renderer = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (value instanceof Flag) {
            renderer = new IconCheckBox((boolean) ((Property) value).getValue());
            ((IconCheckBox) renderer).setHorizontalAlignment(CENTER);
        } else if (value instanceof PropertyHolder) {
            renderer = initButton(value.toString());
        } 
        else if (value instanceof NumericProperty) {
            //default
        }
        else if (value instanceof Property) {
            var label = (JLabel) super.getTableCellRendererComponent(table,
                    ((Property) value).getDescriptor(true), isSelected,
                    hasFocus, row, column);
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            return label;
        }

        return renderer;
    }

    private JButton initButton(String str) {
        var button = new JButton(str);
        button.setToolTipText(str);
        return button;
    }

}
