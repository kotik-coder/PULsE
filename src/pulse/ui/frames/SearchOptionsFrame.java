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

import pulse.search.direction.PathSolver;
import pulse.search.linear.LinearSolver;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.ui.Messages;
import pulse.ui.components.PropertyHolderTable;
import pulse.util.Reflexive;

@SuppressWarnings("serial")
public class SearchOptionsFrame extends JInternalFrame {

	private PropertyHolderTable pathTable;
	private JList<LinearSolver> linearList;
	private PathSolversList pathList;

	private final static Font font = new Font(Messages.getString("PropertyHolderTable.FontName"), Font.ITALIC, 16);
	
	private final static List<PathSolver> pathSolvers		= Reflexive.instancesOf(PathSolver.class);
	private final static List<LinearSolver> linearSolvers	= Reflexive.instancesOf(LinearSolver.class);
	
	
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
		panel.setBorder(new EmptyBorder(5,5,5,5));
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
		if(TaskManager.getPathSolver() != null) {
			pathList.setSelectedIndex(pathSolvers.indexOf(TaskManager.getPathSolver()));
			linearList.setSelectedIndex(linearSolvers.indexOf(PathSolver.getLinearSolver()));	
		}
		else {
			pathList.clearSelection();
			linearList.clearSelection();
			linearList.setEnabled(false);
		}
	}

	class PathSolversList extends JList<PathSolver> {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 3662972578473909850L;

		public PathSolversList() {
		
			super();
			
			setModel(new AbstractListModel<PathSolver>() {
				/**
				 * 
				 */
				private static final long serialVersionUID = -7683200230096704268L;

				@Override
				public int getSize() {
					return pathSolvers.size();
				}
				@Override
				public PathSolver getElementAt(int index) {
					return pathSolvers.get(index);
				}
			});		
			
			setFont(font);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent arg0) {
					
					if(arg0.getValueIsAdjusting())
						return;
					
					if (!(getSelectedValue() instanceof PathSolver)) {
						((DefaultTableModel) pathTable.getModel()).setRowCount(0);
						return;
					}
					
					PathSolver searchScheme = (getSelectedValue());
					
					if(searchScheme == null)
						return;
					
					TaskManager.setPathSolver(searchScheme);
			
					linearList.setEnabled(true);
					
					for(SearchTask t : TaskManager.getTaskList())
						t.checkProblems();
									
				}
			});						
			
		}
	}
	
	class LinearSearchList extends JList<LinearSolver> {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 5478023007473400159L;

		public LinearSearchList() {
			
			super();
			
			setFont(font);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setModel(new AbstractListModel<LinearSolver>() {
				/**
				 * 
				 */
				private static final long serialVersionUID = -3560305247730025830L;
				@Override
				public int getSize() {
					return linearSolvers.size();
				}
				@Override
				public LinearSolver getElementAt(int index) {
					return linearSolvers.get(index);
				}
			});
			
			addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent arg0) {
					
					if(arg0.getValueIsAdjusting())
						return;
					
					if (!(getSelectedValue() instanceof LinearSolver)) {
							pathTable.setEnabled(false);
						return;
					}
					
					LinearSolver linearSolver = (getSelectedValue());
					 
					TaskManager.getPathSolver().setLinearSolver(linearSolver);
					
					pathTable.setPropertyHolder(TaskManager.getPathSolver());
					pathTable.setEnabled(true);
					
					for(SearchTask t : TaskManager.getTaskList())
						t.checkProblems();
					
				}
			});
			
		}
		
	}
	
}
