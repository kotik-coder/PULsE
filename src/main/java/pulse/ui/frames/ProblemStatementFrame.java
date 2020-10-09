package pulse.ui.frames;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.lang.System.err;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.input.InterpolationDataset.StandartType.DENSITY;
import static pulse.input.InterpolationDataset.StandartType.HEAT_CAPACITY;
import static pulse.problem.statements.ProblemComplexity.HIGH;
import static pulse.tasks.TaskManager.getManagerInstance;
import static pulse.tasks.logs.Details.INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT;
import static pulse.tasks.logs.Details.MISSING_DIFFERENCE_SCHEME;
import static pulse.tasks.logs.Details.MISSING_PROBLEM_STATEMENT;
import static pulse.tasks.logs.Status.INCOMPLETE;
import static pulse.ui.Messages.getString;
import static pulse.util.ImageUtils.loadIcon;
import static pulse.util.Reflexive.instancesOf;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.Solver;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.Problem;
import pulse.problem.statements.Pulse;
import pulse.tasks.SearchTask;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.ui.components.ProblemTree;
import pulse.ui.components.PropertyHolderTable;
import pulse.ui.components.PulseChart;
import pulse.ui.components.buttons.LoaderButton;
import pulse.ui.components.panels.SettingsToolBar;
import pulse.ui.frames.TaskControlFrame.Mode;

@SuppressWarnings("serial")
public class ProblemStatementFrame extends JInternalFrame {

	private InternalGraphFrame<Pulse> pulseFrame;

	private PropertyHolderTable problemTable, schemeTable;
	private SchemeSelectionList schemeSelectionList;
	private ProblemTree problemTree;

	private final static List<Problem> knownProblems = instancesOf(Problem.class);

	/**
	 * Create the frame.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ProblemStatementFrame() {
		setResizable(true);
		setClosable(true);
		setMaximizable(true);
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		setTitle(getString("ProblemStatementFrame.Title")); //$NON-NLS-1$

		setBounds(100, 100, WIDTH, HEIGHT);

		getContentPane().setLayout(new BorderLayout());

		/*
		 * Create a 2x2 grid for lists and tables
		 */

		var contentPane = new JPanel();
		var layout = new GridLayout(2, 2);
		layout.setHgap(5);
		layout.setVgap(5);
		contentPane.setLayout(layout);

		/*
		 * Problem selection list and scroller
		 */

		problemTree = new ProblemTree(knownProblems);
		contentPane.add(new JScrollPane(problemTree));

		var instance = getManagerInstance();

		problemTree.addProblemSelectionListener(e -> {

			var newlySelectedProblem = e.getProblem();

			if (newlySelectedProblem == null) {

				((DefaultTableModel) problemTable.getModel()).setRowCount(0);

			}

			else {

				var selectedTask = instance.getSelectedTask();

				if (e.getSource() != instance) {
					if (instance.isSingleStatement())
						instance.getTaskList().stream().forEach(t -> changeProblem(t, newlySelectedProblem));
					else
						changeProblem(selectedTask, newlySelectedProblem);
				}

				problemTable.setPropertyHolder(selectedTask.getCurrentCalculation().getProblem());
				// after problem is selected for this task, show available difference schemes
				var defaultModel = (DefaultListModel<DifferenceScheme>) (schemeSelectionList.getModel());
				defaultModel.clear();
				var schemes = newlySelectedProblem.availableSolutions();
				schemes.forEach(s -> defaultModel.addElement(s));
				selectDefaultScheme(schemeSelectionList, selectedTask.getCurrentCalculation().getProblem());
				schemeSelectionList.setToolTipText(null);

			}

		});

		/*
		 * Scheme list and scroller
		 */

		schemeSelectionList = new SchemeSelectionList();
		schemeSelectionList.setToolTipText(getString("ProblemStatementFrame.PleaseSelect")); //$NON-NLS-1$

		var schemeScroller = new JScrollPane(schemeSelectionList);
		contentPane.add(schemeScroller);

		/*
		 * Problem details scroller
		 */

		problemTable = new PropertyHolderTable(null);
		var problemDetailsScroller = new JScrollPane(problemTable);
		contentPane.add(problemDetailsScroller);

		/*
		 * Scheme details table and scroller
		 */

