package pulse.ui.components.controllers;

import java.awt.Component;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import pulse.properties.NumericProperty;
import pulse.properties.Property;

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
			var output = ((Property) value).formattedOutput();
			result = table.getEditorComponent() != null ? 
					initTextField(output, table.isRowSelected(row))
					: initLabel(output, table.isRowSelected(row));
		} else if(value instanceof Number) {
			result = initLabel(value.toString(), table.isRowSelected(row));
		} else
			result = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		return result;

	}

	protected static JFormattedTextField initTextField(String text, boolean rowSelected) {
		var jtf = new JFormattedTextField(text);
		jtf.setHorizontalAlignment(CENTER);
		jtf.setBackground(
				rowSelected ? UIManager.getColor("Table.selectionBackground") : UIManager.getColor("Table.background"));
		return jtf;
	}

	protected static JLabel initLabel(String text, boolean rowSelected) {
		var lab = new JLabel(text);
		lab.setHorizontalAlignment(CENTER);
		lab.setBackground(
				rowSelected ? UIManager.getColor("Table.selectionBackground") : UIManager.getColor("Table.background"));
		return lab;
	}

}
