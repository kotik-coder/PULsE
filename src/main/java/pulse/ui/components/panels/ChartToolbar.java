package pulse.ui.components.panels;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.text.NumberFormatter;

import pulse.input.ExperimentalData;
import pulse.input.Range;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.ui.Launcher;
import pulse.ui.Messages;
import pulse.ui.components.Chart;
import pulse.ui.components.listeners.PlotRequestListener;

@SuppressWarnings("serial")
public class ChartToolbar extends JPanel {

	private final static int ICON_SIZE = 16;
	private List<PlotRequestListener> listeners;

	public ChartToolbar() {
		super();
		listeners = new ArrayList<>();
		initComponents();
	}

	public void initComponents() {
		setLayout(new GridBagLayout());

		var lowerLimitField = new JFormattedTextField(new NumberFormatter());
		var upperLimitField = new JFormattedTextField(new NumberFormatter());

		var limitRangeBtn = new JButton();
		var adiabaticSolutionBtn = new JToggleButton();
		var residualsBtn = new JToggleButton();

		var gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 0.25;

		lowerLimitField.setValue(0.0);

		String ghostText1 = "Lower bound";
		lowerLimitField.setText(ghostText1);

		String ghostText2 = "Upper bound";

		add(lowerLimitField, gbc);

		upperLimitField.setValue(1.0);
		upperLimitField.setText(ghostText2);

		add(upperLimitField, gbc);

		limitRangeBtn.setText("Limit Range To");

		lowerLimitField.setForeground(Color.GRAY);
		upperLimitField.setForeground(Color.GRAY);

		var ftfFocusListener = new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				JTextField src = (JTextField) e.getSource();
				if (src.getText().length() > 0)
					src.setForeground(Color.black);
			}

			@Override
			public void focusLost(FocusEvent e) {
				JFormattedTextField src = (JFormattedTextField) e.getSource();
				if (src.getValue() == null) {
					src.setText(ghostText1);
					src.setForeground(Color.gray);
				}
			}

		};

		TaskManager.addSelectionListener(event -> {
			SearchTask t = event.getSelection();

			ExperimentalData expCurve = t.getExperimentalCurve();

			lowerLimitField.setValue(expCurve.getRange().getSegment().getMinimum());
			upperLimitField.setValue(expCurve.getRange().getSegment().getMaximum());

		});

		TaskManager.addTaskRepositoryListener(e -> {

			if (e.getState() == TaskRepositoryEvent.State.TASK_FINISHED) {

				SearchTask t = TaskManager.getSelectedTask();

				if (e.getId().equals(t.getIdentifier())) {
					lowerLimitField.setValue(t.getExperimentalCurve().getRange().getSegment().getMinimum());
					upperLimitField.setValue(t.getExperimentalCurve().getRange().getSegment().getMaximum());
					notifyPlot();
				}

			}

		});

		lowerLimitField.addFocusListener(ftfFocusListener);
		upperLimitField.addFocusListener(ftfFocusListener);

		limitRangeBtn.addActionListener(e -> {
			if ((!lowerLimitField.isEditValid()) || (!upperLimitField.isEditValid())) { // The text is invalid.
				if (userSaysRevert(lowerLimitField)) { // reverted
					lowerLimitField.postActionEvent(); // inform the editor
				}
			}

			else {
				double lower = ((Number) lowerLimitField.getValue()).doubleValue();
				double upper = ((Number) upperLimitField.getValue()).doubleValue();
				validateRange(lower, upper);
				notifyPlot();
			}
		});

		gbc.weightx = 0.25;
		add(limitRangeBtn, gbc);

		adiabaticSolutionBtn.setToolTipText("Sanity check (original adiabatic solution)");
		adiabaticSolutionBtn.setIcon(Launcher.loadIcon("parker.png", ICON_SIZE));

		adiabaticSolutionBtn.addActionListener(e -> {
			Chart.setZeroApproximationShown(adiabaticSolutionBtn.isSelected());
			notifyPlot();
		});

		gbc.weightx = 0.125;
		add(adiabaticSolutionBtn, gbc);

		residualsBtn.setToolTipText("Plot residuals");
		residualsBtn.setIcon(Launcher.loadIcon("residuals.png", ICON_SIZE));
		residualsBtn.setSelected(true);

		residualsBtn.addActionListener(e -> {
			Chart.setResidualsShown(residualsBtn.isSelected());
			notifyPlot();
		});

		gbc.weightx = 0.125;
		add(residualsBtn, gbc);
	}

	public void addPlotRequestListener(PlotRequestListener plotRequestListener) {
		listeners.add(plotRequestListener);
	}

	private void notifyPlot() {
		listeners.stream().forEach(l -> l.onPlotRequest());
	}

	private static boolean userSaysRevert(JFormattedTextField ftf) {
		Toolkit.getDefaultToolkit().beep();
		ftf.selectAll();
		Object[] options = { Messages.getString("NumberEditor.EditText"),
				Messages.getString("NumberEditor.RevertText") };
		int answer = JOptionPane.showOptionDialog(SwingUtilities.getWindowAncestor(ftf),
				"<html>Time domain should be consistent with the experimental data range.<br>"
						+ Messages.getString("NumberEditor.MessageLine1")
						+ Messages.getString("NumberEditor.MessageLine2") + "</html>",
				Messages.getString("NumberEditor.InvalidText"), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE,
				null, options, options[1]);

		if (answer == 1) { // Revert!
			ftf.setValue(ftf.getValue());
			return true;
		}
		return false;
	}

	private void validateRange(double a, double b) {
		SearchTask task = TaskManager.getSelectedTask();

		if (task == null)
			return;

		ExperimentalData expCurve = task.getExperimentalCurve();

		if (expCurve == null)
			return;

		StringBuilder sb = new StringBuilder();

		sb.append("<html><p>");
		sb.append(Messages.getString("RangeSelectionFrame.ConfirmationMessage1"));
		sb.append("</p><br>");
		sb.append(Messages.getString("RangeSelectionFrame.ConfirmationMessage2"));
		sb.append(expCurve.getEffectiveStartTime());
		sb.append(" to ");
		sb.append(expCurve.getEffectiveEndTime());
		sb.append("<br><br>");
		sb.append(Messages.getString("RangeSelectionFrame.ConfirmationMessage3"));
		sb.append(String.format("%3.4f", a) + " to " + String.format("%3.4f", b));
		sb.append("</html>");

		int dialogResult = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this), sb.toString(),
				"Confirm chocie", JOptionPane.YES_NO_OPTION);

		if (dialogResult == JOptionPane.YES_OPTION)
			expCurve.setRange(new Range(a, b));

	}

}