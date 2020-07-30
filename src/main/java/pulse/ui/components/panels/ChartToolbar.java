package pulse.ui.components.panels;

import static java.awt.Color.GRAY;
import static java.awt.Color.black;
import static java.awt.Color.gray;
import static java.awt.GridBagConstraints.BOTH;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.lang.String.format;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showOptionDialog;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.tasks.TaskManager.addSelectionListener;
import static pulse.tasks.TaskManager.addTaskRepositoryListener;
import static pulse.tasks.TaskManager.getSelectedTask;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_FINISHED;
import static pulse.ui.Launcher.loadIcon;
import static pulse.ui.Messages.getString;
import static pulse.ui.frames.MainGraphFrame.getChart;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.text.NumberFormatter;

import pulse.input.Range;
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
		gbc.fill = BOTH;
		gbc.weightx = 0.25;

		lowerLimitField.setValue(0.0);

		var ghostText1 = "Lower bound";
		lowerLimitField.setText(ghostText1);

		var ghostText2 = "Upper bound";

		add(lowerLimitField, gbc);

		upperLimitField.setValue(1.0);
		upperLimitField.setText(ghostText2);

		add(upperLimitField, gbc);

		limitRangeBtn.setText("Limit Range To");

		lowerLimitField.setForeground(GRAY);
		upperLimitField.setForeground(GRAY);

		var ftfFocusListener = new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				var src = (JTextField) e.getSource();
				if (src.getText().length() > 0)
					src.setForeground(black);
			}

			@Override
			public void focusLost(FocusEvent e) {
				var src = (JFormattedTextField) e.getSource();
				if (src.getValue() == null) {
					src.setText(ghostText1);
					src.setForeground(gray);
				}
			}

		};

		addSelectionListener(event -> {
			var t = event.getSelection();
			var expCurve = t.getExperimentalCurve();

			lowerLimitField.setValue(expCurve.getRange().getSegment().getMinimum());
			upperLimitField.setValue(expCurve.getRange().getSegment().getMaximum());

		});

		addTaskRepositoryListener(e -> {

			if (e.getState() == TASK_FINISHED) {

				var t = getSelectedTask();

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
				var lower = ((Number) lowerLimitField.getValue()).doubleValue();
				var upper = ((Number) upperLimitField.getValue()).doubleValue();
				validateRange(lower, upper);
				notifyPlot();
			}
		});

		gbc.weightx = 0.25;
		add(limitRangeBtn, gbc);

		adiabaticSolutionBtn.setToolTipText("Sanity check (original adiabatic solution)");
		adiabaticSolutionBtn.setIcon(loadIcon("parker.png", ICON_SIZE));

		adiabaticSolutionBtn.addActionListener(e -> {
			getChart().setZeroApproximationShown(adiabaticSolutionBtn.isSelected());
			notifyPlot();
		});

		gbc.weightx = 0.125;
		add(adiabaticSolutionBtn, gbc);

		residualsBtn.setToolTipText("Plot residuals");
		residualsBtn.setIcon(loadIcon("residuals.png", ICON_SIZE));
		residualsBtn.setSelected(true);

		residualsBtn.addActionListener(e -> {
			getChart().setResidualsShown(residualsBtn.isSelected());
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
		getDefaultToolkit().beep();
		ftf.selectAll();
		Object[] options = { getString("NumberEditor.EditText"), getString("NumberEditor.RevertText") };
		var answer = showOptionDialog(getWindowAncestor(ftf),
				"<html>Time domain should be consistent with the experimental data range.<br>"
						+ getString("NumberEditor.MessageLine1") + getString("NumberEditor.MessageLine2") + "</html>",
				getString("NumberEditor.InvalidText"), YES_NO_OPTION, ERROR_MESSAGE, null, options, options[1]);

		if (answer == 1) { // Revert!
			ftf.setValue(ftf.getValue());
			return true;
		}
		return false;
	}

	private void validateRange(double a, double b) {
		var task = getSelectedTask();

		if (task == null)
			return;

		var expCurve = task.getExperimentalCurve();

		if (expCurve == null)
			return;

		var sb = new StringBuilder();

		sb.append("<html><p>");
		sb.append(getString("RangeSelectionFrame.ConfirmationMessage1"));
		sb.append("</p><br>");
		sb.append(getString("RangeSelectionFrame.ConfirmationMessage2"));
		sb.append(expCurve.getEffectiveStartTime());
		sb.append(" to ");
		sb.append(expCurve.getEffectiveEndTime());
		sb.append("<br><br>");
		sb.append(getString("RangeSelectionFrame.ConfirmationMessage3"));
		sb.append(format("%3.4f", a) + " to " + format("%3.4f", b));
		sb.append("</html>");

		var dialogResult = showConfirmDialog(getWindowAncestor(this), sb.toString(), "Confirm chocie", YES_NO_OPTION);

		if (dialogResult == YES_OPTION)
			expCurve.setRange(new Range(a, b));

	}

}