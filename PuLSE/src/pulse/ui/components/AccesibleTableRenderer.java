package pulse.ui.components;

import java.awt.Component;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.util.PropertyHolder;

public class AccesibleTableRenderer extends NumericPropertyRenderer {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2269077862064919825L;

	public AccesibleTableRenderer() {
		super();
	}
	
	@Override
	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		
		if(value instanceof NumericProperty) 
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		
		if(value instanceof Flag) {
			IconCheckBox btn = new IconCheckBox( (boolean)((Flag)value).getValue() );
			btn.setHorizontalAlignment(CENTER);
			return btn;
		}
		
		if(value instanceof PropertyHolder) {
			JButton button = initButton(value.toString());
			return button;  
		}
		
		if(value instanceof Property) {
			JFormattedTextField jtf = initTextField(value.toString(), table.isRowSelected(row));
			return jtf;
		}
		
		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    
	}
	
	private JButton initButton(String str) {
		JButton button = new JButton(str);
		button.setContentAreaFilled(false);
		button.setFont(button.getFont().deriveFont(9.0f));
		return button;
	}
	
}	