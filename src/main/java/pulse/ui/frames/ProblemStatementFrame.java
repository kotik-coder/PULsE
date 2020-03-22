package pulse.ui.frames;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import pulse.input.InterpolationDataset;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.Solver;
import pulse.problem.statements.Problem;
import pulse.tasks.SearchTask;
import pulse.tasks.Status;
import pulse.tasks.Status.Details;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.listeners.TaskSelectionListener;
import pulse.ui.Messages;
import pulse.ui.components.Chart;
import pulse.ui.components.PropertyHolderTable;
import pulse.ui.components.buttons.LoaderButton;
import pulse.ui.components.panels.SettingsToolBar;
import pulse.util.Reflexive;

@SuppressWarnings("serial")
public class ProblemStatementFrame extends JInternalFrame {

	private PropertyHolderTable problemTable, schemeTable;
	private	SchemeSelectionList schemeSelectionList;
	private ProblemList problemList;

	private final static int LIST_FONT_SIZE = 12;
	
	private final static List<Problem> knownProblems = Reflexive.instancesOf(Problem.class);
	
	
	/**
	 * Create the frame.
	 */
	public ProblemStatementFrame() {
		setResizable(false);
		setClosable(true);
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		
		setTitle(Messages.getString("ProblemStatementFrame.Title")); //$NON-NLS-1$
		
		setBounds(100, 100, WIDTH, HEIGHT);
		
		getContentPane().setLayout(new BorderLayout());
		
		/*
		 * Create a 2x2 grid for lists and tables
		 */
		
		JPanel contentPane = new JPanel();		
		GridLayout layout = new GridLayout(2,2);
		layout.setHgap(5);
		layout.setVgap(5);
		contentPane.setLayout(layout);
		
		/*
		 * Problem selection list and scroller
		 */

		problemList = new ProblemList();
		
		problemList.setSelectionModel(new DefaultListSelectionModel() {
			
			@Override
	        public void setSelectionInterval(int index0, int index1) {
				if(index0 != index1)
					return;
				
				boolean enabledFlag = knownProblems.get(index0).isEnabled();
				
	            if (enabledFlag)
	            	super.setSelectionInterval(index0, index0);
	            else 
	            	return;
	            
	        }
	    
		});
		
		contentPane.add(new JScrollPane(problemList));
		
		/*
		 * Scheme list and scroller
		 */
		
		schemeSelectionList = new SchemeSelectionList();
		schemeSelectionList.setToolTipText(Messages.getString("ProblemStatementFrame.PleaseSelect")); //$NON-NLS-1$		
		
		JScrollPane schemeScroller = new JScrollPane(schemeSelectionList);
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
				( (TableRowSorter<?>)getRowSorter() ).sort();
			}
			
		};			
		
		JScrollPane problemDetailsScroller = new JScrollPane(problemTable);											
		contentPane.add(problemDetailsScroller);
		
		/*
		 * Scheme details table and scroller
		 */		

		schemeTable = new PropertyHolderTable(null); //TODO
		JScrollPane schemeDetailsScroller = new JScrollPane(schemeTable);
		contentPane.add(schemeDetailsScroller);
		
		/*
		 * Toolbar
		 */

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setLayout(new GridLayout());		

		JButton btnSimulate = new JButton(Messages.getString("ProblemStatementFrame.SimulateButton")); //$NON-NLS-1$
		
		//simulate btn listener
		
		btnSimulate.addActionListener(new ActionListener() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
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

				( (Solver)t.getScheme() ).solve(t.getProblem());				
		
				Chart.plot(t, true);				

			}
		});
	
		toolBar.add(btnSimulate);

		LoaderButton btnLoadCv = new LoaderButton(Messages.getString("ProblemStatementFrame.LoadSpecificHeatButton")); //$NON-NLS-1$
		btnLoadCv.setDataType(InterpolationDataset.StandartType.SPECIFIC_HEAT);
		toolBar.add(btnLoadCv);

		LoaderButton btnLoadDensity = new LoaderButton(Messages.getString("ProblemStatementFrame.LoadDensityButton")); //$NON-NLS-1$
		btnLoadDensity.setDataType(InterpolationDataset.StandartType.DENSITY);
		toolBar.add(btnLoadDensity);
		
		/*
		 * 
		 */
		
		getContentPane().add(new SettingsToolBar(problemTable, schemeTable), BorderLayout.NORTH);		
		getContentPane().add(contentPane, BorderLayout.CENTER);
		getContentPane().add(toolBar, BorderLayout.SOUTH);
		
		/*
		 * listeners
		 */
		
		TaskManager.addSelectionListener(new TaskSelectionListener() {

			@Override
			public void onSelectionChanged(TaskSelectionEvent e) {
				update(e.getSelection());
			}
			
		});	
		//TODO
		
		TaskManager.getInstance().addHierarchyListener( event -> 
		{ 		
			if(!(event.getSource() instanceof PropertyHolderTable))
				return;
			
			if(!Problem.isSingleStatement())
				return;
			
			Problem p;
			
			for(SearchTask task : TaskManager.getTaskList()) { 				
				p = task.getProblem();
				if(p != null) p.updateProperty(event, event.getProperty());							
			}
					
		}
		);			
		
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
			task.setScheme(newScheme.copy());
			//task.getScheme().setTimeLimit( task.getTimeLimit() );
		} 
		
		else {
			
		DifferenceScheme oldScheme = task.getScheme().copy(); //stores previous information
		task.setScheme(null);
		task.setScheme(newScheme.copy());					  //assigns new problem type
		
		if(		newScheme.getClass().getSimpleName().equals(
				oldScheme.getClass().getSimpleName()) 
		  )
			task.getScheme().copyFrom(oldScheme);						  //copies information from old problem to new problem type
		//else
			//task.getScheme().setTimeLimit( task.getTimeLimit() );
		
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
		
		public ProblemList() {
			super();
			setFont(getFont().deriveFont(LIST_FONT_SIZE));
			
			DefaultListModel<Problem> listModel = new DefaultListModel<Problem>();
			
			for(Problem p : knownProblems)
				listModel.addElement(p);
			
			setModel(listModel);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent arg0) {
					
					if(arg0.getValueIsAdjusting())
						return;
					
					Problem newlySelectedProblem  	= getSelectedValue();
					
					if(newlySelectedProblem == null) {
						((DefaultTableModel) problemTable.getModel()).setRowCount(0);
						return;
					}
					
					if(TaskManager.getSelectedTask() == null)
						TaskManager.selectFirstTask();
					
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
					
					List<DifferenceScheme> schemes = newlySelectedProblem.availableSolutions();
					
					schemes.forEach(s -> defaultModel.addElement(s));
					
					schemeSelectionList.setToolTipText(null);
										
				}
			});
			
			TaskManager.addSelectionListener(new TaskSelectionListener() {

				@Override
				public void onSelectionChanged(TaskSelectionEvent e) {
					//select appropriate problem type from list				
					
					if(e.getSelection().getProblem() != null) {
					    for(int i = 0; i < getModel().getSize(); i++) { 
					    		Problem p = getModel().getElementAt(i);
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
		
		public SchemeSelectionList() {
			
			super();
			setFont(getFont().deriveFont(LIST_FONT_SIZE));
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			DefaultListModel<DifferenceScheme> m = new DefaultListModel<DifferenceScheme>();
			setModel(m);
			//scheme list listener
			
			addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent arg0) {
					
					if(arg0.getValueIsAdjusting())
						return;
					
					if (!(getSelectedValue() instanceof DifferenceScheme)) {
						((DefaultTableModel) schemeTable.getModel()).setRowCount(0);
						return;
					}
					
					SearchTask selectedTask = TaskManager.getSelectedTask();	
					DifferenceScheme newScheme = (getSelectedValue());
					
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