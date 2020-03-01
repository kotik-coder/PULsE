package pulse.ui.components;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.Identifier;
import pulse.tasks.SearchTask;
import pulse.tasks.StateEntry;
import pulse.tasks.Status;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.DataCollectionListener;
import pulse.tasks.listeners.StatusChangeListener;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskRepositoryListener;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.listeners.TaskSelectionListener;
import pulse.ui.Messages;
import pulse.ui.components.controllers.TaskTableRenderer;

@SuppressWarnings("serial")
public class TaskTable extends JTable {
	
	private final static int ROW_HEIGHT		= 35;
	private final static int HEADER_HEIGHT	= 30;
	
	private TaskTableRenderer taskTableRenderer;
	private	TaskPopupMenu menu;
		
	private Comparator<NumericProperty> numericComparator = (i1, i2) -> i1.compareTo(i2);
	private Comparator<Status> statusComparator = (s1, s2) -> s1.compareTo(s2);
		
	private final static int FONT_SIZE = 14;
	
		public TaskTable() {
			super();
			taskTableRenderer = new TaskTableRenderer();			
			this.setRowSelectionAllowed(true);
			setRowHeight(ROW_HEIGHT);
			
			setFillsViewportHeight(true);
			setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			setShowHorizontalLines(false);
			
			var model = new TaskTableModel();
			setModel(model);
					   
			TableHeader th = new TableHeader(getColumnModel(), new String[]{
				   NumericProperty.theDefault(NumericPropertyKeyword.IDENTIFIER).getDescriptor(true), 
				   NumericProperty.theDefault(NumericPropertyKeyword.TEST_TEMPERATURE).getDescriptor(true),
				   NumericProperty.theDefault(NumericPropertyKeyword.SUM_OF_SQUARES).getDescriptor(true),
				   NumericProperty.theDefault(NumericPropertyKeyword.RSQUARED).getDescriptor(true), 
				   NumericProperty.theDefault(NumericPropertyKeyword.STATISTIC).getDescriptor(true), 
				   ("Task status")
				});
			
			setTableHeader(th);
			
			Font font = getTableHeader().getFont().deriveFont(FONT_SIZE);
			getTableHeader().setFont(font);
			getTableHeader().setPreferredSize(new Dimension( 50 , HEADER_HEIGHT ));
			
			setAutoCreateRowSorter(true);
			var sorter = new TableRowSorter<DefaultTableModel>();
			sorter.setModel(model);
		    var list	= new ArrayList<RowSorter.SortKey>();
		    
		    for(int i = 0; i < this.getModel().getColumnCount(); i++) {
		    	list.add( new RowSorter.SortKey(i, SortOrder.ASCENDING) );
		    	if(i == TaskTableModel.STATUS_COLUMN)
		    		sorter.setComparator(i, statusComparator);
		    	else
		    		sorter.setComparator(i, numericComparator);
		    }
		    	
		    sorter.setSortKeys(list);
		    setRowSorter(sorter);
		    
		    initListeners();
		    menu = new TaskPopupMenu();
		    
		}
		
		public void initListeners() {
			
			/*
			 * task removed/added listener
			 */
			
			TaskManager.addTaskRepositoryListener(new TaskRepositoryListener() {

				@Override
				public void onTaskListChanged(TaskRepositoryEvent e) {
					if(e.getState() == TaskRepositoryEvent.State.TASK_REMOVED)
						((TaskTableModel) getModel()).removeTask(e.getId());
					else if(e.getState() == TaskRepositoryEvent.State.TASK_ADDED)
						((TaskTableModel) getModel()).addTask(TaskManager.getTask(e.getId()));
				}
				
			});
			
			/*
			 * mouse listener
			 */
			
			addMouseListener(new MouseAdapter() {
				
				@Override
		        public void mouseClicked(MouseEvent e) {
					
					if(rowAtPoint(e.getPoint()) < 0)
						return;
					
					if(rowAtPoint(e.getPoint()) != getSelectedRow())
						return;
					
		            if (SwingUtilities.isRightMouseButton(e))
		                menu.show(e.getComponent(), e.getX(), e.getY());
		            
		        }
				
			});
			
			/*
			 * selection listener
			 */
		    
			ListSelectionModel lsm = getSelectionModel();
			TaskTable reference = this;
			
			lsm.addListSelectionListener(new ListSelectionListener() {

				@Override
				public void valueChanged(ListSelectionEvent e) {
					if(lsm.getValueIsAdjusting())
						return;
					
					if(lsm.isSelectionEmpty()) 
						return;
					
					Identifier id = (Identifier)getValueAt(lsm.getMinSelectionIndex(), 0);
					TaskManager.selectTask( id, reference );
					
				}
				
			});
			
			TaskManager.addSelectionListener(new TaskSelectionListener() {

				@Override
				public void onSelectionChanged(TaskSelectionEvent e) {
					//simply ignore call if event is triggered by taskTable
					if(e.getSource() instanceof TaskTable)
						return;
						
					Identifier id = e.getSelection().getIdentifier();
					Identifier idFromTable = null;															
					
					for(int i = 0; i < getRowCount(); i++) {
						idFromTable = (Identifier) getValueAt(i, 0);
						
						if(idFromTable.equals(id)) {						
							setRowSelectionInterval(i, i);
							return;
						}
						
					}
					
					clearSelection();
					
				}
							
			});
			
		}
			
