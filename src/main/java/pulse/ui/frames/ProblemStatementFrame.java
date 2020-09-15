package pulse.ui.frames;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.Font.BOLD;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.lang.System.err;
import static javax.swing.BorderFactory.createLineBorder;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.input.InterpolationDataset.StandartType.DENSITY;
import static pulse.input.InterpolationDataset.StandartType.HEAT_CAPACITY;
import static pulse.problem.statements.Problem.isSingleStatement;
import static pulse.problem.statements.ProblemComplexity.HIGH;
import static pulse.tasks.Status.INCOMPLETE;
import static pulse.tasks.Status.Details.INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT;
import static pulse.tasks.Status.Details.MISSING_DIFFERENCE_SCHEME;
import static pulse.tasks.Status.Details.MISSING_PROBLEM_STATEMENT;
import static pulse.tasks.TaskManager.addSelectionListener;
import static pulse.tasks.TaskManager.getInstance;
import static pulse.tasks.TaskManager.getSelectedTask;
import static pulse.tasks.TaskManager.getTaskList;
import static pulse.tasks.TaskManager.selectFirstTask;
import static pulse.ui.Messages.getString;
import static pulse.util.Reflexive.instancesOf;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.Solver;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.Problem;
import pulse.tasks.SearchTask;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.ui.components.PropertyHolderTable;
import pulse.ui.components.buttons.LoaderButton;
import pulse.ui.components.controllers.ProblemListCellRenderer;
import pulse.ui.components.panels.SettingsToolBar;

@SuppressWarnings("serial")
public class ProblemStatementFrame extends JInternalFrame {

	private PropertyHolderTable problemTable, schemeTable;
	private SchemeSelectionList schemeSelectionList;
	private ProblemList problemList;

	private final static int LIST_FONT_SIZE = 12;

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

		LoaderButton btnLoadCv, btnLoadDensity;

		/*
		 * Problem selection list and scroller
		 */

		problemList = new ProblemList();
		contentPane.add(new JScrollPane(problemList));

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

		problemTable = new PropertyHolderTable(null) {

			/**
			 * 
			 */
			private static final long serialVersionUID = -6578898159810827328L;

			@Override
			public void updateTable() {
				super.updateTable();
				((DefaultRowSorter<?, Integer>) getRowSorter()).sort();
			}

		};

		var problemDetailsScroller = new JScrollPane(problemTable);
		contentPane.add(problemDetailsScroller);

		/*
		 * Scheme details table and scroller
		 */

		schemeTable = new PropertyHolderTable(null); // TODO
		var schemeDetailsScroller = new JScrollPane(schemeTable);
		contentPane.add(schemeDetailsScroller);

		/*
		 * Toolbar
		 */

		var toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setLayout(new GridLayout());

		var btnSimulate = new JButton(getString("ProblemStatementFrame.SimulateButton")); //$NON-NLS-1$
		btnSimulate.setFont(btnSimulate.getFont().deriveFont(BOLD, 14f));

		// simulate btn listener

		btnSimulate.addActionListener((ActionEvent arg0) -> {
			var t = getSelectedTask();
			if (t == null)
				return;
			if (t.checkProblems() == INCOMPLETE) {
				var d = t.getStatus().getDetails();
				if (d == MISSING_PROBLEM_STATEMENT || d == MISSING_DIFFERENCE_SCHEME
						|| d == INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT) {
					getDefaultToolkit().beep();
					showMessageDialog(getWindowAncestor((Component) arg0.getSource()), t.getStatus().getMessage(),
							getString("ProblemStatementFrame.ErrorTitle"), //$NON-NLS-1$
							ERROR_MESSAGE);
					return;
				}
			}
			try {
				((Solver) t.getScheme()).solve(t.getProblem());
			} catch (SolverException e) {
				err.println("Solver of " + t + " has encountered an error. Details: ");
				e.printStackTrace();
			}
			MainGraphFrame.getInstance().plot();
			AuxGraphFrame.getInstance().plot();
			problemTable.updateTable();
			schemeTable.updateTable();
		});

		toolBar.add(btnSimulate);

