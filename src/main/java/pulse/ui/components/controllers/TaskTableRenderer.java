package pulse.ui.components.controllers;

import static java.awt.Font.BOLD;

import java.awt.Component;

import javax.swing.JTable;

import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.tasks.Identifier;
import pulse.tasks.logs.Status;
import pulse.util.PropertyHolder;

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
			return initLabel("" + ((Property) value).getValue(), table.isRowSelected(row));

		else if (value instanceof Status) {

			var lab = initLabel(value.toString(), table.isRowSelected(row));
			lab.setForeground(((Status) value).getColor());
			lab.setFont(lab.getFont().deriveFont(BOLD));

			return lab;

		}
		
		else if(value instanceof PropertyHolder) {
			return initLabel("" + ((PropertyHolder)value).describe(), table.isRowSelected(row));
		}

		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	}

}