package pulse.ui.components;

import java.awt.Component;
import java.awt.Font;
import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import pulse.properties.NumericProperty;
import pulse.tasks.Identifier;
import pulse.tasks.Status;

public class TaskTableRenderer extends NumericPropertyRenderer {		
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3697779016101849934L;

	public TaskTableRenderer() {
		super();
		setVerticalAlignment( SwingConstants.TOP );
	}
	
	@Override
	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {			
		
		if(value instanceof NumericProperty) 
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		
		else if(value instanceof Identifier) {
			
			JFormattedTextField jtf = initTextField("" + ((Identifier) value).getValue(), table.isRowSelected(row));				
			return jtf;
	
		}
		
		else if(value instanceof Status) {
		
			JFormattedTextField jtf = initTextField(value.toString(), table.isRowSelected(row));	
			jtf.setForeground(((Status) value).getColor());
			jtf.setFont(jtf.getFont().deriveFont(Font.BOLD));
			
			return jtf;
			
		}
		
		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			
	}
	
}