package pulse.ui.components.panels;

import static java.awt.GridBagConstraints.BOTH;
import static javax.swing.JOptionPane.showOptionDialog;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.LOWER_BOUND;
import static pulse.properties.NumericPropertyKeyword.UPPER_BOUND;
import static pulse.ui.Messages.getString;
import static pulse.ui.frames.MainGraphFrame.getChart;
import static pulse.util.ImageUtils.loadIcon;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import pulse.input.ExperimentalData;

import pulse.input.Range;
import pulse.tasks.Calculation;
import pulse.tasks.TaskManager;
import pulse.ui.Messages;
import pulse.ui.components.RangeTextFields;
import pulse.ui.components.ResidualsChart;
import pulse.ui.components.listeners.PlotRequestListener;
import pulse.ui.frames.HistogramFrame;

@SuppressWarnings("serial")
public final class ChartToolbar extends JToolBar {

    private final static int ICON_SIZE = 16;
    private List<PlotRequestListener> listeners;

    private RangeTextFields rtf;

    public ChartToolbar() {
        super();
        setFloatable(false);
        listeners = new ArrayList<>();
        rtf = new RangeTextFields();
        initComponents();
    }

    public final void initComponents() {
        setLayout(new GridBagLayout());

        var limitRangeBtn = new JButton();
        var adiabaticSolutionBtn = new JToggleButton(loadIcon("parker.png", ICON_SIZE, Color.white));
        var residualsBtn = new JToggleButton(loadIcon("residuals.png", ICON_SIZE, Color.white));
        var pdfBtn = new JButton(loadIcon("pdf.png", ICON_SIZE, Color.white));
        pdfBtn.setToolTipText("Residuals Histogram");

        var residualsChart = new ResidualsChart("Residual value", "Frequency");
        var chFrame = new HistogramFrame(residualsChart, 450, 450);

        pdfBtn.addActionListener(e -> {

            var task = TaskManager.getManagerInstance().getSelectedTask();
            var calc = (Calculation) task.getResponse();

            if (task != null && calc.getModelSelectionCriterion() != null) {

                chFrame.setLocationRelativeTo(null);
                chFrame.setVisible(true);
                chFrame.plot(calc.getOptimiserStatistic());

            }

        });

        var gbc = new GridBagConstraints();
        gbc.fill = BOTH;
        gbc.weightx = 0.25;

        add(rtf.getLowerLimitField(), gbc);
        add(rtf.getUpperLimitField(), gbc);

        limitRangeBtn.setText("Set Range");
        limitRangeBtn.addActionListener(e -> {
            var lower = ((Number) rtf.getLowerLimitField().getValue()).doubleValue();
            var upper = ((Number) rtf.getUpperLimitField().getValue()).doubleValue();
            validateRange(lower, upper);
            notifyPlot();
        });

        gbc.weightx = 0.25;
        add(limitRangeBtn, gbc);

        adiabaticSolutionBtn.setToolTipText("Sanity check (original adiabatic solution)");

        adiabaticSolutionBtn.addActionListener(e -> {
            getChart().setZeroApproximationShown(adiabaticSolutionBtn.isSelected());
            notifyPlot();
        });

        gbc.weightx = 0.08;
        add(adiabaticSolutionBtn, gbc);

        residualsBtn.setToolTipText("Plot residuals");
        residualsBtn.setSelected(true);

        residualsBtn.addActionListener(e -> {
            getChart().setResidualsShown(residualsBtn.isSelected());
            notifyPlot();
        });

        add(residualsBtn, gbc);
        add(pdfBtn, gbc);
    }

    public void addPlotRequestListener(PlotRequestListener plotRequestListener) {
        listeners.add(plotRequestListener);
    }

    private void notifyPlot() {
        listeners.stream().forEach(l -> l.onPlotRequest());
    }

    private void validateRange(double a, double b) {
        var task = TaskManager.getManagerInstance().getSelectedTask();

        if (task == null) {
            return;
        }

        var expCurve = (ExperimentalData) task.getInput();

        if (expCurve == null) {
            return;
        }

        var sb = new StringBuilder();

        sb.append(Messages.getString("TextWrap.0"))
                .append(getString("RangeSelectionFrame.ConfirmationMessage1"))
                .append("<br>")
                .append(getString("RangeSelectionFrame.ConfirmationMessage2"));
        try {
            sb.append(rtf.getLowerLimitField().getFormatter().valueToString(expCurve.getEffectiveStartTime()))
                    .append(" to ")
                    .append(rtf.getUpperLimitField().getFormatter().valueToString(expCurve.getEffectiveEndTime())
                    );
        } catch (ParseException ex) {
            Logger.getLogger(ChartToolbar.class.getName()).log(Level.SEVERE, null, ex);
        }
        sb.append("<br>").append(getString("RangeSelectionFrame.ConfirmationMessage3"))
                .append(rtf.getLowerLimitField().getText())
                .append(" to ")
                .append(rtf.getUpperLimitField().getText())
                .append(Messages.getString("TextWrap.1"));

        String[] options = new String[]{"Apply to all", "Change current", "Cancel"};

        var dialogResult = showOptionDialog(getWindowAncestor(this),
                sb.toString(), "Confirm chocie", JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[2]);

        if (dialogResult == JOptionPane.NO_OPTION) {
            //just set the range for this particular dataset
            setRange(expCurve, a, b);
        } else if (dialogResult == JOptionPane.YES_OPTION) {
            // set range for all available experimental datasets
            TaskManager.getManagerInstance().getTaskList()
                    .stream().forEach((aTask)
                            -> setRange( (ExperimentalData) aTask.getInput(), a, b)
                    );
        }

    }

    private void setRange(ExperimentalData expCurve, double a, double b) {
        if (expCurve.getRange() == null) {
            expCurve.setRange(new Range(a, b));
        } else {
            expCurve.getRange().setLowerBound(derive(LOWER_BOUND, a));
            expCurve.getRange().setUpperBound(derive(UPPER_BOUND, b));
        }
    }

}