		schemeTable = new PropertyHolderTable(null);
		var schemeDetailsScroller = new JScrollPane(schemeTable);
		contentPane.add(schemeDetailsScroller);

		/*
		 * Toolbar
		 */

		var toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setLayout(new GridLayout());

		var btnSimulate = new JButton(getString("ProblemStatementFrame.SimulateButton")); //$NON-NLS-1$

		pulseFrame = new InternalGraphFrame<Pulse>("Pulse Shape", new PulseChart("Time (ms)", "Laser Power (a. u.)"));
		pulseFrame.setFrameIcon(loadIcon("pulse.png", 20));
		pulseFrame.setVisible(false);

		// simulate btn listener

		btnSimulate.addActionListener((ActionEvent arg0) -> {
			var t = instance.getSelectedTask();
			if (t == null)
				return;
			var calc = t.getCurrentCalculation();
			if (t.checkProblems() == INCOMPLETE) {
				var d = calc.getStatus().getDetails();
				if (d == MISSING_PROBLEM_STATEMENT || d == MISSING_DIFFERENCE_SCHEME
						|| d == INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT) {
					getDefaultToolkit().beep();
					showMessageDialog(getWindowAncestor((Component) arg0.getSource()), calc.getStatus().getMessage(),
							getString("ProblemStatementFrame.ErrorTitle"), //$NON-NLS-1$
							ERROR_MESSAGE);
					return;
				}
			}
			try {
				((Solver) calc.getScheme()).solve(calc.getProblem());
			} catch (SolverException e) {
				err.println("Solver of " + t + " has encountered an error. Details: ");
				e.printStackTrace();
			}
			MainGraphFrame.getInstance().plot();
			pulseFrame.plot(calc.getProblem().getPulse());
			problemTable.updateTable();
			schemeTable.updateTable();
		});

		toolBar.add(btnSimulate);

		var btnLoadCv = new LoaderButton(getString("ProblemStatementFrame.LoadSpecificHeatButton")); //$NON-NLS-1$
		btnLoadCv.setDataType(HEAT_CAPACITY);
		toolBar.add(btnLoadCv);

		var btnLoadDensity = new LoaderButton(getString("ProblemStatementFrame.LoadDensityButton")); //$NON-NLS-1$
		btnLoadDensity.setDataType(DENSITY);
		toolBar.add(btnLoadDensity);

		problemTree.setSelectionModel(new DefaultTreeSelectionModel() {

			@Override
			public void setSelectionPath(TreePath path) {
				var object = (DefaultMutableTreeNode) path.getLastPathComponent();

				if (!(object.getUserObject() instanceof Problem))
					super.setSelectionPath(path);

				else {

					var problem = (Problem) object.getUserObject();
					var enabledFlag = problem.isEnabled();

					if (enabledFlag) {
						super.setSelectionPath(path);
						if(!problem.isReady()) {
							btnLoadDensity.highlightIfNeeded();
							btnLoadCv.highlightIfNeeded();
						}
					} else {
						showMessageDialog(null,
								"This problem statement is not currently supported. Please select another.",
								"Feature not supported", WARNING_MESSAGE);
						path = null;
					}

				}

			}

		});

		/*
		 * 
		 */

		getContentPane().add(new SettingsToolBar(problemTable, schemeTable), NORTH);
		getContentPane().add(contentPane, CENTER);
		getContentPane().add(toolBar, SOUTH);

		/*
		 * listeners
		 */

		instance.addSelectionListener((TaskSelectionEvent e) -> update(instance.getSelectedTask()));
		// TODO

