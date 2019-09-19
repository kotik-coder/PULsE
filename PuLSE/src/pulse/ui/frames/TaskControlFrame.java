package pulse.ui.frames;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GridLayout;
import java.awt.Toolkit;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.input.Metadata;
import pulse.io.readers.MetaFileReader;
import pulse.io.readers.ReaderManager;
import pulse.problem.statements.Problem;
import pulse.tasks.Identifier;
import pulse.tasks.Log;
import pulse.tasks.LogEntry;
import pulse.tasks.ResultFormat;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.LogEntryListener;
import pulse.tasks.listeners.ResultFormatEvent;
import pulse.tasks.listeners.ResultFormatListener;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskRepositoryListener;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.listeners.TaskSelectionListener;
import pulse.ui.Launcher;
import pulse.ui.Messages;
import pulse.ui.charts.Chart;
import pulse.ui.components.ButtonTabComponent;
import pulse.ui.components.ExecutionButton;
import pulse.ui.components.LogPane;
import pulse.ui.components.LogToolBar;
import pulse.ui.components.PlotType;
import pulse.ui.components.ResultTable;
import pulse.ui.components.ResultTableModel;
import pulse.ui.components.ResultsToolBar;
import pulse.ui.components.TaskTable;
import pulse.ui.components.TaskTable.TaskTableModel;
import pulse.ui.components.ToolBarButton;
import pulse.util.SaveableDirectory;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import javax.swing.JFileChooser;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.awt.event.ActionEvent;
import java.awt.Color;
import javax.swing.UIManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.Font;

import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.ChangeEvent;

