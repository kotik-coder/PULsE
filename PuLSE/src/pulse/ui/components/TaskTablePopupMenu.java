package pulse.ui.components;

import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

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
import pulse.ui.frames.TaskControlFrame;

public class TaskTablePopupMenu extends JPopupMenu {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4545548692231417093L;
	
	private final static Font f = new Font(Messages.getString("TaskTable.FontName"), Font.BOLD, 18); //$NON-NLS-1$ 

	public TaskTablePopupMenu() {	
		JMenuItem problemStatement, itemExecute, itemChart, itemExtendedChart, itemShowMeta, itemReset, itemGenerateResult, itemShowStatus;
		
		problemStatement = new JMenuItem(Messages.getString("TaskTablePopupMenu.ShowDetails")); //$NON-NLS-1$
		
		problemStatement.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Launcher.showProblemStatementFrame();
			}
			
		});
		problemStatement.setFont(f);
	
		Window referenceWindow = SwingUtilities.getWindowAncestor(this);
		
		itemChart		= new JMenuItem(Messages.getString("TaskTablePopupMenu.ShowHeatingCurve")); //$NON-NLS-1$
		itemChart.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				plot(false);
			}
			
		});
		
		itemChart.setFont(f);
		
		itemExtendedChart = new JMenuItem(Messages.getString("TaskTablePopupMenu.ShowExtendedHeatingCurve")); //$NON-NLS-1$
		itemExtendedChart.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				plot(true);				
			}
			
		});
		
		itemExtendedChart.setFont(f);
		
		itemShowMeta	= new JMenuItem("Show metadata");
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
		
		itemShowStatus	= new JMenuItem("What is missing?");
		
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
		
		itemExecute		= new JMenuItem(Messages.getString("TaskTablePopupMenu.Execute")); //$NON-NLS-1$
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
					int dialogResult = JOptionPane.showConfirmDialog(referenceWindow, Messages.getString("TaskTablePopupMenu.TaskCompletedWarning") + System.lineSeparator() + Messages.getString("TaskTablePopupMenu.AskToDelete"), Messages.getString("TaskTablePopupMenu.DeleteTitle"), dialogButton); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					if(dialogResult == 1) 
					  return;
					else {
						TaskManager.removeResult(t);
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
		
		itemReset = new JMenuItem(Messages.getString("TaskTablePopupMenu.Reset"));
		itemReset.setFont(f);
		
		itemReset.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				TaskManager.getSelectedTask().reset();
			}
			
		});
		
		itemGenerateResult = new JMenuItem(Messages.getString("TaskTablePopupMenu.GenerateResult"));
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
				
				try {
					r = new Result(TaskManager.getSelectedTask(), ResultFormat.getFormat());
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				TaskManager.useResult(t, r);
				TaskRepositoryEvent e = new TaskRepositoryEvent
						(TaskRepositoryEvent.State.TASK_FINISHED, 
								TaskManager.getSelectedTask().getIdentifier());
				TaskManager.notifyListeners(e);
			}
			
		});
		

		add(problemStatement);
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
	
	public void plot(boolean extended) {
		SearchTask t = TaskManager.getSelectedTask();
		
		if(t == null) {
			JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) this), 
					Messages.getString("TaskTablePopupMenu.EmptySelection2"), Messages.getString("TaskTablePopupMenu.11"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		
		Details statusDetails = t.getStatus().getDetails();
		
		if(statusDetails == Details.MISSING_HEATING_CURVE) {
			JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) this), 
					Messages.getString("TaskTablePopupMenu.12"), Messages.getString("TaskTablePopupMenu.13"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
			return;	
		}
		
		if(t.getScheme() != null) {
			t.adjustScheme();
			t.solveProblem();
		}
		
		TaskControlFrame.plot(t, extended);
	}
	
	
}
