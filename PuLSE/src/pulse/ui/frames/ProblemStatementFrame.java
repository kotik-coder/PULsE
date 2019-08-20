package pulse.ui.frames;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.statements.Problem;
import pulse.tasks.SearchTask;
import pulse.tasks.Status;
import pulse.tasks.TaskManager;
import pulse.tasks.Status.Details;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.listeners.TaskSelectionListener;
import pulse.ui.components.LoaderButton;
import pulse.ui.components.PropertyHolderTable;
import pulse.ui.components.TaskSelectionToolBar;
import pulse.ui.components.ToolBarButton;
import pulse.ui.components.WrapCellRenderer;
import pulse.ui.components.LoaderButton.DataType;
import pulse.util.Reflexive;

public class ProblemStatementFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 826803930933344406L;
	private JPanel contentPane;
	private PropertyHolderTable problemTable, schemeTable;
	private	SchemeSelectionList schemeSelectionList;
	private ProblemList problemList;
	private TaskSelectionToolBar taskToolBar;
	
	private final static int WIDTH	= 1000;
	private final static int HEIGHT = 600;
	
	private final static int LIST_FONT_SIZE = 16;
	private final static Font LIST_FONT = new Font(Messages.getString("ProblemStatementFrame.LIST_FONT"), Font.PLAIN, LIST_FONT_SIZE);
	
	/**
	 * Create the frame.
	 */
	public ProblemStatementFrame() {
		setType(Type.UTILITY);
		setAlwaysOnTop(true);
		setResizable(false);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setTitle(Messages.getString("ProblemStatementFrame.Title")); //$NON-NLS-1$
		setBounds(100, 100, WIDTH, HEIGHT);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JSplitPane splitPane = new JSplitPane();
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerSize(10);
		splitPane.setResizeWeight(0.5);
		contentPane.add(splitPane);

		JPanel pane1 = new JPanel();
		splitPane.setLeftComponent(pane1);
		pane1.setLayout(new GridLayout(0, 1, 0, 0));

		problemList = new ProblemList();		
		problemList.setCellRenderer(new WrapCellRenderer(this.getWidth()/2 - 150));
		
		JScrollPane problemScroller = new JScrollPane(problemList);
		pane1.add(problemScroller);

		JScrollPane problemDetailsScroller = new JScrollPane();
		pane1.add(problemDetailsScroller);
		
		problemTable = new PropertyHolderTable(null) {
			
			/**
			 * 
			 */
			private static final long serialVersionUID = -6578898159810827328L;

			@Override
			public void updateTable() {
				super.updateTable();
				( (TableRowSorter<?>)getRowSorter() ).sort();
			}
			
		};			
		
		problemDetailsScroller.setViewportView(problemTable);
		
		JPanel pane2 = new JPanel();
		splitPane.setRightComponent(pane2);
		pane2.setLayout(new GridLayout(0, 1, 0, 0));

		JScrollPane schemeSroller = new JScrollPane();
		schemeSroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		pane2.add(schemeSroller);
		schemeSelectionList = new SchemeSelectionList();
		schemeSelectionList.setToolTipText(Messages.getString("ProblemStatementFrame.PleaseSelect")); //$NON-NLS-1$
		schemeSelectionList.setCellRenderer(new WrapCellRenderer(this.getWidth()/2 - 150));
		schemeSroller.setViewportView(schemeSelectionList);
		
		JScrollPane schemeDetailsScroller = new JScrollPane();
		pane2.add(schemeDetailsScroller);

		schemeTable = new PropertyHolderTable(null); //TODO
		schemeDetailsScroller.setViewportView(schemeTable);

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setLayout(new GridLayout());
		contentPane.add(toolBar, BorderLayout.SOUTH);

		JButton btnSimulate = new ToolBarButton(Messages.getString("ProblemStatementFrame.SimulateButton")); //$NON-NLS-1$
		
		//simulate btn listener
		
		btnSimulate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					
				SearchTask t = TaskManager.getSelectedTask();
				
				if(t.checkProblems() == Status.INCOMPLETE) {
					Details d = t.getStatus().getDetails();
					if(	  d == Details.MISSING_PROBLEM_STATEMENT 
					   || d == Details.MISSING_DIFFERENCE_SCHEME ||
						  d == Details.INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT) {
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(
							SwingUtilities.getWindowAncestor((Component) arg0.getSource()),
							t.getStatus().getMessage(),
							Messages.getString("ProblemStatementFrame.ErrorTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					return;
					}
				}						

				t.adjustScheme();
				t.solveProblem();
		
				TaskControlFrame.plot(t, true);				

			}
		});
	
		toolBar.add(btnSimulate);

		LoaderButton btnLoadCv = new LoaderButton(Messages.getString("ProblemStatementFrame.LoadSpecificHeatButton")); //$NON-NLS-1$
		btnLoadCv.setDataType(DataType.SPECIFIC_HEAT);
		toolBar.add(btnLoadCv);

		LoaderButton btnLoadDensity = new LoaderButton(Messages.getString("ProblemStatementFrame.LoadDensityButton")); //$NON-NLS-1$
		btnLoadDensity.setDataType(DataType.DENSITY);
		toolBar.add(btnLoadDensity);
		
		taskToolBar = new TaskSelectionToolBar(problemTable, schemeTable);
		contentPane.add(taskToolBar, BorderLayout.NORTH);
		
		/*
		 * Window events
		 */

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent arg0) {							
				
				if(TaskManager.numberOfTasks() < 1) {
					JOptionPane.showMessageDialog(arg0.getWindow(),
							"Please create a task first!", "Task List Empty", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
					arg0.getWindow().setVisible(false);
					return;
				}
							
				update(TaskManager.getSelectedTask());	
				
				}
			}
		);
		
		TaskManager.addSelectionListener(new TaskSelectionListener() {

			@Override
			public void onSelectionChanged(TaskSelectionEvent e) {
				update(e.getSelection());
			}
			
		});		
		
	}	
	
	public void update() {
		update(TaskManager.getSelectedTask());
	}
	
	private void update(SearchTask selectedTask) {
		
		Problem selectedProblem			= selectedTask == null ? null : selectedTask.getProblem();		   	
		DifferenceScheme selectedScheme	= selectedTask == null ? null : selectedTask.getScheme();		

		//problem
		
		if(selectedProblem == null) 
	    	problemList.clearSelection();
		else {
			setSelectedElement(problemList, selectedProblem);
			problemTable.setPropertyHolder(selectedProblem);
			
		}
		
		//scheme
			
		if(selectedScheme == null) 
			schemeSelectionList.clearSelection();
		else {
			setSelectedElement(schemeSelectionList, selectedScheme);
			schemeTable.setPropertyHolder(selectedScheme);
		}
		
	}
	
	private void changeProblem(SearchTask task, Problem newProblem) {
		Problem oldProblem = task.getProblem(); //stores previous information
		
		Class<? extends Problem> problemClass 				  = newProblem.getClass();
		Constructor<? extends Problem> problemCopyConstructor = null;
		
		try {
			if(newProblem != null) { 
				problemCopyConstructor = newProblem.getClass().getConstructor(Problem.class);	
			}
		} catch (NoSuchMethodException | SecurityException e) {
			System.err.println(Messages.getString("ProblemStatementFrame.ConstructorAccessError") + problemClass); //$NON-NLS-1$
			e.printStackTrace();
		}
		
		Problem np = null;
		
		try {
			if(problemCopyConstructor != null && oldProblem != null)
				np = problemCopyConstructor.newInstance(oldProblem);
			else 
				np = newProblem.getClass().getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			System.err.println(Messages.getString("ProblemStatementFrame.InvocationError") + problemCopyConstructor); //$NON-NLS-1$
			e.printStackTrace();
		}
		
		task.setProblem(np);	 //copies information from old problem to new problem type
		
		oldProblem 			   = null;		 
		problemCopyConstructor = null;
		problemClass		   = null;

		task.checkProblems();
		
	}
	
	private void changeScheme(SearchTask task, DifferenceScheme newScheme) {
		
		//TODO
		    
		if(task.getScheme() == null) {
			task.setScheme(DifferenceScheme.copy(newScheme));
			task.getScheme().setTimeLimit( task.getTimeLimit() );
		} 
		
		else {
			
		DifferenceScheme oldScheme = DifferenceScheme.copy(task.getScheme()); //stores previous information
		task.setScheme(null);
		task.setScheme(DifferenceScheme.copy(newScheme));					  //assigns new problem type
		
		if(		newScheme.getClass().getSimpleName().equals(
				oldScheme.getClass().getSimpleName()) 
		  )
			task.getScheme().copyEverythingFrom(oldScheme);						  //copies information from old problem to new problem type
		else
			task.getScheme().setTimeLimit( task.getTimeLimit() );
		
		oldScheme = null;													  //deletes reference to old problem
		
		}
		
		task.checkProblems();
		
	}	

	private void setSelectedElement(JList<?> list, Object o) {
		if(o == null) {
			list.clearSelection();
			return;
		}
			
		int size = list.getModel().getSize();
		Object fromList = null;
		boolean found = false;
		
		for(int i = 0; i < size; i++) {
			fromList = list.getModel().getElementAt(i);
			if( fromList.toString().equals(o.toString()) ) {
				list.setSelectedIndex(i);
				found = true;
			}
		}
		
		if(!found)
			list.clearSelection();
		
	}
	
	/*
	 * ##################
	 * Problem List Class
	 * ##################
	 */
	
	class ProblemList extends JList<Problem> {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 4769009831926832789L;

		public ProblemList() {
			super();
			setFont(LIST_FONT); //$NON-NLS-1$
			
			List<Problem> knownProblems = Reflexive.instancesOf(Problem.class);
			
			DefaultListModel<Problem> listModel = new DefaultListModel<Problem>();
			for(Problem p : knownProblems)
				listModel.addElement(p);
			setModel(listModel);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent arg0) {
					
					if(arg0.getValueIsAdjusting())
						return;
					
					Problem newlySelectedProblem  	= (Problem) getSelectedValue();
					
					if(newlySelectedProblem == null) {
						((DefaultTableModel) problemTable.getModel()).setRowCount(0);
						return;
					}
					
					SearchTask selectedTask	= TaskManager.getSelectedTask();
					
					if( Problem.isSingleStatement() )  
						for(SearchTask t : TaskManager.getTaskList()) 
							changeProblem(t, newlySelectedProblem);					
					else 
						changeProblem(selectedTask, newlySelectedProblem);
					
					listModel.set(
							listModel.indexOf(newlySelectedProblem), 
							selectedTask.getProblem());	
					
					problemTable.setPropertyHolder( TaskManager.getSelectedTask().getProblem() );
					
					//after problem is selected for this task, show available difference schemes
					
					DefaultListModel<DifferenceScheme> defaultModel = (DefaultListModel<DifferenceScheme>)(schemeSelectionList.getModel());
					defaultModel.clear();
					
					DifferenceScheme[] schemes = newlySelectedProblem.availableSolutions();
					
					for(DifferenceScheme s : schemes)
						defaultModel.addElement(s);
					
					schemeSelectionList.setToolTipText(null);
										
				}
			});
			
			TaskManager.addSelectionListener(new TaskSelectionListener() {

				@Override
				public void onSelectionChanged(TaskSelectionEvent e) {
					//select appropriate problem type from list				
					
					if(e.getSelection().getProblem() != null) {
					    for(int i = 0; i < getModel().getSize(); i++) { 
					    		Problem p = (Problem) getModel().getElementAt(i);
					    		if(e.getSelection().getProblem().getClass().equals(p.getClass())) {
					    			setSelectedIndex(i);
					    			break;
					    		}			    						    			
					    }
					}
				    
				    //then, select appropriate scheme type
				    
					if(e.getSelection().getScheme() != null) {
					    for(int i = 0; i < schemeSelectionList.getModel().getSize(); i++) {
					    	if(e.getSelection().getScheme().getClass().equals(schemeSelectionList.getModel().getElementAt(i).getClass())) {
					    		schemeSelectionList.setSelectedIndex(i);
					    		break;
					    	}
					    }		
					}
				    
				}
				
			});		
			
		}
		
	}
	
	/* ###########################
	 * Scheme selection list class
	 * ###########################
	 */
	
	class SchemeSelectionList extends JList<DifferenceScheme> {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 7976007297729697556L;

		public SchemeSelectionList() {
			
			super();
			setFont(LIST_FONT);		 //$NON-NLS-1$
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			DefaultListModel<DifferenceScheme> m = new DefaultListModel<DifferenceScheme>();
			setModel(m);
			//scheme list listener
			
			addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent arg0) {
					
					if(arg0.getValueIsAdjusting())
						return;
					
					if (!(getSelectedValue() instanceof DifferenceScheme)) {
						((DefaultTableModel) schemeTable.getModel()).setRowCount(0);
						return;
					}
					
					SearchTask selectedTask = TaskManager.getSelectedTask();	
					DifferenceScheme newScheme = (DifferenceScheme) (getSelectedValue());
					
					if(newScheme == null)
						return;
					
					if( Problem.isSingleStatement() ) 
						for(SearchTask t : TaskManager.getTaskList())
							changeScheme(t, newScheme);
					else
							changeScheme(selectedTask, newScheme);
					
					schemeTable.setPropertyHolder(selectedTask.getScheme());
					
				}
			});
			
		}
		
	}
	
}