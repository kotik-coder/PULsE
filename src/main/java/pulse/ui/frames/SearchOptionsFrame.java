package pulse.ui.frames;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import pulse.search.direction.PathOptimiser;
import pulse.search.linear.LinearOptimiser;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.ui.Messages;
import pulse.ui.components.PropertyHolderTable;
import pulse.ui.components.controllers.SearchListRenderer;
import pulse.util.Reflexive;

@SuppressWarnings("serial")
public class SearchOptionsFrame extends JInternalFrame {

	private PropertyHolderTable pathTable;
	private JList<LinearOptimiser> linearList;
	private PathSolversList pathList;

	private final static Font font = new Font(Messages.getString("PropertyHolderTable.FontName"), Font.ITALIC, 16);

	private final static List<PathOptimiser> pathSolvers = Reflexive.instancesOf(PathOptimiser.class);
	private final static List<LinearOptimiser> linearSolvers = Reflexive.instancesOf(LinearOptimiser.class);

	/**
	 * Create the frame.
	 */
	public SearchOptionsFrame() {
		setClosable(true);
		setTitle(Messages.getString("SearchOptionsFrame.SelectSearch")); //$NON-NLS-1$
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		setBounds(100, 100, WIDTH, HEIGHT);

		/*
		 * Path solver list and scroller
		 */

		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(panel);

		pathList = new PathSolversList();
		JScrollPane pathListScroller = new JScrollPane(pathList);
		pathListScroller.setBorder(BorderFactory.createTitledBorder("Select a Direction Search Method"));

		linearList = new LinearSearchList();
		linearList.setEnabled(false);
		JScrollPane linearListScroller = new JScrollPane(linearList);
		linearListScroller.setBorder(BorderFactory.createTitledBorder("Select a Line Search Method"));

		pathTable = new PropertyHolderTable(null);

		getContentPane().setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridy = 0;
		gbc.gridx = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 0.2;

		getContentPane().add(pathListScroller, gbc);

		gbc.gridy = 1;

		getContentPane().add(linearListScroller, gbc);

		gbc.gridy = 2;
		gbc.weighty = 0.6;

		JScrollPane tableScroller = new JScrollPane(pathTable);
		tableScroller.setBorder(BorderFactory.createTitledBorder("Select search variables and settings"));
		getContentPane().add(tableScroller, gbc);

	}

	public void update() {
		var selected = PathOptimiser.getSelectedPathOptimiser();
		if (selected != null) {
			pathList.setSelectedIndex(pathSolvers.indexOf(selected));
			linearList.setSelectedIndex(linearSolvers.indexOf(PathOptimiser.getLinearSolver()));
			pathTable.updateTable();
		} else {
			pathList.clearSelection();
			linearList.clearSelection();
			linearList.setEnabled(false);
		}
	}

	class PathSolversList extends JList<PathOptimiser> {

		/**
		 * 
		 */
		private static final long serialVersionUID = 3662972578473909850L;

		public PathSolversList() {

			super();

			setModel(new AbstractListModel<PathOptimiser>() {
				/**
				 * 
				 */
				private static final long serialVersionUID = -7683200230096704268L;

				@Override
				public int getSize() {
					return pathSolvers.size();
				}

				@Override
				public PathOptimiser getElementAt(int index) {
					return pathSolvers.get(index);
				}
			});

			setFont(font);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setCellRenderer(new SearchListRenderer());

			addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent arg0) {

					if (arg0.getValueIsAdjusting())
						return;

					if (!(getSelectedValue() instanceof PathOptimiser)) {
						((DefaultTableModel) pathTable.getModel()).setRowCount(0);
						return;
					}

					PathOptimiser searchScheme = (getSelectedValue());

					if (searchScheme == null)
						return;

					PathOptimiser.setSelectedPathOptimiser(searchScheme);

					linearList.setEnabled(true);

					for (SearchTask t : TaskManager.getTaskList())
						t.checkProblems();

				}
			});

		}
	}

	class LinearSearchList extends JList<LinearOptimiser> {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5478023007473400159L;

		public LinearSearchList() {

			super();

			setFont(font);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setModel(new AbstractListModel<LinearOptimiser>() {
				/**
				 * 
				 */
				private static final long serialVersionUID = -3560305247730025830L;

				@Override
				public int getSize() {
					return linearSolvers.size();
				}

				@Override
				public LinearOptimiser getElementAt(int index) {
					return linearSolvers.get(index);
				}
			});
			
			this.setCellRenderer(new SearchListRenderer());

			addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent arg0) {

					if (arg0.getValueIsAdjusting())
						return;

					if (!(getSelectedValue() instanceof LinearOptimiser)) {
						pathTable.setEnabled(false);
						return;
					}

					LinearOptimiser linearSolver = (getSelectedValue());

					var pathSolver = PathOptimiser.getSelectedPathOptimiser();
					pathSolver.setLinearSolver(linearSolver);

					pathTable.setPropertyHolder(pathSolver);
					pathTable.setEnabled(true);

					for (SearchTask t : TaskManager.getTaskList())
						t.checkProblems();

				}
			});

		}

	}

}