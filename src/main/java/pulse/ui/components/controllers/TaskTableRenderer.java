package pulse.ui.components.controllers;

import static java.awt.Font.BOLD;

import java.awt.Component;

import javax.swing.JTable;

import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.tasks.Identifier;
import pulse.tasks.logs.Status;

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
			return initTextField("" + ((Property) value).getValue(), table.isRowSelected(row));

		else if (value instanceof Status) {

			var jtf = initTextField(value.toString(), table.isRowSelected(row));
			jtf.setForeground(((Status) value).getColor());
			jtf.setFont(jtf.getFont().deriveFont(BOLD));

			return jtf;

		}

		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	}

}