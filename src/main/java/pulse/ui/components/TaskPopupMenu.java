package pulse.ui.components;

import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import pulse.tasks.Result;
import pulse.tasks.ResultFormat;
import pulse.tasks.SearchTask;
import pulse.tasks.Status;
import pulse.tasks.Status.Details;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.ui.Launcher;
import pulse.ui.Messages;

@SuppressWarnings("serial")
public class TaskPopupMenu extends JPopupMenu {

	private final static Font f = new Font(Messages.getString("TaskTable.FontName"), Font.PLAIN, 16); //$NON-NLS-1$ 

	private final static int ICON_SIZE = 24;
	
    private static ImageIcon ICON_GRAPH		= Launcher.loadIcon("graph.png", ICON_SIZE);  
    private static ImageIcon ICON_METADATA	= Launcher.loadIcon("metadata.png", ICON_SIZE);
    private static ImageIcon ICON_MISSING	= Launcher.loadIcon("missing.png", ICON_SIZE);
    private static ImageIcon ICON_RUN		= Launcher.loadIcon("execute_single.png", ICON_SIZE);
    private static ImageIcon ICON_RESET		= Launcher.loadIcon("reset.png", ICON_SIZE);
    private static ImageIcon ICON_RESULT	= Launcher.loadIcon("result.png", ICON_SIZE);	
    
	public TaskPopupMenu() {	
		JMenuItem itemExecute, itemChart, itemExtendedChart, itemShowMeta, itemReset, itemGenerateResult, itemShowStatus;		
	
		Window referenceWindow = SwingUtilities.getWindowAncestor(this);
		
		itemChart		= new JMenuItem(Messages.getString("TaskTablePopupMenu.ShowHeatingCurve"), ICON_GRAPH); //$NON-NLS-1$
		itemChart.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				plot(false);
			}
			
		});
		
		itemChart.setFont(f);
		
		itemExtendedChart = new JMenuItem(Messages.getString("TaskTablePopupMenu.ShowExtendedHeatingCurve"), ICON_GRAPH); //$NON-NLS-1$
		itemExtendedChart.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				plot(true);				
			}
			
		});
		
		itemExtendedChart.setFont(f);
		
		itemShowMeta	= new JMenuItem("Show metadata", ICON_METADATA);
		itemShowMeta.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				SearchTask t = TaskManager.getSelectedTask();
				
				if(t == null) {
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()), 
							Messages.getString("TaskTablePopupMenu.EmptySelection2"), Messages.getString("TaskTablePopupMenu.11"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
				
				JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()), 
							t.getExperimentalCurve().getMetadata().toString(), 
							"Metadata", JOptionPane.PLAIN_MESSAGE);
				
			}
			
		});
		
		itemShowMeta.setFont(f);
		
		itemShowStatus	= new JMenuItem("What is missing?", ICON_MISSING);
		
		TaskManager.addSelectionListener(event -> {
			if(TaskManager.getSelectedTask().getStatus().getDetails() == null)
				itemShowStatus.setEnabled(false);
			else
				itemShowStatus.setEnabled(true);
		});
		
		itemShowStatus.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				SearchTask t = TaskManager.getSelectedTask();
				
				if(t == null) 
					return;								
				
				Details d = t.getStatus().getDetails();
				
				if(d == null)
					return;
				
				JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()), 
							d.toString(), 
							t + " status", JOptionPane.INFORMATION_MESSAGE);
				
			}
			
		});
		
		itemShowStatus.setFont(f);
		
		itemExecute		= new JMenuItem(Messages.getString("TaskTablePopupMenu.Execute"), ICON_RUN); //$NON-NLS-1$
		itemExecute.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				SearchTask t = TaskManager.getSelectedTask();
				
				if(t == null) {
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()), 
							Messages.getString("TaskTablePopupMenu.EmptySelection"), Messages.getString("TaskTablePopupMenu.ErrorTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
				
				if(t.checkProblems() == Status.DONE) {
					int dialogButton = JOptionPane.YES_NO_OPTION;
					int dialogResult = JOptionPane.showConfirmDialog(referenceWindow, Messages.getString("TaskTablePopupMenu.TaskCompletedWarning") + System.lineSeparator() + Messages.getString("TaskTablePopupMenu.AskToDelete"), Messages.getString("TaskTablePopupMenu.DeleteTitle"), dialogButton);
					if(dialogResult == 1) 
					  return;
					else {
						TaskManager.removeResult(t);
						//t.storeCurrentSolution();
					}
				}
				
				if(t.checkProblems() != Status.READY) {
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()),
							t.toString() + " is " + t.getStatus().getMessage(), //$NON-NLS-1$
						    Messages.getString("TaskTablePopupMenu.TaskNotReady"), //$NON-NLS-1$
						    JOptionPane.ERROR_MESSAGE);			
					return;
				}
				
				TaskManager.execute(TaskManager.getSelectedTask());				
				
			}						
			
		});
		
		itemExecute.setFont(f);
		
		itemReset = new JMenuItem(Messages.getString("TaskTablePopupMenu.Reset"), ICON_RESET);
		itemReset.setFont(f);
		
		itemReset.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				TaskManager.getSelectedTask().clear();
			}
			
		});
		
		itemGenerateResult = new JMenuItem(Messages.getString("TaskTablePopupMenu.GenerateResult"), ICON_RESULT);
		itemGenerateResult.setFont(f);
		
		itemGenerateResult.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Result r = null;
				SearchTask t = TaskManager.getSelectedTask();
				
				if(t == null)
					return;
				
				if(t.getProblem() == null)
					return;
				
				r = new Result(TaskManager.getSelectedTask(), ResultFormat.getInstance());				
				TaskManager.useResult(t, r);
				TaskRepositoryEvent e = new TaskRepositoryEvent
						(TaskRepositoryEvent.State.TASK_FINISHED, 
								TaskManager.getSelectedTask().getIdentifier());
				TaskManager.notifyListeners(e);
			}
			
		});
		
		add(itemShowMeta);
		add(itemShowStatus);
		add(new JSeparator());
		add(itemChart);
		add(itemExtendedChart);
		add(new JSeparator());
		add(itemReset);
		add(itemGenerateResult);
		add(new JSeparator());
		add(itemExecute);
		
	}
	
	@SuppressWarnings("unchecked")
	public void plot(boolean extended) {
		SearchTask t = TaskManager.getSelectedTask();
		
		if(t == null) {
			JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this), 
					Messages.getString("TaskTablePopupMenu.EmptySelection2"), Messages.getString("TaskTablePopupMenu.11"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		
		Details statusDetails = t.getStatus().getDetails();
		
		if(statusDetails == Details.MISSING_HEATING_CURVE) {
			JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this), 
					Messages.getString("TaskTablePopupMenu.12"), Messages.getString("TaskTablePopupMenu.13"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
			return;	
		}
		
		if(t.getScheme() != null) 
			t.getScheme().getSolver(t.getProblem()).solve(t.getProblem());		
		
		Chart.plot(t, extended);
	}
	
	
}
