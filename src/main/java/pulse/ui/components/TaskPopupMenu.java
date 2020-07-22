package pulse.ui.components;

import static java.awt.Font.PLAIN;
import static java.lang.System.err;
import static java.lang.System.lineSeparator;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.tasks.ResultFormat.getInstance;
import static pulse.tasks.Status.DONE;
import static pulse.tasks.Status.READY;
import static pulse.tasks.Status.Details.MISSING_HEATING_CURVE;
import static pulse.tasks.Status.Details.NONE;
import static pulse.tasks.TaskManager.addSelectionListener;
import static pulse.tasks.TaskManager.execute;
import static pulse.tasks.TaskManager.getSelectedTask;
import static pulse.tasks.TaskManager.notifyListeners;
import static pulse.tasks.TaskManager.removeResult;
import static pulse.tasks.TaskManager.useResult;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_FINISHED;
import static pulse.ui.Launcher.loadIcon;
import static pulse.ui.Messages.getString;
import static pulse.ui.frames.MainGraphFrame.getChart;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import pulse.problem.schemes.solvers.Solver;
import pulse.problem.schemes.solvers.SolverException;
import pulse.tasks.Result;
import pulse.tasks.listeners.TaskRepositoryEvent;

@SuppressWarnings("serial")
public class TaskPopupMenu extends JPopupMenu {

	private final static Font f = new Font(getString("TaskTable.FontName"), PLAIN, 16); //$NON-NLS-1$

	private final static int ICON_SIZE = 24;

	private static ImageIcon ICON_GRAPH = loadIcon("graph.png", ICON_SIZE);
	private static ImageIcon ICON_METADATA = loadIcon("metadata.png", ICON_SIZE);
	private static ImageIcon ICON_MISSING = loadIcon("missing.png", ICON_SIZE);
	private static ImageIcon ICON_RUN = loadIcon("execute_single.png", ICON_SIZE);
	private static ImageIcon ICON_RESET = loadIcon("reset.png", ICON_SIZE);
	private static ImageIcon ICON_RESULT = loadIcon("result.png", ICON_SIZE);

