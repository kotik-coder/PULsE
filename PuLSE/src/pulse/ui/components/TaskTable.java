package pulse.ui.components;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.DefaultRowSorter;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import pulse.tasks.Identifier;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.DataCollectionListener;
import pulse.tasks.listeners.StatusChangeListener;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskRepositoryListener;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.listeners.TaskSelectionListener;
import pulse.tasks.listeners.TaskStateEvent;

public class TaskTable extends JTable {
	
		/**
	 * 
	 */
	private static final long serialVersionUID = -6220530138940491704L;
		private final static int ROW_HEIGHT = 35;
		private final static int HEADER_HEIGHT = 45;
		private TaskTableRenderer taskTableRenderer;
		private	TaskTablePopupMenu menu;
		
		private final static Font f = new Font(Messages.getString("TaskTable.FontName"), Font.PLAIN, 14); //$NON-NLS-1$
		
		public TaskTable() {
			super();
			taskTableRenderer = new TaskTableRenderer();
			this.setRowSelectionAllowed(true);
			setRowHeight(ROW_HEIGHT);
			setFillsViewportHeight(true);
			setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			setShowHorizontalLines(false);
			setModel(new TaskTableModel());
			
			getTableHeader().setFont(f);
			getTableHeader().setPreferredSize(new Dimension( 50 , HEADER_HEIGHT ));
			
			menu = new TaskTablePopupMenu();
			
			setAutoCreateRowSorter(true);
		    
			DefaultRowSorter sorter		= ((DefaultRowSorter) getRowSorter()); 
		    ArrayList<RowSorter.SortKey> list	= new ArrayList();
		    
			list.add( new RowSorter.SortKey(0, SortOrder.ASCENDING) );
		    sorter.setComparator(0, new IdentifierComparator());
		    
		    for(int i = 1; i < 4; i++) {
		    	list.add( new RowSorter.SortKey(i, SortOrder.ASCENDING) );
		    	sorter.setComparator(1, new NumericPropertyComparator());
		    }
		    	
		    sorter.setSortKeys(list);
		    
		    initListeners();
			
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
						if(! idFromTable.equals(id))
							continue;
						
						setRowSelectionInterval(i, i);
						return;
						
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
			private static final int STATUS_COLUMN = 4;
			
			public TaskTableModel() { 
				super( new Object[][] {},
					   new String[] {
							   Messages.getString("TaskTable.TaskID"),  //$NON-NLS-1$
							   Messages.getString("TaskTable.Temperature"),  //$NON-NLS-1$
							   Messages.getString("TaskTable.SS2"),  //$NON-NLS-1$
							   Messages.getString("TaskTable.R2"),  //$NON-NLS-1$
							   Messages.getString("TaskTable.Status") //$NON-NLS-1$
					 });
								
			}
	
			public void addTask(SearchTask t) {
				Object[] data = new Object[]{
						t.getIdentifier(),
						t.getTestTemperature(),					
						t.getSumOfSquares(),
						t.getRSquared(),
						t.getStatus()
				};
				
				super.addRow(data);	
				
				int row = super.getRowCount() - 1;
			
				t.addStatusChangeListener(new StatusChangeListener() {

					@Override
					public void onStatusChange(TaskStateEvent e) { 
							setValueAt(e.getState(), searchRow(t.getIdentifier()), STATUS_COLUMN);						
					}
					
				});
				
				t.addTaskListener(new DataCollectionListener() {

					@Override
					public void onDataCollected(TaskStateEvent e) {
						setValueAt(t.getSumOfSquares(), searchRow(t.getIdentifier()), SS_COLUMN);
						setValueAt(t.getRSquared(), searchRow(t.getIdentifier()), R2_COLUMN);
					}
					
				});
				
			}
			
			public void removeTask(Identifier id) {
				Identifier idFromTable = null;
				
				for(int i = 0; i < getRowCount(); i++) {
					idFromTable = (Identifier) getValueAt(i, 0);
					if(! idFromTable.equals(id))
						continue;
					
					removeRow(i);
					break;
					
				}
				
				return;
			}
			
			public int searchRow(Identifier id) {
				int rows = this.getRowCount();
				
				Vector dataVector = this.getDataVector();
				
				for(int i = 0; i < rows; i++) {
					if(id.equals(
							((Vector)dataVector.elementAt(i)).elementAt(0)
							))
						return i;
				}
				
				return -1;
				
			}
						
		}
		
}
