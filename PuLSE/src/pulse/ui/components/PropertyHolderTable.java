package pulse.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SortOrder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import pulse.input.Pulse;
import pulse.properties.BooleanProperty;
import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.ui.frames.DataFrame;
import pulse.util.PropertyHolder;

public class PropertyHolderTable extends JTable {

	private PropertyHolder propertyHolder;
	
	private static final long serialVersionUID = 1L;
	
	private final static Font font = new Font(Messages.getString("PropertyHolderTable.FontName"), Font.PLAIN, 13); //$NON-NLS-1$
	private	final static int ROW_HEIGHT = 50;
	
	private PropertySorter sorter;
	
	public PropertyHolderTable(PropertyHolder p) {
		super();
		
		this.propertyHolder = p;
		
		Object[][] data 	= p == null ? null : p.data();
		
		DefaultTableModel model = new DefaultTableModel(data,
				new String[] {Messages.getString("PropertyHolderTable.ParameterColumn"), Messages.getString("PropertyHolderTable.ValueColumn")} //$NON-NLS-1$ //$NON-NLS-2$
				);
		
		setModel(model);
		
		setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
	
		setShowGrid(false);
		setFont(font);
		setRowHeight(ROW_HEIGHT);
		
		setRowSorter(sorter = new PropertySorter(model));
		sorter.sort();
		
		model.addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {
				int row    = e.getFirstRow();
		        int column = e.getColumn();
				
		        if( (row < 0) || (column < 0)) 
					return;	
		      
				Object changedObject = (((DefaultTableModel)e.getSource()).getValueAt(row, column));
				
				if(! (changedObject instanceof Property))  
					return;
				
				Property changedProperty = (Property)changedObject;
				
				try {
					propertyHolder.updateProperty(changedProperty);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
					System.err.println(Messages.getString("PropertyHolderTable.UpdateError") + propertyHolder); //$NON-NLS-1$
					e1.printStackTrace();
				}
				
			}
			
		});	
		
	}	
	
	public void setPropertyHolder(PropertyHolder propertyHolder) {
		this.propertyHolder = propertyHolder;
		if(propertyHolder != null)
			updateTable();	
	}
	
	protected void updateTable() {
		if(propertyHolder == null)
			return;
		
		this.editCellAt(-1, -1);
		this.clearSelection();
		
		Object[][] data = propertyHolder.data();
			
		DefaultTableModel model = ((DefaultTableModel) getModel());
		model.setDataVector(data, new String[]{model.getColumnName(0), model.getColumnName(1)});
	
		sorter.reset();
		sorter.sort();
		
	}
	
	@Override
	public TableCellEditor getCellEditor(int row, int column) {
	   
		Object value = super.getValueAt(row, column);
	   
	   if(value != null) {	
		  
		   //do not edit labels
		   
		  if(value instanceof String) 
			  return null;		
		  
		  if(value instanceof NumericProperty)
			  return new NumberEditor( (NumericProperty) value);
		  
		  if(value instanceof JComboBox) 
	          return new DefaultCellEditor((JComboBox<?>)value);
		  
	      if(value instanceof Pulse.PulseShape) 
	    	  return new DefaultCellEditor(new JComboBox<Object>(Pulse.PulseShape.values()));
	    
	      if((value instanceof PropertyHolder)) 
	    	  return new ButtonEditor(
	    			  (AbstractButton) getCellRenderer(row, column).getTableCellRendererComponent(this, value, false, false, row, column),
	    			  (PropertyHolder)value);
	      
	      if((value instanceof BooleanProperty)) {
	    	  JToggleButton btn = new JToggleButton(value.toString());
	    	  return new ButtonEditor(btn, ((BooleanProperty) value).getSimpleName());
	      }

	      return getDefaultEditor(value.getClass());
	      
	   }
	   
	   return super.getCellEditor(row, column);
	   
	}
	
	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		   
		Object value = super.getValueAt(row, column);
		
		return value != null ? new AccesibleTableRenderer() :
							   super.getCellRenderer(row, column);
		  
	}		
	
	protected class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		AbstractButton btn;
		PropertyHolder dat;
		String boolName;
		
		public ButtonEditor(AbstractButton btn, PropertyHolder dat) {
			this.btn = btn;						
			this.dat = dat;
			
			btn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					JFrame dataFrame = new DataFrame(dat, btn);
					dataFrame.setVisible(true);
					dataFrame.addWindowListener(new WindowAdapter() {
						@Override
						public void windowClosed(WindowEvent we) {
							btn.setText(((DataFrame) dataFrame).getDataObject().toString());
						}
					});
				}
			});			
			
		}
		
		public ButtonEditor(AbstractButton btn, String boolName) {
			this.btn = btn;
			this.boolName = boolName;
	    	btn.setContentAreaFilled(false);
			btn.setOpaque(true);

			btn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Boolean b = Boolean.parseBoolean(btn.getText());
					btn.setText(Boolean.toString(!b));										
					btn.setBackground(!b ? new Color(232,232,232) : null);					
				}
				
			});
			
		}

		@Override
		public Object getCellEditorValue() {
			if(dat != null)
				return dat;					
			return new BooleanProperty(boolName, Boolean.parseBoolean(btn.getText()));
		}

		@Override
		public Component getTableCellEditorComponent(JTable arg0, Object value, boolean arg2, int arg3, int arg4) {
			return btn;
		}		
		
		
	}
	
	public PropertyHolder getPropertyHolder() {
		return propertyHolder;
	}
	
	class PropertySorter extends TableRowSorter {
		
		public PropertySorter(DefaultTableModel model) {
			super();
			this.setModel(model);
				
			reset();
		}
		
		public void reset() {
			ArrayList<SortKey> list	= new ArrayList<SortKey>();
		    list.add( new SortKey(0, SortOrder.ASCENDING) );
		    setSortKeys(list);
		}
		
	}
	
}
