package pulse.ui.frames;

import java.awt.BorderLayout;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import pulse.tasks.TaskManager;
import pulse.ui.components.TaskTable;
import pulse.ui.components.TaskTable.TaskTableModel;
import pulse.ui.components.listeners.TaskActionListener;
import pulse.ui.components.panels.TaskToolbar;

@SuppressWarnings("serial")
public class TaskManagerFrame extends JInternalFrame {
	
	private TaskTable taskTable;
	private TaskToolbar taskToolbar;
	
	public TaskManagerFrame() {
		super("Task Manager", true, false, true, true);
		initComponents();
		adjustEnabledControls();
        manageListeners();
        setVisible(true);
	}
	
	private void manageListeners() {
		taskToolbar.addTaskActionListener(new TaskActionListener() {

			@Override
			public void onRemoveRequest() {
				taskTable.removeSelectedRows();
			}

			@Override
			public void onClearRequest() {
				TaskManager.clear();
			}

			@Override
			public void onResetRequest() {
				//no new actions
			}

			@Override
			public void onGraphRequest() {
				//no new actions
			}
			
		});
	}
	
	private void initComponents() {
        var taskScrollPane = new JScrollPane();
        taskTable = new TaskTable();
        taskScrollPane.setViewportView(taskTable);
        getContentPane().add(taskScrollPane, BorderLayout.CENTER);        
        taskToolbar = new TaskToolbar();
        getContentPane().add(taskToolbar, BorderLayout.PAGE_START);
	}
	
	private void adjustEnabledControls() {
		TaskTableModel ttm = (TaskTableModel) taskTable.getModel();
		
		ttm.addTableModelListener(
				new TableModelListener(){

					@Override
					public void tableChanged(TableModelEvent arg0) {
						if(ttm.getRowCount() < 1) {
							taskToolbar.setClearEnabled(false);
							taskToolbar.setResetEnabled(false);
							taskToolbar.setExecEnabled(false);
						} else {
							taskToolbar.setClearEnabled(true);
							taskToolbar.setResetEnabled(true);
							taskToolbar.setExecEnabled(true);
						}
					}						
					
					
		});
		
		taskTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				int[] selection = taskTable.getSelectedRows();
				if(taskTable.getSelectedRow() < 0) {
					taskToolbar.setRemoveEnabled(false);
					taskToolbar.setGraphEnabled(false);	
				} else {
					if(selection.length > 1) {
						taskToolbar.setRemoveEnabled(false);
						taskToolbar.setGraphEnabled(false);
					}
					else if(selection.length > 0) {
						taskToolbar.setRemoveEnabled(true);
						taskToolbar.setGraphEnabled(true);
					}
				}
			}
		
		});
	}

	public TaskToolbar getTaskToolbar() {
		return taskToolbar;
	}

}