		btnLoadCv = new LoaderButton(getString("ProblemStatementFrame.LoadSpecificHeatButton")); //$NON-NLS-1$
		btnLoadCv.setDataType(HEAT_CAPACITY);
		toolBar.add(btnLoadCv);

		btnLoadDensity = new LoaderButton(getString("ProblemStatementFrame.LoadDensityButton")); //$NON-NLS-1$
		btnLoadDensity.setDataType(DENSITY);
		toolBar.add(btnLoadDensity);

		problemList.setSelectionModel(new DefaultListSelectionModel() {

			@Override
			public void setSelectionInterval(int index0, int index1) {
				if (index0 != index1)
					return;

				var problem = knownProblems.get(index0);
				var enabledFlag = problem.isEnabled();

				if (enabledFlag) {
					super.setSelectionInterval(index0, index0);
					problemList.ensureIndexIsVisible(index0);

					if (!problem.isReady()) {
						var bred = new Color(1.0f, 0.0f, 0.0f, 0.35f);
						btnLoadDensity.setBorder(createLineBorder(bred, 3));
						btnLoadCv.setBorder(createLineBorder(bred, 3));
					} else {
						btnLoadDensity.setBorder(null);
						btnLoadCv.setBorder(null);
					}

				} else
					showMessageDialog(null, "This problem statement is not currently supported. Please select another.",
							"Feature not supported", WARNING_MESSAGE);

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

		addSelectionListener((TaskSelectionEvent e) -> 
			update(e.getSelection())
		);
		// TODO

		getInstance().addHierarchyListener(event -> {
			if (!(event.getSource() instanceof PropertyHolderTable))
				return;

			if (!isSingleStatement())
				return;

			Problem p;

			for (var task : getTaskList()) {
				p = task.getProblem();
				if (p != null)
					p.updateProperty(event, event.getProperty());
			}

		});

	}

	public void update() {
		update(getSelectedTask());
	}

	private void update(SearchTask selectedTask) {

		var selectedProblem = selectedTask == null ? null : selectedTask.getProblem();
		var selectedScheme = selectedTask == null ? null : selectedTask.getScheme();

		// problem

		if (selectedProblem == null)
			problemList.clearSelection();
		else {
			setSelectedElement(problemList, selectedProblem);
			problemTable.setPropertyHolder(selectedProblem);

		}

		// scheme

		if (selectedScheme == null)
			schemeSelectionList.clearSelection();
		else {
			setSelectedElement(schemeSelectionList, selectedScheme);
			schemeTable.setPropertyHolder(selectedScheme);
		}

	}

	private void changeProblem(SearchTask task, Problem newProblem) {
		var oldProblem = task.getProblem(); // stores previous information

		var problemClass = newProblem.getClass();
		Constructor<? extends Problem> problemCopyConstructor = null;

		try {
			if (newProblem != null) {
				problemCopyConstructor = newProblem.getClass().getConstructor(Problem.class);
			}
		} catch (NoSuchMethodException | SecurityException e) {
			err.println(getString("ProblemStatementFrame.ConstructorAccessError") + problemClass); //$NON-NLS-1$
			e.printStackTrace();
		}

		Problem np = null;

		try {
			if (problemCopyConstructor != null && oldProblem != null)
				np = problemCopyConstructor.newInstance(oldProblem);
			else
				np = newProblem.getClass().getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			err.println(getString("ProblemStatementFrame.InvocationError") + problemCopyConstructor); //$NON-NLS-1$
			e.printStackTrace();
		}

		task.setProblem(np); // copies information from old problem to new problem type

		oldProblem = null;
		problemCopyConstructor = null;
		problemClass = null;

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

		if (task.getScheme() == null) {
			task.setScheme(newScheme.copy());
			// task.getScheme().setTimeLimit( task.getTimeLimit() );
		}

		else {

			var oldScheme = task.getScheme().copy(); // stores previous information
			task.setScheme(null);
			task.setScheme(newScheme.copy()); // assigns new problem type

			if (newScheme.getClass().getSimpleName().equals(oldScheme.getClass().getSimpleName()))
				task.getScheme().copyFrom(oldScheme); // copies information from old problem to new problem type
			// else
			// task.getScheme().setTimeLimit( task.getTimeLimit() );

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
	 * ################## Problem List Class ##################
	 */

	class ProblemList extends JList<Problem> {

		public ProblemList() {
			super();
			setFont(getFont().deriveFont(LIST_FONT_SIZE));
			this.setCellRenderer(new ProblemListCellRenderer());

			var listModel = new DefaultListModel<Problem>();
			for (var p : knownProblems) {
				listModel.addElement(p);
			}

			setModel(listModel);
			setSelectionMode(SINGLE_SELECTION);

			addListSelectionListener((ListSelectionEvent arg0) -> {
				if (arg0.getValueIsAdjusting())
					return;
				var newlySelectedProblem = getSelectedValue();
				if (newlySelectedProblem == null) {
					((DefaultTableModel) problemTable.getModel()).setRowCount(0);
					return;
				}
				if (getSelectedTask() == null) {
					selectFirstTask();
				}
				var selectedTask = getSelectedTask();
				if (isSingleStatement()) {
					for (var t : getTaskList()) {
						changeProblem(t, newlySelectedProblem);
					}
				} else {
					changeProblem(selectedTask, newlySelectedProblem);
				}
				listModel.set(listModel.indexOf(newlySelectedProblem), selectedTask.getProblem());
				problemTable.setPropertyHolder(getSelectedTask().getProblem());
				// after problem is selected for this task, show available difference schemes
				var defaultModel = (DefaultListModel<DifferenceScheme>) (schemeSelectionList.getModel());
				defaultModel.clear();
				var schemes = newlySelectedProblem.availableSolutions();
				schemes.forEach(s -> defaultModel.addElement(s));
				selectDefaultScheme(schemeSelectionList, selectedTask.getProblem());
				schemeSelectionList.setToolTipText(null);
			});

			addSelectionListener((TaskSelectionEvent e) -> {
				// select appropriate problem type from list
				if (e.getSelection().getProblem() != null) {
					for (var i = 0; i < getModel().getSize(); i++) {
						var p = getModel().getElementAt(i);
						if (e.getSelection().getProblem().getClass().equals(p.getClass())) {
							setSelectedIndex(i);
							break;
						}
					}
				}
				// then, select appropriate scheme type
				if (e.getSelection().getScheme() != null) {
					for (var i = 0; i < schemeSelectionList.getModel().getSize(); i++) {
						if (e.getSelection().getScheme().getClass()
								.equals(schemeSelectionList.getModel().getElementAt(i).getClass())) {
							schemeSelectionList.setSelectedIndex(i);
							break;
						}
					}
				}
			});

		}

	}

	/*
	 * ########################### Scheme selection list class
	 * ###########################
	 */

	class SchemeSelectionList extends JList<DifferenceScheme> {

		public SchemeSelectionList() {

			super();
			setFont(getFont().deriveFont(LIST_FONT_SIZE));
			setSelectionMode(SINGLE_SELECTION);
			var m = new DefaultListModel<DifferenceScheme>();
			setModel(m);
			// scheme list listener

			addListSelectionListener((ListSelectionEvent arg0) -> {
				if (arg0.getValueIsAdjusting())
					return;
				if (!(getSelectedValue() instanceof DifferenceScheme)) {
					((DefaultTableModel) schemeTable.getModel()).setRowCount(0);
					return;
				}
				var selectedTask = getSelectedTask();
				var newScheme = getSelectedValue();
				if (newScheme == null)
					return;
				if (isSingleStatement()) {
					for (var t : getTaskList()) {
						changeScheme(t, newScheme);
					}
				} else {
					changeScheme(selectedTask, newScheme);
				}
				schemeTable.setPropertyHolder(selectedTask.getScheme());
				if (selectedTask.getProblem().getComplexity() == HIGH) {
					showMessageDialog(null, "<html><body><p style='width: 300px;'>" + "You have selected a "
							+ "high-complexity problem statement. Calculations will take longer than usual. "
							+ "You may track the progress of your task with the verbose logging option. Watch out for "
							+ "timeouts as they typically may occur for multi-variate optimisation when the problem is ill-posed."
							+ "</p></body></html>", "High complexity", INFORMATION_MESSAGE);
				}
			});

		}

	}

}