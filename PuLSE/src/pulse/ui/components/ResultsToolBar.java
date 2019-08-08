package pulse.ui.components;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import pulse.tasks.AbstractResult;
import pulse.tasks.AverageResult;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.ui.components.ResultTable.ResultTableModel;
import pulse.ui.frames.PlotFrame;
import pulse.ui.frames.SimpleInputFrame;

public class ResultsToolBar extends JToolBar {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4896841965798920817L;

	private final static String fileTypeLabel = Messages.getString("ResultsToolBar.0"); //$NON-NLS-1$
	
	private static PlotFrame plotFrame;
	private static SimpleInputFrame simpleInput;
	private ResultTable resultsTable;

	public ResultsToolBar(ResultTable resultsTable, JFrame parentWindow) {
		super();
		setLayout(new GridLayout());
		
		/*
		 * CONTROL BUTTONS
		 */
			
		this.resultsTable = resultsTable;
		
		ToolBarButton btnDel = new ToolBarButton(Messages.getString("ResultsToolBar.DeleteButton")); //$NON-NLS-1$
		btnDel.setEnabled(false);
		add(btnDel);		
		
		btnDel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ResultTableModel rtm = (ResultTableModel) resultsTable.getModel();
				
				int[] selection = resultsTable.getSelectedRows();								
				
				if(selection.length < 0)
					return;
			
				for(int i = selection.length - 1; i >= 0; i--) 
					rtm.removeRow( 
							resultsTable.convertRowIndexToModel(selection[i]) );
				
			}
			
		});
				
		ToolBarButton btnAvg = new ToolBarButton(Messages.getString("ResultsToolBar.AutoAverageButton")); //$NON-NLS-1$
		btnAvg.setEnabled(false);
		add(btnAvg);
		
		btnAvg.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(resultsTable.getRowCount() < 1)
					return;
				if(simpleInput == null)
					simpleInput = new SimpleInputFrame(resultsTable);
				simpleInput.setLocationRelativeTo(null);
				simpleInput.setVisible(true);
			}
			
		});		
		
		ToolBarButton btnExpand = new ToolBarButton("Undo"); //$NON-NLS-1$
		add(btnExpand);		
		
		btnExpand.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ResultTableModel dtm = (ResultTableModel) resultsTable.getModel();
										
				for(int i = dtm.getRowCount()-1; i >= 0; i--) 
					dtm.removeRow( 
							resultsTable.convertRowIndexToModel(i) );
				
				for(SearchTask t : TaskManager.getTaskList())
					dtm.addRow( TaskManager.getResult(t) );
				
			}
			
		});
				
		ToolBarButton btnPlot = new ToolBarButton(Messages.getString("ResultsToolBar.PlotButton")); //$NON-NLS-1$
		btnPlot.setEnabled(false);
		add(btnPlot);				
		
		btnPlot.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				if(resultsTable.getModel().getRowCount() < 1) {
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()),
						    Messages.getString("ResultsToolBar.NoDataError"), //$NON-NLS-1$
						    Messages.getString("ResultsToolBar.NoResultsError"), //$NON-NLS-1$
						    JOptionPane.ERROR_MESSAGE);			
					return;
				}						
				
				showPlotFrame();
				
			}
			
		});
				
		ToolBarButton btnSave = new ToolBarButton(Messages.getString("ResultsToolBar.SaveButton")); //$NON-NLS-1$
		btnSave.setEnabled(false);
		add(btnSave);
		
		btnSave.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(resultsTable.getRowCount() < 1) {
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()),
						    Messages.getString("ResultsToolBar.7"), //$NON-NLS-1$
						    Messages.getString("ResultsToolBar.8"), //$NON-NLS-1$
						    JOptionPane.ERROR_MESSAGE);			
					return;
				}
				
				resultsTable.askToSave(parentWindow, fileTypeLabel);
				
			}
			
			
		});
		
		resultsTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				int[] selection = resultsTable.getSelectedRows();
				if(selection.length < 1) {
					btnDel.setEnabled(false);					
				}
				else {
					btnDel.setEnabled(true);
				}
			}
					
			}
		);
		
		resultsTable.getModel().addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent arg0) {				
				if(resultsTable.getRowCount() > 1) {
					btnAvg.setEnabled(true);
					if(resultsTable.getRowCount() > 3) 
						btnPlot.setEnabled(true);
					else
						btnPlot.setEnabled(false);
				} 
				else {
					btnAvg.setEnabled(false);
				}				
				
				if(resultsTable.getRowCount() > 0) 
					btnSave.setEnabled(true);
				else
					btnSave.setEnabled(false);
				
			}
			
		});
		
		setFloatable(false);
		
	}
	
	private void showPlotFrame() {
		if(plotFrame == null) 			
			plotFrame = new PlotFrame( resultsTable.getColumnNames(), resultsTable.data() );		
		else
		    plotFrame.update(resultsTable.getColumnNames(), resultsTable.data());
		
		plotFrame.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
		plotFrame.setVisible(true);
		plotFrame.requestFocus();
	}
	
}
