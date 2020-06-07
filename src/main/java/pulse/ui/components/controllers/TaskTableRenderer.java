package pulse.ui.components.controllers;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JFormattedTextField;
import javax.swing.JTable;

import pulse.properties.NumericProperty;
import pulse.tasks.Identifier;
import pulse.tasks.Status;

@SuppressWarnings("serial")
public class TaskTableRenderer extends NumericPropertyRenderer {

	public TaskTableRenderer() {
		super();
	}

	@Override

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		if (value instanceof NumericProperty)
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		else if (value instanceof Identifier)
			return initTextField("" + ((Identifier) value).getValue(), table.isRowSelected(row));

		else if (value instanceof Status) {

			JFormattedTextField jtf = initTextField(value.toString(), table.isRowSelected(row));
			jtf.setForeground(((Status) value).getColor());
			jtf.setFont(jtf.getFont().deriveFont(Font.BOLD));

			return jtf;

		}

		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	}

}