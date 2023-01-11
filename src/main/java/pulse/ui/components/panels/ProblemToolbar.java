package pulse.ui.components.panels;

import static java.awt.Toolkit.getDefaultToolkit;
import static java.lang.System.err;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.input.InterpolationDataset.StandartType.DENSITY;
import static pulse.input.InterpolationDataset.StandartType.HEAT_CAPACITY;
import static pulse.tasks.logs.Status.INCOMPLETE;
import static pulse.ui.Messages.getString;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JToolBar;

import pulse.problem.schemes.solvers.Solver;
import pulse.problem.schemes.solvers.SolverException;
import pulse.tasks.Calculation;
import pulse.tasks.TaskManager;
import pulse.ui.components.buttons.LoaderButton;
import pulse.ui.frames.MainGraphFrame;
import pulse.ui.frames.TaskControlFrame;

@SuppressWarnings("serial")
public class ProblemToolbar extends JToolBar {

    private JButton btnSimulate;
    private LoaderButton btnLoadCv;
    private LoaderButton btnLoadDensity;

    public ProblemToolbar() {
        super();
        setFloatable(false);
        setLayout(new GridLayout());

        btnSimulate = new JButton(getString("ProblemStatementFrame.SimulateButton")); //$NON-NLS-1$
        add(btnSimulate);

        btnLoadCv = new LoaderButton(getString("ProblemStatementFrame.LoadSpecificHeatButton")); //$NON-NLS-1$
        btnLoadCv.setDataType(HEAT_CAPACITY);
        add(btnLoadCv);

        btnLoadDensity = new LoaderButton(getString("ProblemStatementFrame.LoadDensityButton")); //$NON-NLS-1$
        btnLoadDensity.setDataType(DENSITY);
        add(btnLoadDensity);

        btnSimulate.addActionListener((ActionEvent e)
                -> plot(e));
    }

    public static void plot(ActionEvent e) {
        var instance = TaskManager.getManagerInstance();

        if (instance.getSelectedTask() == null) {
            instance.selectFirstTask();
        }

        var t = instance.getSelectedTask();

        var calc = (Calculation) t.getResponse();

        t.checkProblems();
        var status = t.getStatus();

        if (status == INCOMPLETE && !status.checkProblemStatementSet()) {

            getDefaultToolkit().beep();
            showMessageDialog(getWindowAncestor((Component) e.getSource()), 
                    calc.getStatus().getMessage(),
                    getString("ProblemStatementFrame.ErrorTitle"), //$NON-NLS-1$
                    ERROR_MESSAGE);

        } else {
            try {
                Solver solver = (Solver) calc.getScheme();
                solver.solve(calc.getProblem());
            } catch (SolverException se) {
                err.println("Solver of " + t + " has encountered an error. Details: ");
                se.printStackTrace();
            }
            MainGraphFrame.getInstance().plot();
            TaskControlFrame.getInstance().getPulseFrame().plot(calc);
        }

    }

    public void highlightButtons(boolean highlight) {
        if (highlight) {
            btnLoadDensity.highlightIfNeeded();
            btnLoadCv.highlightIfNeeded();
        } else {
            btnLoadDensity.highlight(false);
            btnLoadCv.highlight(false);
        }
    }

}
