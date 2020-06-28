package pulse.ui.components.controllers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.ui.Messages;

@SuppressWarnings("serial")
public class NumericPropertyRenderer extends DefaultTableCellRenderer {

	protected static final Color LIGHT_BLUE = new Color(175, 238, 238);
	private final static Font font = new Font(Messages.getString("PropertyHolderTable.FontName"), Font.PLAIN, 14);

	public NumericPropertyRenderer() {
		super();
	}

	@Override

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		if (value instanceof NumericProperty)
			return initTextField(((Property) value).formattedValue(), table.isRowSelected(row));

		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	}

	protected static JFormattedTextField initTextField(String text, boolean rowSelected) {
		JFormattedTextField jtf = new JFormattedTextField(text);
		jtf.setOpaque(true);
		jtf.setBorder(null);
		jtf.setHorizontalAlignment(SwingConstants.CENTER);
		jtf.setFont(font);

		if (rowSelected)
			jtf.setBackground(LIGHT_BLUE);
		else
			jtf.setBackground(Color.white);

		return jtf;
	}

}
