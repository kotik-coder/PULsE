package pulse.ui.components.controllers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JTable;

import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.ui.components.buttons.IconCheckBox;
import pulse.util.PropertyHolder;

public class AccessibleTableRenderer extends NumericPropertyRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2269077862064919825L;

	public AccessibleTableRenderer() {
		super();
	}

	@Override

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		if (value instanceof NumericProperty) {
			var renderer = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			renderer.setForeground(Color.BLUE);
			return renderer;
		}

		if (value instanceof Flag) {
			IconCheckBox btn = new IconCheckBox((boolean) ((Property) value).getValue());
			btn.setHorizontalAlignment(CENTER);
			if (isSelected)
				btn.setBackground(LIGHT_BLUE);
			else
				btn.setBackground(Color.WHITE);
			return btn;
		}

		if (value instanceof PropertyHolder) {
			JButton button = initButton(value.toString());
			return button;
		}

		if (value instanceof Property) {
			JFormattedTextField jtf = initTextField(value.toString(), table.isRowSelected(row));
			jtf.setForeground(Color.RED);
			jtf.setFont(jtf.getFont().deriveFont(Font.BOLD));
			return jtf;
		}

		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	}

	private JButton initButton(String str) {
		JButton button = new JButton(str);
		button.setContentAreaFilled(true);
		button.setBackground(Color.LIGHT_GRAY);
		button.setFont(button.getFont().deriveFont(12.0f).deriveFont(Font.ITALIC));
		button.setToolTipText(str);
		return button;
	}

}