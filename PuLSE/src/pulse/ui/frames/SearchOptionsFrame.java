package pulse.ui.frames;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import pulse.search.direction.ApproximatedHessianSolver;
import pulse.search.direction.PathSolver;
import pulse.search.direction.SteepestDescentSolver;
import pulse.search.linear.GoldenSectionSolver;
import pulse.search.linear.LinearSolver;
import pulse.search.linear.WolfeSolver;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.ui.components.PropertyHolderTable;
import pulse.ui.components.WrapCellRenderer;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SearchOptionsFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6832552989196599722L;
	private JPanel contentPane;
	private PropertyHolderTable pathTable;
	private JList<LinearSolver> linearList;

	private final static int WIDTH = 750;
	private final static int HEIGHT = 550;
	private final static Font LIST_FONT = new Font(Messages.getString("SearchOptionsFrame.ListFont"), Font.PLAIN, 18);
	
	/**
	 * Create the frame.
	 */
	public SearchOptionsFrame() {
		setAlwaysOnTop(true);
		setResizable(false);
		setTitle(Messages.getString("SearchOptionsFrame.SelectSearch")); //$NON-NLS-1$
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, WIDTH, HEIGHT);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new GridLayout(0, 2, 0, 0));
		
		JPanel panel = new JPanel();
		contentPane.add(panel);
		panel.setLayout(new GridLayout(2, 1, 0, 0));
		

		PathSolversList pathList = new PathSolversList();
		
		ListCellRenderer renderer = new WrapCellRenderer(this.getWidth()/2 - 150);
		
		pathList.setCellRenderer(renderer);
		panel.add(new JScrollPane(pathList));
		
		linearList = new LinearSearchList();
		
		linearList.setCellRenderer(renderer);
		linearList.setEnabled(false);
		panel.add(new JScrollPane(linearList));
		
		JPanel panel_1 = new JPanel();
		contentPane.add(panel_1);
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));
		
		JScrollPane scrollPane_2 = new JScrollPane();
		panel_1.add(scrollPane_2);
		
		pathTable = new PropertyHolderTable(null);
		
		scrollPane_2.setViewportView(pathTable);
		
		/*
		 * Window Events
		 */
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent arg0) {
				if(TaskManager.numberOfTasks() < 1) {
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) arg0.getSource()),
							Messages.getString("SearchOptionsFrame.PleaseCreateTask"), //$NON-NLS-1$
							"No tasks", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					setVisible(false);
					return;
				}			
				
			}
			
			
		});
		
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
				PathSolver[] values = new PathSolver[] 
						{SteepestDescentSolver.getInstance(),
						 ApproximatedHessianSolver.getInstance()};
				public int getSize() {
					return values.length;
				}
				public PathSolver getElementAt(int index) {
					return values[index];
				}
			});		
			
			setFont(LIST_FONT); //$NON-NLS-1$
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent arg0) {
					
					if(arg0.getValueIsAdjusting())
						return;
					
					if (!(getSelectedValue() instanceof PathSolver)) {
						((DefaultTableModel) pathTable.getModel()).setRowCount(0);
						return;
					}
					
					PathSolver searchScheme = (PathSolver) (getSelectedValue());
					
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
			
			setFont(LIST_FONT); //$NON-NLS-1$
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setModel(new AbstractListModel<LinearSolver>() {
				/**
				 * 
				 */
				private static final long serialVersionUID = -3560305247730025830L;
				LinearSolver[] values = new LinearSolver[] {
						GoldenSectionSolver.getInstance(),
						WolfeSolver.getInstance()
						};
				public int getSize() {
					return values.length;
				}
				public LinearSolver getElementAt(int index) {
					return values[index];
				}
			});
			
			addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent arg0) {
					
					if(arg0.getValueIsAdjusting())
						return;
					
					if (!(getSelectedValue() instanceof LinearSolver)) {
							pathTable.setEnabled(false);
						return;
					}
					
					LinearSolver linearSolver = (LinearSolver) (getSelectedValue());
					 
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