public class TaskControlFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1109525291742210857L;
	
	private static JTabbedPane tabbedPane;
	
	private static LogPane logText;
	
	private static TaskToolBar taskToolBar;	
	private static ResultTable resultsTable;

	private static TaskTable taskTable;
	
	private final static int RESULTS_HEADER_HEIGHT = 70;		
	
	private final static int WIDTH = 1200;
	private final static int HEIGHT = 700;
	
	private static TaskControlFrame instance = new TaskControlFrame();
	
	private final static float TABBED_PANE_FONT_SIZE = 18;
	private final static float SYSTEM_INFO_FONT_SIZE = 14;
	
	private static File dir;
	
	/**
	 * Create the frame.
	 */
	
	private TaskControlFrame() {
		BufferedImage logo = null;
		try {
			logo = ImageIO.read(getClass().getResourceAsStream(Messages.getString("TaskControlFrame.LogoImagePath"))); //$NON-NLS-1$
		} catch (IOException e1) {
			System.err.println("Failed to load logo image"); //$NON-NLS-1$
			e1.printStackTrace();
		}
		
		setIconImage(logo);
		
		setTitle(Messages.getString("TaskControlFrame.SoftwareTitle")); //$NON-NLS-1$
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(WIDTH, HEIGHT));
				
		getContentPane().setLayout(new BorderLayout());
		
		setJMenuBar( new MainMenu() );										//create main menu
		
		tabbedPane = createTabbedPane();									//create tabbed pane
		tabbedPane.setFont(tabbedPane.getFont().deriveFont(TABBED_PANE_FONT_SIZE));
		
		getContentPane().add( tabbedPane, BorderLayout.CENTER );
		
		generateSystemPanel();
		
		JPanel taskManagerPane = new JPanel();		
		taskManagerPane.setLayout(new BorderLayout());
		
		taskTable = new TaskTable(); 	
		JScrollPane taskScroller = new JScrollPane(taskTable);
		taskManagerPane.add(taskScroller, BorderLayout.CENTER);
		
		taskToolBar = new TaskToolBar();
		
		taskManagerPane.add(taskToolBar, BorderLayout.NORTH);	//add task toolbar
			
		JSplitPane outputPane = new JSplitPane();
		
		outputPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1)); 
	    outputPane.setBorder(null);
		
		outputPane.setOneTouchExpandable(true);
		outputPane.setResizeWeight(0.6);
		outputPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		
		resultsTable = new ResultTable(ResultFormat.DEFAULT_FORMAT);
		
		adjustEnabledControls();
		
		JScrollPane resultsScroller = new JScrollPane(resultsTable);
		
		Dimension headersSize = resultsTable.getTableHeader().getPreferredSize();
		headersSize.height = RESULTS_HEADER_HEIGHT;
		resultsTable.getTableHeader().setPreferredSize(headersSize);		
		
		ResultsToolBar resultsToolBar	= new ResultsToolBar(resultsTable, this);

		JPanel topPane		= new JPanel();
		topPane.setLayout(new BorderLayout(0, 0));
		topPane.add(resultsScroller);
		topPane.add(resultsToolBar, BorderLayout.SOUTH);
				
		outputPane.setTopComponent(topPane); 					//Sets top component
		
		JPanel bottomPane		= new JPanel();	
		
		bottomPane.setLayout(new BorderLayout(0, 0));
		
		logText 				= new LogPane();
		scheduleLogEvents();		
		
		JScrollPane scrollPane	= new JScrollPane(logText);
		bottomPane.add(scrollPane);
		bottomPane.add(new LogToolBar(this, logText), BorderLayout.SOUTH);
		
		outputPane.setBottomComponent(bottomPane);			//Sets bottom component
		
		JSplitPane splitPane = new JSplitPane();
		
		splitPane.setResizeWeight(0.5);
		splitPane.setOneTouchExpandable(true);

		splitPane.setLeftComponent(taskManagerPane);	//Sets left component
		splitPane.setRightComponent(outputPane);			//Sets right component
		
		addStandardTab(StandardTabName.TaskManager, splitPane, false);		

		splitPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1)); 
		splitPane.setBorder(null);		
		
		scheduleCloseEvent();
				
	}
	
	public void scheduleCloseEvent() {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {				
				Object[] options = {"Yes", "No"}; //$NON-NLS-1$ //$NON-NLS-2$
				Toolkit.getDefaultToolkit().beep();
				int answer = JOptionPane.showOptionDialog(
						(Component)arg0.getSource(),
								Messages.getString("TaskControlFrame.ExitMessage"), //$NON-NLS-1$
								Messages.getString("TaskControlFrame.ExitTitle"), //$NON-NLS-1$
								JOptionPane.YES_NO_OPTION,
								JOptionPane.WARNING_MESSAGE,
								null,
								options,
								options[1]);
				if(answer == 1)
					((JFrame)arg0.getSource()).setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				else 
					((JFrame)arg0.getSource()).setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			}
		});
	}
	
	public void scheduleLogEvents() {
		TaskManager.addSelectionListener(new TaskSelectionListener() {

			@Override
			public void onSelectionChanged(TaskSelectionEvent e) {				
				logText.callPrintAll();
			}
			
		});
		
		TaskManager.addTaskRepositoryListener( event -> { 
			if(event.getState() != TaskRepositoryEvent.State.TASK_ADDED)
				return;
						
			SearchTask task = TaskManager.getTask(event.getId());
			
			task.getLog().addListener( new LogEntryListener() {

				@Override
				public void onNewEntry(LogEntry e) {
					if(TaskManager.getSelectedTask() == task) 
						logText.callUpdate();	
				}
				
				@Override
				public void onLogFinished(Log log) {
					if(TaskManager.getSelectedTask() == task) {
						
						try {
							logText.getUpdateExecutor().awaitTermination(10, TimeUnit.MILLISECONDS);
						} catch (InterruptedException e) {
							System.err.println("Log not finished in time");
							e.printStackTrace();
						}
						
						logText.printTimeTaken(log);
						
					}
				}
				
			}
			
			);
			
			}
		);
	}
	
	public void adjustEnabledControls() {
		TaskTableModel ttm = (TaskTableModel) taskTable.getModel();
		
		ttm.addTableModelListener(
				new TableModelListener(){

					@Override
					public void tableChanged(TableModelEvent arg0) {
						if(ttm.getRowCount() < 1) {
							taskToolBar.btnClr.setEnabled(false);
							taskToolBar.btnReset.setEnabled(false);
							taskToolBar.controlButton.setEnabled(false);
						} else {
							taskToolBar.btnClr.setEnabled(true);
							taskToolBar.btnReset.setEnabled(true);
							taskToolBar.controlButton.setEnabled(true);
						}
					}						
					
					
		});
		
		taskTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				int[] selection = taskTable.getSelectedRows();
				if(taskTable.getSelectedRow() < 0) {
					taskToolBar.btnRemove.setEnabled(false);
					taskToolBar.btnShowCurve.setEnabled(false);				
				} else {
					if(selection.length < 2)
						taskToolBar.btnShowCurve.setEnabled(true);
					else
						taskToolBar.btnShowCurve.setEnabled(false);
					taskToolBar.btnRemove.setEnabled(true);
				}
			}
		
		});
	}
	
	public static TaskControlFrame getInstance() {
		return instance;
	}
	
	public void generateSystemPanel() {
		JPanel usagePanel = new JPanel();
		usagePanel.setLayout(new GridLayout(1,3));
		usagePanel.setBorder(BorderFactory.createLineBorder(Color.black, 1));

		String coresAvailable = String.format("Running on " + (Launcher.threadsAvailable()+1) + " cores");
		JLabel coresLabel = new JLabel(coresAvailable);
		coresLabel.setFont(coresLabel.getFont().deriveFont(SYSTEM_INFO_FONT_SIZE));
		
		String cpuString = String.format("CPU usage: %3f%%", Launcher.CPUUsage());
		JLabel cpuUsageLabel = new JLabel(cpuString);
		cpuUsageLabel.setFont(cpuUsageLabel.getFont().deriveFont(SYSTEM_INFO_FONT_SIZE));
		
		String memoryString = String.format("Memory usage: %3.1f%%", Launcher.getMemoryUsage());
		JLabel 		memoryUsageLabel = new JLabel(memoryString);
		memoryUsageLabel.setFont(memoryUsageLabel.getFont().deriveFont(SYSTEM_INFO_FONT_SIZE));
		usagePanel.add(cpuUsageLabel);
		usagePanel.add(memoryUsageLabel);
		usagePanel.add(coresLabel);
		
		getContentPane().add(usagePanel, BorderLayout.SOUTH);		
		
		ScheduledExecutorService executor =
			    Executors.newSingleThreadScheduledExecutor();

			Runnable periodicTask = new Runnable() {
			    public void run() {
			    	double cpuUsage = Launcher.CPUUsage();
			    	double memoryUsage = Launcher.getMemoryUsage();			    
			    	
			        cpuUsageLabel.setText( String.format("CPU usage: %3.1f%%", cpuUsage) );
			        memoryUsageLabel.setText( String.format("Memory usage: %3.1f%%", memoryUsage) );
			        
			        Color borderColor = ( (javax.swing.border.LineBorder) usagePanel.getBorder()).getLineColor();
			        
			        if((cpuUsage > 75) || (memoryUsage > 75)) {
			        	if(! borderColor.equals(Color.red))
			        		usagePanel.setBorder(BorderFactory.createLineBorder(Color.red, 2));
			        } 
			        else if((cpuUsage > 50) || (memoryUsage > 50)) {
			        	if(! borderColor.equals(Color.yellow))
			        		usagePanel.setBorder(BorderFactory.createLineBorder(Color.orange, 2));
			        } else 
			        	usagePanel.setBorder(BorderFactory.createLineBorder(Color.black, 1));
			    }
			};
			
		executor.scheduleAtFixedRate(periodicTask, 0, 2, TimeUnit.SECONDS);
	}
	
	/*
	 * Initiates the dialog to load heating curve experimental data
	 */
	
	public static void askToLoadData() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(dir);
		
		fileChooser.setMultiSelectionEnabled(true);
		
		List<String> extensions = ReaderManager.getCurveExtensions();							
		String[] extArray = extensions.toArray(new String[extensions.size()]);			
		
		fileChooser.setFileFilter(new FileNameExtensionFilter(Messages.getString("TaskControlFrame.ExtensionDescriptor"), extArray)); //$NON-NLS-1$
		
		boolean approve = fileChooser.showOpenDialog(instance) == JFileChooser.APPROVE_OPTION;
		dir = fileChooser.getCurrentDirectory();
		
		if(!approve)
			return;
		
		TaskManager.generateTasks(Arrays.asList(fileChooser.getSelectedFiles()));
		TaskManager.selectFirstTask();									
				
	}

	public static void askToTruncate() {								
		Object[] options = {"Truncate", "Do not change"}; //$NON-NLS-1$ //$NON-NLS-2$
		Toolkit.getDefaultToolkit().beep();
		int answer = JOptionPane.showOptionDialog(
				instance,
						("The acquisition time for some experiments appears to be too long.\nIf time resolution is low, the model estimates will be biased.\n\nIt is recommended to allow PULSE to truncate this data.\n\nWould you like to proceed? "), //$NON-NLS-1$
						"Potential Problem with Data", //$NON-NLS-1$
						JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE,
						null,
						options,
						options[0]);
		if(answer == 0) 
			TaskManager.truncateData();										

	}
	
	/*
	 * Initiates the dialog to load metadata
	 */
	
	public static void askToLoadMetadata() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(dir);
		
		fileChooser.setMultiSelectionEnabled(false);
		
		MetaFileReader reader = MetaFileReader.getInstance();
		
		String extension = reader.getSupportedExtension();										
		
		fileChooser.setFileFilter(new FileNameExtensionFilter(Messages.getString("TaskControlFrame.ExtensionDescriptor"), extension)); //$NON-NLS-1$
		
		boolean approve = fileChooser.showOpenDialog(instance) == JFileChooser.APPROVE_OPTION;
		dir = fileChooser.getCurrentDirectory();
		
		if(!approve)
			return;
		
		try {
			
			Metadata met;
			ExperimentalData data;
			File f = fileChooser.getSelectedFile();
			
			for(SearchTask task : TaskManager.getTaskList()) {
				data = task.getExperimentalCurve();
				
				if(data == null)
					continue;
				
				met = data.getMetadata();
				reader.populateMetadata(f, met);
				
				Problem p = task.getProblem();
				if(p != null)
					p.retrieveData(data);
 				
			}
			
			System.gc();

			
		} catch (IOException e) {
			Toolkit.getDefaultToolkit().beep();
			JOptionPane.showMessageDialog(instance,
				    Messages.getString("TaskControlFrame.LoadError"), //$NON-NLS-1$
				    Messages.getString("TaskControlFrame.IOError"), //$NON-NLS-1$
				    JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		
		if(TaskManager.dataNeedsTruncation())
			askToTruncate();
				
		TaskManager.selectFirstTask();
		
	}

	public static Chart activeChart() {
		final int total = tabbedPane.getTabCount();
		
		for(int i = 0; i < total; i++) {
			Component component = tabbedPane.getComponentAt(i);
			
			if(! (component instanceof Chart) )
				continue;

			if(! (	tabbedPane.getSelectedIndex() == i))
				continue;
			
			return (Chart) component;			
		}
		
		return null;
		
	}

	public static Chart searchForPanel(Identifier taskIdentifier) {
		List<Chart> allPanels = allChartPanels();
		for(Chart p : allPanels)
			if(p.getIdentifier().equals(taskIdentifier))
				return p;

		Chart chart = new Chart();
		chart.setIdentifier(taskIdentifier);
		addChartTab(chart);
		chart.makeChart();
		return chart;		
	}
	
	public static List<Chart> allChartPanels() {
		final int total = tabbedPane.getTabCount();
		List<Chart> chartList = new ArrayList<Chart>();
		
		for(int i = 0; i < total; i++) {
			Component component = tabbedPane.getComponentAt(i);
			if(! (component instanceof Chart) )
				continue;
			chartList.add((Chart)component);			
		}
		
		return chartList;		
	}
	
	public static void addChartTab(Chart tab) {		
		tabbedPane.add(TaskManager.getSelectedTask().toString(), tab);
		tabbedPane.setSelectedComponent(tab);
		int lastAdded = tabbedPane.getTabCount() - 1;
		tabbedPane.setTabComponentAt(lastAdded, new ButtonTabComponent(tabbedPane));
	}
	
	public static void plot(SearchTask task, boolean extendedCurvePlotting) {

		if(task == null) {
			JOptionPane.showMessageDialog(instance,
				    Messages.getString("TaskControlFrame.NoTaskError"), //$NON-NLS-1$
				    Messages.getString("TaskControlFrame.ShowError"), //$NON-NLS-1$
				    JOptionPane.WARNING_MESSAGE);
			return;
		}		
		
		Chart chart = searchForPanel(task.getIdentifier());		
		
		chart.clear();				
		chart.plot(task.getExperimentalCurve(), PlotType.EXPERIMENTAL_DATA, extendedCurvePlotting); //add exp data					
		
		tabbedPane.setSelectedIndex(tabbedPane.indexOfComponent(chart));
		
		if(task.getProblem() == null)
			return;
		
		HeatingCurve solutionCurve = task.getProblem().getHeatingCurve();
		
		if(solutionCurve == null)
			return;
		
		if(task.getScheme() == null)
			return;
		
		solutionCurve.setName(task.getProblem().shortName() +  " : " + task.getScheme().shortName());				
		chart.plot(extendedCurvePlotting ? 
				solutionCurve.extendedTo(task.getExperimentalCurve()) : solutionCurve, 
				PlotType.SOLUTION, extendedCurvePlotting); //add solution (if it exists)		
		
	}

	private enum StandardTabName {
		
		TaskManager(Messages.getString("TaskControlFrame.TaskManagerTitle")); //$NON-NLS-1$ //$NON-NLS-2$
		
		private String name;
		private Component component;
		
		private StandardTabName(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public Component getAssociatedComponent() {
			return component;
		}
		
		public void setAssociatedComponent(Component c) {
			this.component = c;
		}
		
	}
	
	private void addStandardTab(StandardTabName tabName, Component c, boolean closeable) {
		tabbedPane.addTab(tabName.getName(), null, c, null);
		int lastAdded = tabbedPane.getTabCount() - 1;
		if(closeable) 
			tabbedPane.setTabComponentAt(lastAdded, new ButtonTabComponent(tabbedPane));	
		tabName.setAssociatedComponent(c);
	}
	
	private JTabbedPane createTabbedPane() {
		JTabbedPane taskTabbedPane = new JTabbedPane(JTabbedPane.TOP);
		taskTabbedPane.setBackground(UIManager.getColor(Messages.getString("TaskControlFrame.Color")));		 //$NON-NLS-1$
	
		taskTabbedPane.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if(taskTabbedPane.getSelectedIndex() < 0)
					return;
				String title = taskTabbedPane.getTitleAt(taskTabbedPane.getSelectedIndex());
				Identifier id = Identifier.parse(title);
				if(id != null)
					TaskManager.selectTask(id, taskTabbedPane);
			}
			
		});
		
		return taskTabbedPane;
		
	}
	
	/*
	 * Task tool bar
	 */
	
	class TaskToolBar extends JToolBar {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -6823289494869587461L;
		
		private ToolBarButton btnRemove, btnClr, btnReset, btnShowCurve;
		private ExecutionButton controlButton;

		public TaskToolBar() {
			super();
			setBackground(new Color(240, 248, 255));
			setForeground(Color.BLACK);
			setLayout(new GridLayout());				
			
			btnRemove = new ToolBarButton(Messages.getString("TaskControlFrame.RemoveSelected")); //$NON-NLS-1$
			btnRemove.setEnabled(false);
			add(btnRemove);
			
			btnRemove.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					int[] rows = taskTable.getSelectedRows();
					Identifier id; 
					
					for(int i = rows.length - 1; i >= 0; i--) {
						id = (Identifier) taskTable.getValueAt(rows[i], 0);
						TaskManager.removeTask(TaskManager.getTask(id));
					}
					
					taskTable.clearSelection();
					
				}
					
			});
			
			btnClr = new ToolBarButton(Messages.getString("TaskControlFrame.Clear")); //$NON-NLS-1$
			btnClr.setEnabled(false);
			add(btnClr);
			
			btnClr.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					TaskManager.clear();
					logText.clear();
					resultsTable.clear();
				}
				
			});
			
			btnReset = new ToolBarButton(Messages.getString("TaskControlFrame.Reset")); //$NON-NLS-1$
			add(btnReset);
			btnReset.setEnabled(false);
			
			btnReset.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					TaskManager.reset();
					logText.clear();
					resultsTable.removeAll();
				}
				
			});
				
			btnShowCurve = new ToolBarButton(Messages.getString("TaskControlFrame.Graph")); //$NON-NLS-1$
			btnShowCurve.setEnabled(false);
			btnShowCurve.addActionListener(new ActionListener() {
					
				public void actionPerformed(ActionEvent e) {
					int[] rows = taskTable.getSelectedRows();
					
					if(rows.length > 1) 
						return;				 
																				
					plot(TaskManager.getSelectedTask(), false);
					
				}
					
			});
				
			add(btnShowCurve);
				
			controlButton = new ExecutionButton();
			controlButton.setEnabled(false);
			add(controlButton);
				
			setFloatable(false);			
			
		}
		
	}
	
	/*
	 * Main menu
	 */
	
	class MainMenu extends JMenuBar {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 5676122766981731562L;

		private Font MENU_FONT = new Font(Messages.getString("TaskControlFrame.MenuFont"), Font.BOLD, 16); //$NON-NLS-1$
		private Font MENU_ITEM_FONT = new Font(Messages.getString("TaskControlFrame.MenuFont"), Font.BOLD, 15); //$NON-NLS-1$
		
		public MainMenu() {
			super();
			
			setBackground(UIManager.getColor(Messages.getString("TaskControlFrame.Color2"))); //$NON-NLS-1$
			
			/*
			 * MENU ITEMS
			 */
			
			JMenu mnLoadData = new JMenu(Messages.getString("TaskControlFrame.LoadData")); //$NON-NLS-1$
			mnLoadData.setFont(MENU_FONT); //$NON-NLS-1$
			add(mnLoadData);
			
			JMenuItem itemLoadExp = new JMenuItem(Messages.getString("TaskControlFrame.ExperimentalData")); //$NON-NLS-1$
			itemLoadExp.setFont(MENU_ITEM_FONT); //$NON-NLS-1$
			itemLoadExp.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					askToLoadData();
					tabbedPane.setSelectedComponent(StandardTabName.TaskManager.getAssociatedComponent());
				}
			});
			
			mnLoadData.add(itemLoadExp);
			
			JMenuItem itemLoadMeta = new JMenuItem(Messages.getString("TaskControlFrame.MetaData")); //$NON-NLS-1$
			itemLoadMeta.setFont(MENU_ITEM_FONT); //$NON-NLS-1$
			itemLoadMeta.setEnabled(false);
			itemLoadMeta.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					askToLoadMetadata();
					tabbedPane.setSelectedComponent(StandardTabName.TaskManager.getAssociatedComponent());
				}
			});			
			
			mnLoadData.add(itemLoadMeta);
			
			JMenu mnSettings = new JMenu(Messages.getString("TaskControlFrame.CalculationSettings")); //$NON-NLS-1$
			mnSettings.setFont(MENU_FONT); //$NON-NLS-1$
			add(mnSettings);
			
			JMenuItem mnHeatProblem = new JMenuItem(Messages.getString("TaskControlFrame.MenuProblemStatement")); //$NON-NLS-1$
			mnHeatProblem.setFont(MENU_ITEM_FONT); //$NON-NLS-1$
			mnHeatProblem.setEnabled(false);
			mnHeatProblem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					Launcher.showProblemStatementFrame();
				}
			});					
			
			mnSettings.add(mnHeatProblem);
			
			JMenuItem mntmNewMenuItem_2 = new JMenuItem(Messages.getString("TaskControlFrame.MenuLeastSquares")); //$NON-NLS-1$
			mntmNewMenuItem_2.setFont(MENU_ITEM_FONT); //$NON-NLS-1$
			mntmNewMenuItem_2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					Launcher.showSearchOptionsFrame();
				}
			});
			mntmNewMenuItem_2.setEnabled(false);
			mnSettings.add(mntmNewMenuItem_2);						
			
			mnSettings.addSeparator();
			
			JMenuItem mntmNewMenuItem_3 = new JMenuItem(Messages.getString("TaskControlFrame.MenuChangeResultFormat")); //$NON-NLS-1$
			mntmNewMenuItem_3.setFont(MENU_ITEM_FONT); //$NON-NLS-1$
			mntmNewMenuItem_3.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					ResultChangeDialog changeDialog = new ResultChangeDialog();
					changeDialog.setLocationRelativeTo(instance);
					changeDialog.setAlwaysOnTop(true);
					changeDialog.setVisible(true);
					
					ResultFormat.addResultFormatListener(new ResultFormatListener() {

						@Override
						public void resultFormatChanged(ResultFormatEvent rfe) {
							ResultFormat res = rfe.getResultFormat();
							 ( (ResultTableModel)resultsTable.getModel()).changeFormat(res);
						}
						
					});
					
				}
			});
			mnSettings.add(mntmNewMenuItem_3);
			
			JMenu mnChartControls = new JMenu(Messages.getString("TaskControlFrame.ChartControls")); //$NON-NLS-1$
			mnChartControls.setFont(MENU_FONT); //$NON-NLS-1$
			add(mnChartControls);
			mnChartControls.setEnabled(false);
			
			TaskManager.addTaskRepositoryListener(new TaskRepositoryListener() {

				@Override
				public void onTaskListChanged(TaskRepositoryEvent e) {
					if(TaskManager.getTaskList().size() > 0) {
						itemLoadMeta.setEnabled(true);
						mnHeatProblem.setEnabled(true);
						mntmNewMenuItem_2.setEnabled(true);
						mnChartControls.setEnabled(true);
					} else {
						itemLoadMeta.setEnabled(false);
						mnHeatProblem.setEnabled(false);
						mntmNewMenuItem_2.setEnabled(false);
						mnChartControls.setEnabled(false);
					}
				}				
				
			});
			
			JMenuItem mntmShowClassicSolution = new JMenuItem(Messages.getString("TaskControlFrame.ShowParkerSolution")); //$NON-NLS-1$
			mntmShowClassicSolution.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					
					Chart chartPanel = activeChart();
					
					if(chartPanel == null) {
						Toolkit.getDefaultToolkit().beep();
						JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()),
							    Messages.getString("TaskControlFrame.ParkerNoChartsError"), //$NON-NLS-1$
							    Messages.getString("TaskControlFrame.CreateChartTitle"), //$NON-NLS-1$
							    JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					Problem problem = TaskManager.getSelectedTask().getProblem();
					
					if(problem == null)
						return;
	
					chartPanel.plot( HeatingCurve.classicSolution(problem, 
							TaskManager.getSelectedTask().getExperimentalCurve().timeLimit(), 30), PlotType.CLASSIC_SOLUTION, false);

				}
			});
			
			mntmShowClassicSolution.setFont(MENU_ITEM_FONT); //$NON-NLS-1$
			mnChartControls.add(mntmShowClassicSolution);
			
			JMenuItem mntmSaveAs = new JMenuItem(Messages.getString("TaskControlFrame.SaveAsButton")); //$NON-NLS-1$
			mntmSaveAs.setFont(MENU_ITEM_FONT); //$NON-NLS-1$
			mnChartControls.add(mntmSaveAs);
			mntmSaveAs.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					SearchTask selectedTask = TaskManager.getSelectedTask();
					
					if(selectedTask == null) {
						JOptionPane.showMessageDialog(instance, "No data to export!",
							    "No Data to Export",
							    JOptionPane.WARNING_MESSAGE);
						return; 
					}
					
					selectedTask.askToSave(instance);
					
					tabbedPane.setSelectedComponent(StandardTabName.TaskManager.getAssociatedComponent());
				}
			});
			
			JMenuItem mntmSaveAll = new JMenuItem(Messages.getString("TaskControlFrame.SaveAllButton")); //$NON-NLS-1$
			mntmSaveAll.setFont(MENU_ITEM_FONT); //$NON-NLS-1$
			mnChartControls.add(mntmSaveAll);
			mntmSaveAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					SaveableDirectory.askToSave(instance, 
							TaskManager.getInstance().describe(), 
							TaskManager.saveableContents()
							);					
					tabbedPane.setSelectedComponent(StandardTabName.TaskManager.getAssociatedComponent());
				}
			});
			
			JMenu mnHelp = new JMenu(Messages.getString("TaskControlFrame.InfoMenu")); //$NON-NLS-1$
			mnHelp.setFont(MENU_FONT); //$NON-NLS-1$
			add(mnHelp);
			
			JMenuItem mntmNewMenuItem_1 = new JMenuItem(Messages.getString("TaskControlFrame.AboutMenutItem")); //$NON-NLS-1$
			mntmNewMenuItem_1.setFont(MENU_ITEM_FONT); //$NON-NLS-1$
			mnHelp.add(mntmNewMenuItem_1);
			
			mntmNewMenuItem_1.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					String info = Messages.getString("TaskControlFrame.SoftwareDescription"); //$NON-NLS-1$
					JLabel label = new JLabel(info);
					JDialog aboutDialog = new AboutDialog();
					aboutDialog.setLocationRelativeTo(instance);
					aboutDialog.setAlwaysOnTop(true);
					aboutDialog.setVisible(true);
				}
				
				
			});
			
		}
		
		
	}
		
}