		getManagerInstance().addHierarchyListener(event -> {
			if ((event.getSource() instanceof PropertyHolderTable) && instance.isSingleStatement())
				instance.getTaskList().stream().map(t -> t.getCurrentCalculation().getProblem()).filter(p -> p != null)
				.forEach(pp -> pp.updateProperty(event, event.getProperty()));

		});

	}

	public void update() {
		update(getManagerInstance().getSelectedTask());
	}

	private void update(SearchTask selectedTask) {

		var calc = selectedTask.getCurrentCalculation();
		var selectedProblem = selectedTask == null ? null : calc.getProblem();
		var selectedScheme = selectedTask == null ? null : calc.getScheme();

		// problem

		if (selectedProblem == null)
			problemTree.clearSelection();
		else
			problemTree.setSelectedProblem(selectedProblem);

		// scheme

		if (selectedScheme == null)
			schemeSelectionList.clearSelection();
		else {
			setSelectedElement(schemeSelectionList, selectedScheme);
			schemeTable.setPropertyHolder(selectedScheme);
		}

	}

	private void changeProblem(SearchTask task, Problem newProblem) {
		var data = task.getExperimentalCurve();
		var calc = task.getCurrentCalculation();
		var oldProblem = calc.getProblem(); // stores previous information
		var np = newProblem.copy();

		if (oldProblem != null) {
			np.initProperties(oldProblem.getProperties().copy());
			np.getPulse().initFrom(oldProblem.getPulse());
		}

		calc.setProblem(np, data); // copies information from old problem to new problem type

		task.checkProblems();
	}

	private static void selectDefaultScheme(JList<DifferenceScheme> list, Problem p) {
		var defaultSchemeClass = p.defaultScheme();

		var model = list.getModel();
		DifferenceScheme element = null;

		for (int i = 0, size = model.getSize(); i < size; i++) {
			element = model.getElementAt(i);

			if (defaultSchemeClass.isAssignableFrom(element.getClass())) {
				list.setSelectedValue(element, true);
				break;
			}
		}

	}

	private void changeScheme(SearchTask task, DifferenceScheme newScheme) {

		// TODO

		var calc = task.getCurrentCalculation();
		var data = task.getExperimentalCurve();

		if (calc.getScheme() == null)
			calc.setScheme(newScheme.copy(), data);

		else {

			var oldScheme = calc.getScheme().copy(); // stores previous information
			calc.setScheme(newScheme.copy(), data); // assigns new problem type

			if (newScheme.getClass().getSimpleName().equals(oldScheme.getClass().getSimpleName()))
				calc.getScheme().copyFrom(oldScheme); // copies information from old problem to new problem type

			oldScheme = null; // deletes reference to old problem

		}

		task.checkProblems();

	}

	private void setSelectedElement(JList<?> list, Object o) {
		if (o == null) {
			list.clearSelection();
			return;
		}

		var size = list.getModel().getSize();
		Object fromList = null;
		var found = false;

		for (var i = 0; i < size; i++) {
			fromList = list.getModel().getElementAt(i);
			if (fromList.toString().equals(o.toString())) {
				list.setSelectedIndex(i);
				found = true;
			}
		}

		if (!found)
			list.clearSelection();

	}

	/*
	 * ########################### Scheme selection list class
	 * ###########################
	 */

	class SchemeSelectionList extends JList<DifferenceScheme> {

		public SchemeSelectionList() {

			super();
			setSelectionMode(SINGLE_SELECTION);
			var m = new DefaultListModel<DifferenceScheme>();
			setModel(m);
			// scheme list listener

			addListSelectionListener((ListSelectionEvent arg0) -> {
				if (TaskControlFrame.getInstance().getMode() != Mode.PROBLEM)
					return;

				if (arg0.getValueIsAdjusting() || !(getSelectedValue() instanceof DifferenceScheme)) {
					((DefaultTableModel) schemeTable.getModel()).setRowCount(0);
					return;
				}

				var instance = getManagerInstance();
				var selectedTask = instance.getSelectedTask();
				var newScheme = getSelectedValue();
				if (newScheme == null)
					return;
				if (instance.isSingleStatement()) {
					instance.getTaskList().stream().forEach(t -> changeScheme(t, newScheme));
				} else {
					changeScheme(selectedTask, newScheme);
				}
				schemeTable.setPropertyHolder(selectedTask.getCurrentCalculation().getScheme());
				if (selectedTask.getCurrentCalculation().getProblem().getComplexity() == HIGH) {
					showMessageDialog(null, "<html><body><p style='width: 300px;'>" + "You have selected a "
							+ "high-complexity problem statement. Calculations will take longer than usual. "
							+ "You may track the progress of your task with the verbose logging option. Watch out for "
							+ "timeouts as they typically may occur for multi-variate optimisation when the problem is ill-posed."
							+ "</p></body></html>", "High complexity", INFORMATION_MESSAGE);
				}
			});

		}

	}

	public InternalGraphFrame<Pulse> getPulseFrame() {
		return pulseFrame;
	}

}