	public TaskPopupMenu() {
		JMenuItem itemExecute, itemChart, itemExtendedChart, itemShowMeta, itemReset, itemGenerateResult,
				itemShowStatus;

		var referenceWindow = getWindowAncestor(this);

		itemChart = new JMenuItem(getString("TaskTablePopupMenu.ShowHeatingCurve"), ICON_GRAPH); //$NON-NLS-1$
		itemChart.addActionListener((ActionEvent e) -> {
                    plot(false);
        });

		itemChart.setFont(f);

		itemExtendedChart = new JMenuItem(getString("TaskTablePopupMenu.ShowExtendedHeatingCurve"), //$NON-NLS-1$
				ICON_GRAPH);
		itemExtendedChart.addActionListener((ActionEvent e) -> {
                    plot(true);
        });

		itemExtendedChart.setFont(f);

		itemShowMeta = new JMenuItem("Show metadata", ICON_METADATA);
		itemShowMeta.addActionListener((ActionEvent e) -> {
            var t = getSelectedTask();
            if (t == null) {
                showMessageDialog(getWindowAncestor((Component) e.getSource()), getString("TaskTablePopupMenu.EmptySelection2"), //$NON-NLS-1$
                getString("TaskTablePopupMenu.11"), ERROR_MESSAGE); //$NON-NLS-1$
                //$NON-NLS-1$
                return;
            }
            showMessageDialog(getWindowAncestor((Component) e.getSource()), t.getExperimentalCurve().getMetadata().toString(), "Metadata", PLAIN_MESSAGE);
        });

		itemShowMeta.setFont(f);

		itemShowStatus = new JMenuItem("What is missing?", ICON_MISSING);

		addSelectionListener(event -> {
			var details = getSelectedTask().checkProblems(false).getDetails();
			if ((details == null) || (details == NONE))
				itemShowStatus.setEnabled(false);
			else
				itemShowStatus.setEnabled(true);
		});

		itemShowStatus.addActionListener((ActionEvent e) -> {
            var t = getSelectedTask();
            if (t != null) {
                var d = t.getStatus().getDetails();
                showMessageDialog(getWindowAncestor((Component) e.getSource()), "<html>This is due to " + d.toString() + "</html>", "Problems with " + t, INFORMATION_MESSAGE);
            }
        });

		itemShowStatus.setFont(f);

		itemExecute = new JMenuItem(getString("TaskTablePopupMenu.Execute"), ICON_RUN); //$NON-NLS-1$
		itemExecute.addActionListener((ActionEvent e) -> {
            var t = getSelectedTask();
            if (t == null) {
                showMessageDialog(getWindowAncestor((Component) e.getSource()), getString("TaskTablePopupMenu.EmptySelection"), //$NON-NLS-1$
                getString("TaskTablePopupMenu.ErrorTitle"), ERROR_MESSAGE); //$NON-NLS-1$
                //$NON-NLS-1$
                return;
            }
            if (t.checkProblems() == DONE) {
                var dialogButton = YES_NO_OPTION;
                var dialogResult = showConfirmDialog(referenceWindow, getString("TaskTablePopupMenu.TaskCompletedWarning") + lineSeparator() + getString("TaskTablePopupMenu.AskToDelete"), getString("TaskTablePopupMenu.DeleteTitle"), dialogButton);
                if (dialogResult == 1) {
                    return;
                } else {
                    removeResult(t);
                    // t.storeCurrentSolution();
                }
            }
            if (t.checkProblems() != READY) {
                showMessageDialog(getWindowAncestor((Component) e.getSource()), t.toString() + " is " + t.getStatus().getMessage(), //$NON-NLS-1$
                getString("TaskTablePopupMenu.TaskNotReady"), //$NON-NLS-1$
                ERROR_MESSAGE);
                return;
            }
            execute(getSelectedTask());
        });

		itemExecute.setFont(f);

		itemReset = new JMenuItem(getString("TaskTablePopupMenu.Reset"), ICON_RESET);
		itemReset.setFont(f);

		itemReset.addActionListener((ActionEvent arg0) -> {
            getSelectedTask().clear();
        });

		itemGenerateResult = new JMenuItem(getString("TaskTablePopupMenu.GenerateResult"), ICON_RESULT);
		itemGenerateResult.setFont(f);

		itemGenerateResult.addActionListener((ActionEvent arg0) -> {
                    Result r = null;
            var t = getSelectedTask();
            if (t == null)
                return;
            if (t.getProblem() == null)
                return;
            r = new Result(getSelectedTask(), getInstance());
            useResult(t, r);
            var e = new TaskRepositoryEvent(TASK_FINISHED, getSelectedTask().getIdentifier());
            notifyListeners(e);
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void plot(boolean extended) {
		var t = getSelectedTask();

		if (t == null) {
			showMessageDialog(getWindowAncestor(this),
					getString("TaskTablePopupMenu.EmptySelection2"), //$NON-NLS-1$
            getString("TaskTablePopupMenu.11"), ERROR_MESSAGE); //$NON-NLS-1$
			return;
		}

		var statusDetails = t.getStatus().getDetails();

		if (statusDetails == MISSING_HEATING_CURVE) {
			showMessageDialog(getWindowAncestor(this),
					getString("TaskTablePopupMenu.12"), getString("TaskTablePopupMenu.13"), //$NON-NLS-1$ //$NON-NLS-2$
            ERROR_MESSAGE);
			return;
		}

		if (t.getScheme() != null)
			try {
				((Solver) t.getScheme()).solve(t.getProblem());
			} catch (SolverException e) {
				err.println("Solver error for " + t + "Details: ");
				e.printStackTrace();
			}

		getChart().plot(t, extended);
	}

}
