package pulse.ui.components;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;
import pulse.ui.components.buttons.IconCheckBox;
import pulse.ui.components.controllers.AccesibleTableRenderer;
import pulse.ui.components.controllers.NumberEditor;
import pulse.ui.frames.DataFrame;
import pulse.util.Group;
import pulse.util.PropertyHolder;

@SuppressWarnings("serial")
public class PropertyHolderTable extends JTable {

	private PropertyHolder propertyHolder;

	private final static Font font = new Font(Messages.getString("PropertyHolderTable.FontName"), Font.BOLD, 12);
	private	final static int ROW_HEIGHT = 40;
	
	private TableRowSorter<DefaultTableModel> sorter;
	
	public PropertyHolderTable(PropertyHolder p) {
		super();
		
		this.propertyHolder = p;				
		
		DefaultTableModel model = new DefaultTableModel(dataArray(p),
				new String[] {Messages.getString("PropertyHolderTable.ParameterColumn"), Messages.getString("PropertyHolderTable.ValueColumn")} //$NON-NLS-1$ //$NON-NLS-2$
				);
		
		setModel(model);
		
		setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
	
		setShowGrid(false);
		setFont(font);
		setRowHeight(ROW_HEIGHT);	
		
		sorter = new TableRowSorter<DefaultTableModel>();
		sorter.setModel(model);
		ArrayList<SortKey> list	= new ArrayList<SortKey>();
	    list.add( new SortKey(0, SortOrder.ASCENDING) );
	    sorter.setSortKeys(list);		
		sorter.sort();
		
		setRowSorter(sorter);
		
		/*
		 * Update properties of the PropertyHolder when table is changed by the user
		 */
		
		final PropertyHolderTable reference = this;
		
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
				propertyHolder.updateProperty(reference, changedProperty);
				
			}
			
		});
		
		addListeners();
		
	}
	
	private void addListeners() {
		if(propertyHolder == null)
			return;
		
		propertyHolder.addListener( 
					event -> { 
						if(! (event.getSource() instanceof PropertyHolderTable))
							updateTable(); } 
				);		
		
	}
	
	private Object[][] dataArray(PropertyHolder p) {
		if(p == null) 
			return null;
		
		List<Property> data = p.data();				
		List<Group> internalHolders = new ArrayList<Group>(0);
		
		if(p.ignoreSiblings()) {
			try {
				internalHolders = p.subgroups();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
		int size = data.size();
		Object[][] dataArray = new Object[size + internalHolders.size()][2];
		Property property;
		
		int i = 0;
		
		for(Iterator<Property> it = data.iterator(); it.hasNext();i++) {
			property = it.next();
			dataArray[i][0] = property.getDescriptor(true);
			dataArray[i][1] = property;
		}
		
		PropertyHolder propertyHolder;
		
		for(; i < dataArray.length; i++) {
			propertyHolder = (PropertyHolder) internalHolders.get(i-size);
			dataArray[i][0] = propertyHolder.getPrefix() != null ? 
									propertyHolder.getPrefix() : propertyHolder.getDescriptor();
			dataArray[i][1] = propertyHolder;
		}
		
		return dataArray;
	}
	
	public void setPropertyHolder(PropertyHolder propertyHolder) {
		this.propertyHolder = propertyHolder;
		if(propertyHolder != null)
			updateTable();
		addListeners();
	}
	
	public void updateTable() {				
		if(propertyHolder == null)
			return;
				
		this.editCellAt(-1, -1);
		this.clearSelection();		
			
		DefaultTableModel model = ((DefaultTableModel) getModel());
		model.setDataVector(dataArray(propertyHolder), new String[]{model.getColumnName(0), model.getColumnName(1)});
	
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
		  
	      if(value instanceof Enum) 
	    	  return new DefaultCellEditor(new JComboBox<Object>(((Enum<?>)value).getDeclaringClass().getEnumConstants()));
	
	      if((value instanceof PropertyHolder)) 
	    	  return new ButtonEditor( 
	    			  (AbstractButton) getCellRenderer(row, column).getTableCellRendererComponent(this, value, false, false, row, column),
	    			  (PropertyHolder)value);	      
	      
	      if(value instanceof Flag)
	    	  return new ButtonEditor((IconCheckBox) getCellRenderer(row, column).getTableCellRendererComponent(this, value, false, false, row, column)
	    			  , ((Flag)value).getType() );	    	  	      

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
		NumericPropertyKeyword type;
				
		public ButtonEditor(AbstractButton btn, PropertyHolder dat) {
			this.btn = btn;						
			this.dat = dat;
			
			btn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					JFrame dataFrame = new DataFrame(dat, btn);
					dataFrame.setVisible(true);
					btn.setEnabled(false);
					dataFrame.addWindowListener(new WindowAdapter() {
						@Override
						public void windowClosed(WindowEvent we) {
							btn.setText(((DataFrame) dataFrame).getDataObject().toString());
							btn.setEnabled(true);
						}
					});
				}
			});			
			
		}
		
		public ButtonEditor(IconCheckBox btn, NumericPropertyKeyword index) {
			this.btn = btn;
			this.type = index;

			btn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					IconCheckBox source = (IconCheckBox) e.getSource();
					source.setHorizontalAlignment(SwingConstants.CENTER);
					((JTable) ( source.getParent()) ).getCellEditor().stopCellEditing();
				}
				
			});
			
		}

		@Override
		public Object getCellEditorValue() {
			if(dat != null)
				return dat;					
			Flag f = new Flag(type);
			f.setValue( btn.isSelected() );
			return f;
		}

		@Override
		public Component getTableCellEditorComponent(JTable arg0, Object value, boolean arg2, int arg3, int arg4) {
			return btn;
		}		
		
		
	}
	
	public PropertyHolder getPropertyHolder() {
		return propertyHolder;
	}
	
}