package pulse.ui.frames;

import static java.awt.Font.ITALIC;
import static java.awt.GridBagConstraints.BOTH;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static pulse.search.direction.PathOptimiser.getInstance;
import static pulse.search.direction.PathOptimiser.setInstance;
import static pulse.ui.Messages.getString;
import static pulse.util.Reflexive.instancesOf;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;

import pulse.search.direction.PathOptimiser;
import pulse.tasks.TaskManager;
import pulse.ui.components.PropertyHolderTable;
import pulse.ui.components.controllers.SearchListRenderer;

@SuppressWarnings("serial")
public class SearchOptionsFrame extends JInternalFrame {

	private PropertyHolderTable pathTable;
	private PathSolversList pathList;

	private final static Font font = new Font(getString("PropertyHolderTable.FontName"), ITALIC, 16);
	private final static List<PathOptimiser> pathSolvers = instancesOf(PathOptimiser.class);

	/**
	 * Create the frame.
	 */
	public SearchOptionsFrame() {
		setClosable(true);
		setTitle(getString("SearchOptionsFrame.SelectSearch")); //$NON-NLS-1$
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setBounds(100, 100, WIDTH, HEIGHT);

		/*
		 * Path solver list and scroller
		 */

		var panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(panel);

		pathList = new PathSolversList();
		var pathListScroller = new JScrollPane(pathList);
		pathListScroller.setBorder(createTitledBorder("Select an Optimiser"));

		pathTable = new PropertyHolderTable(null);

		getContentPane().setLayout(new GridBagLayout());

		var gbc = new GridBagConstraints();

		gbc.fill = BOTH;
		gbc.gridy = 0;
		gbc.gridx = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 0.2;

		getContentPane().add(pathListScroller, gbc);

		gbc.gridy = 1;
		gbc.weighty = 0.6;

		var tableScroller = new JScrollPane(pathTable);
		tableScroller.setBorder(createTitledBorder("Select search variables and settings"));
		getContentPane().add(tableScroller, gbc);

	}

	public void update() {
		var selected = getInstance();
		if (selected != null) {
			pathList.setSelectedIndex(pathSolvers.indexOf(selected));
			pathTable.updateTable();
		} else {
			pathList.clearSelection();
		}
	}

	class PathSolversList extends JList<PathOptimiser> {

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
			setSelectionMode(SINGLE_SELECTION);
			setCellRenderer(new SearchListRenderer());

			addListSelectionListener((ListSelectionEvent arg0) -> {
				if (arg0.getValueIsAdjusting())
					return;
				
				var optimiser = getSelectedValue();
				
				setInstance(optimiser);
				pathTable.setPropertyHolder(optimiser);
				
				for (var t : TaskManager.getManagerInstance().getTaskList()) {
					t.checkProblems(true);
				}
			});

		}
	}

}