		@Override
		public TableCellEditor getCellEditor(int row, int column) {
		   return null;
		}
		
		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			   return taskTableRenderer;
		}		
		
		public class TaskTableModel extends DefaultTableModel {
			
			/**
			 * 
			 */
			private static final long serialVersionUID = 7196461896699123800L;
			private static final int SS_COLUMN = 2;
			private static final int R2_COLUMN = 3;
			private static final int KS_COLUMN = 4;
			private static final int STATUS_COLUMN = 5;
			
			public TaskTableModel() { 								
				
				super( new Object[][] {},
						   new String[] {
								   NumericProperty.theDefault(NumericPropertyKeyword.IDENTIFIER).getAbbreviation(true),
								   NumericProperty.theDefault(NumericPropertyKeyword.TEST_TEMPERATURE).getAbbreviation(true),
								   NumericProperty.theDefault(NumericPropertyKeyword.SUM_OF_SQUARES).getAbbreviation(true),  
								   NumericProperty.theDefault(NumericPropertyKeyword.RSQUARED).getAbbreviation(true),  
								   NumericProperty.theDefault(NumericPropertyKeyword.STATISTIC).getAbbreviation(true),  
								   Messages.getString("TaskTable.Status")} );
									
			}		
	
			public void addTask(SearchTask t) {
				Object[] data = new Object[]{
						t.getIdentifier(),
						t.getTestTemperature(),					
						t.getSumOfSquares(),
						t.getRSquared(),
						t.getNormalityTest().getStatistic(),
						t.getStatus()
				};	
				
				super.addRow(data);	
				
				t.addStatusChangeListener(new StatusChangeListener() {

					@Override
					public void onStatusChange(StateEntry e) { 
							setValueAt(e.getState(), searchRow(t.getIdentifier()), STATUS_COLUMN);	
							setValueAt(t.getNormalityTest().getStatistic(), searchRow(t.getIdentifier()), KS_COLUMN);
					}
					
				});
				
				t.addTaskListener(new DataCollectionListener() {

					@Override
					public void onDataCollected(StateEntry e) {
						setValueAt(t.getSumOfSquares(), searchRow(t.getIdentifier()), SS_COLUMN);
						setValueAt(t.getRSquared(), searchRow(t.getIdentifier()), R2_COLUMN);
					}
					
				});
				
			}
			
			public void removeTask(Identifier id) {
				Identifier idFromTable = null;
				
				for(int i = 0; i < getRowCount(); i++) {
					idFromTable = (Identifier) getValueAt(i, 0);
					
					if(idFromTable.equals(id)) {
						removeRow(i);
						break;
					}
					
				}
				
			}
			
			public int searchRow(Identifier id) {
				int rows = this.getRowCount();
				
				var dataVector = this.getDataVector();
				
				for(int i = 0; i < rows; i++) {
					if(id.equals(
							(dataVector.elementAt(i)).elementAt(0)
							))
						return i;
				}
				
				return -1;
				
			}
						
		}		

		private class TableHeader extends JTableHeader {
			
			private String[] tooltips;
			
		    public TableHeader(TableColumnModel columnModel, String[] columnTooltips) {
		      super(columnModel);//do everything a normal JTableHeader does
		      this.tooltips = columnTooltips;//plus extra data
		    }

		    @Override
			public String getToolTipText(MouseEvent e) {
		        java.awt.Point p = e.getPoint();
		        int index = columnModel.getColumnIndexAtX(p.x);
		        int realIndex = columnModel.getColumn(index).getModelIndex();
		        return this.tooltips[realIndex];
		    }
			
		}
		
}