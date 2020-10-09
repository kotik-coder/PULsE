package pulse.ui.components.controllers;

import static java.awt.Color.RED;

import java.awt.Component;

import javax.swing.JButton;
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
		
		var selectedBackground = UIManager.getColor("Table.selectionBackground");
		var deselectedBackground = UIManager.getColor("Table.bakground");
		Component renderer = null;
		
		if (value instanceof NumericProperty) 
			renderer = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		else if (value instanceof Flag) { 
			renderer = new IconCheckBox((boolean) ((Property) value).getValue());
			((IconCheckBox)renderer).setHorizontalAlignment(CENTER);
		}

		else if (value instanceof PropertyHolder) 
			renderer = initButton(value.toString());
		
		else if (value instanceof Property) {
			renderer = initTextField(value.toString(), table.isRowSelected(row));
			renderer.setForeground(RED);
		}
		
		if(renderer != null) {
			renderer.setBackground(isSelected ? selectedBackground : deselectedBackground);
			return renderer;
		}
		else
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	}

	private JButton initButton(String str) {
		var button = new JButton(str);
		button.setToolTipText(str);
		return button;
	}

}