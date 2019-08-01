package pulse.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import pulse.properties.NumericProperty;

public class NumericPropertyRenderer extends DefaultTableCellRenderer {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -501909930818738712L;
	private static final Color LIGHT_BLUE = new Color(175, 238, 238);
	
	public NumericPropertyRenderer() {
		super();
	}
	
	@Override
	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {			
		
		if(value instanceof NumericProperty) {
			JFormattedTextField jtf = initTextField(((NumericProperty) value).formattedValue(), table.isRowSelected(row));	
			return jtf;
			
		}
		
		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			
    }
	
	protected static JFormattedTextField initTextField(String text, boolean rowSelected) {
		JFormattedTextField jtf = new JFormattedTextField(text);
		jtf.setOpaque(true);
		jtf.setBorder(null);			
		jtf.setHorizontalAlignment(JFormattedTextField.CENTER);					
		
		Font f = new Font(Messages.getString("NumericPropertyRenderer.FontName"), Font.PLAIN, 16);  //$NON-NLS-1$
		jtf.setFont(f);					
		
		if(rowSelected)
			jtf.setBackground(LIGHT_BLUE);
		else
			jtf.setBackground(Color.white);
		
		return jtf;
	}
	
}
