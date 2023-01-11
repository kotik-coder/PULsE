package pulse.ui.components.controllers;


import java.awt.Component;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;

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

        Component result = null;
        
        if (value instanceof Flag) {
            result = new IconCheckBox((boolean) ((Property) value).getValue());
            ((IconCheckBox) result).setHorizontalAlignment(CENTER);
        } 
        
        else if (value instanceof PropertyHolder) {
             var sb = new StringBuilder("Click to Edit/View ");
             sb.append(((PropertyHolder) value).getSimpleName());
             sb.append("...");
             result = new JButton(sb.toString());
             ((JButton)result).setToolTipText(value.toString());
             ((JButton)result).setHorizontalAlignment(LEFT);
        }

        else { 
            result = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
        
        return result;
    
    }
    

}