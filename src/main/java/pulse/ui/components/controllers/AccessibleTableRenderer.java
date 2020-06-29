package pulse.ui.components.controllers;

import static java.awt.Color.BLUE;
import static java.awt.Color.LIGHT_GRAY;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.awt.Font.BOLD;
import static java.awt.Font.ITALIC;

import java.awt.Component;

import javax.swing.JButton;
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
			renderer.setForeground(BLUE);
			return renderer;
		}

		if (value instanceof Flag) {
			var btn = new IconCheckBox((boolean) ((Property) value).getValue());
			btn.setHorizontalAlignment(CENTER);
			if (isSelected)
				btn.setBackground(LIGHT_BLUE);
			else
				btn.setBackground(WHITE);
			return btn;
		}

		if (value instanceof PropertyHolder) {
			var button = initButton(value.toString());
			return button;
		}

		if (value instanceof Property) {
			var jtf = initTextField(value.toString(), table.isRowSelected(row));
			jtf.setForeground(RED);
			jtf.setFont(jtf.getFont().deriveFont(BOLD));
			return jtf;
		}

		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	}

	private JButton initButton(String str) {
		var button = new JButton(str);
		button.setContentAreaFilled(true);
		button.setBackground(LIGHT_GRAY);
		button.setFont(button.getFont().deriveFont(12.0f).deriveFont(ITALIC));
		button.setToolTipText(str);
		return button;
	}

}