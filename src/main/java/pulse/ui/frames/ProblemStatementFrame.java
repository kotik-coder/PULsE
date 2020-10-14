package pulse.ui.frames;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static pulse.problem.statements.ProblemComplexity.HIGH;
import static pulse.tasks.TaskManager.getManagerInstance;
import static pulse.ui.Messages.getString;
import static pulse.util.Reflexive.instancesOf;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.statements.Problem;
import pulse.tasks.SearchTask;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.ui.components.ProblemTree;
import pulse.ui.components.PropertyHolderTable;
import pulse.ui.components.panels.ProblemToolbar;
import pulse.ui.components.panels.SettingsToolBar;
import pulse.ui.frames.TaskControlFrame.Mode;

@SuppressWarnings("serial")
public class ProblemStatementFrame extends JInternalFrame {

	private PropertyHolderTable problemTable;
	private PropertyHolderTable schemeTable;
	private JList<DifferenceScheme> schemeSelectionList;
	private ProblemTree problemTree;
	private ProblemToolbar toolbar;

	private final static List<Problem> knownProblems = instancesOf(Problem.class);

	/**
	 * Create the frame.
	 */
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

		schemeSelectionList = new JList<DifferenceScheme>();
		schemeSelectionList.setSelectionMode(SINGLE_SELECTION);
		schemeSelectionList.setModel(new DefaultListModel<DifferenceScheme>());

		schemeSelectionList.addListSelectionListener((ListSelectionEvent arg0) -> {
			if (TaskControlFrame.getInstance().getMode() != Mode.PROBLEM)
				return;

			var selectedValue = schemeSelectionList.getSelectedValue();

			if (arg0.getValueIsAdjusting() || !(selectedValue instanceof DifferenceScheme)) {
				((DefaultTableModel) schemeTable.getModel()).setRowCount(0);
				return;
			}

			var selectedTask = instance.getSelectedTask();
			var newScheme = selectedValue;
			if (instance.isSingleStatement()) {
				instance.getTaskList().stream().forEach(t -> changeScheme(t, newScheme));
			} else {
				changeScheme(selectedTask, newScheme);
			}
			schemeTable.setPropertyHolder(selectedTask.getCurrentCalculation().getScheme());
			if (selectedTask.getCurrentCalculation().getProblem().getComplexity() == HIGH) {
				showMessageDialog(null, getString("complexity.warning"), "High complexity", INFORMATION_MESSAGE);
			}
		});

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

		toolbar = new ProblemToolbar();

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
						toolbar.highlightButtons(!problem.isReady());
					} else {
						showMessageDialog(null, getString("problem.notsupportedmessage"),
								getString("problem.notsupportedtitle"), WARNING_MESSAGE);
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
		getContentPane().add(toolbar, SOUTH);

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

		task.checkProblems(true);
		toolbar.highlightButtons(!np.isReady());
		
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

		task.checkProblems(true);

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

}