package pulse.ui.frames;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import pulse.HeatingCurve;
import pulse.input.Metadata;
import pulse.io.readers.MetaFileReader;
import pulse.io.readers.ReaderManager;
import pulse.tasks.Identifier;
import pulse.tasks.ResultFormat;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.ResultFormatEvent;
import pulse.tasks.listeners.ResultFormatListener;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskRepositoryListener;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.listeners.TaskSelectionListener;
import pulse.ui.Launcher;
import pulse.ui.charts.Charting;
import pulse.ui.charts.HeatingChartPanel;
import pulse.ui.components.ButtonTabComponent;
import pulse.ui.components.LogPane;
import pulse.ui.components.LogToolBar;
import pulse.ui.components.ResultTable;
import pulse.ui.components.ResultTable.ResultTableModel;
import pulse.ui.components.ResultsToolBar;
import pulse.ui.components.TaskTable;
import pulse.ui.components.TaskTable.TaskTableModel;
import pulse.ui.components.ToolBarButton;
import pulse.util.Request;
import pulse.util.RequestListener;
import pulse.util.SaveableDirectory;

import java.awt.Component;
import java.awt.Dimension;

import org.apache.commons.text.WordUtils;

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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
	private JPanel contentPane;
	private LogPane logText;
	
	private JScrollPane taskScroller;
	private JTabbedPane tabbedPane;
	
	private ResultTable resultsTable;
	private JSplitPane outputPane;
	
	private JSplitPane splitPane;
	
	private JPanel taskManagerPane;
	private TaskTable taskTable;
	private final static int RESULTS_HEADER_HEIGHT = 70;
	
	private static List<RequestListener> requestListeners = new ArrayList<RequestListener>();
	
	private TaskControlFrame reference;
	
	private final static int WIDTH = 1200;
	private final static int HEIGHT = 700;
	
	private BufferedImage logo;
	
	/**
	 * Create the frame.
	 */
	
	public TaskControlFrame() {
		try {
			logo = ImageIO.read(getClass().getResourceAsStream(Messages.getString("TaskControlFrame.LogoImagePath"))); //$NON-NLS-1$
		} catch (IOException e1) {
			System.err.println("Failed to load logo image"); //$NON-NLS-1$
			e1.printStackTrace();
		}
		
		setIconImage(logo);
		reference = this;
		
		setTitle(Messages.getString("TaskControlFrame.SoftwareTitle")); //$NON-NLS-1$
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setMinimumSize(new Dimension(WIDTH, HEIGHT));
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		
		setJMenuBar( new MainMenu() );										//create main menu
		
		tabbedPane = createTabbedPane();									//create tabbed pane
		contentPane.add( tabbedPane );
		
		//addStandardTab(StandardTabName.WelcomeInfo, new WelcomePane(), true);	//add welcome info

		taskManagerPane = new JPanel();		
		taskManagerPane.setLayout(new BorderLayout());
		
		taskTable = new TaskTable(); 	
		taskScroller = new JScrollPane(taskTable);
		taskManagerPane.add(taskScroller, BorderLayout.CENTER);
		
		TaskToolBar taskToolBar = new TaskToolBar();
		
		taskManagerPane.add(taskToolBar, BorderLayout.NORTH);	//add task toolbar
			
		outputPane = new JSplitPane();
		
		outputPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1)); 
	    outputPane.setBorder(null);
		
		outputPane.setOneTouchExpandable(true);
		outputPane.setResizeWeight(0.6);
		outputPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		
		resultsTable = new ResultTable(ResultFormat.defaultFormat);
		
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
		
		TaskManager.addSelectionListener(new TaskSelectionListener() {

			@Override
			public void onSelectionChanged(TaskSelectionEvent e) {
				logText.callUpdate();				
			}
			
		});
		
		TaskManager.addTaskRepositoryListener(new TaskRepositoryListener() {

			@Override
			public void onTaskListChanged(TaskRepositoryEvent e) {
				switch(e.getState()) {
				case TASK_ADDED :
				case TASK_REMOVED :
					break;									
				default :
					if(TaskManager.getTask(e.getId()) == TaskManager.getSelectedTask()) 																														
						logText.callUpdate();					
					break;			
				}
			}
			
		});
		
		JScrollPane scrollPane	= new JScrollPane(logText);
		bottomPane.add(scrollPane);
		bottomPane.add(new LogToolBar(this, logText), BorderLayout.SOUTH);
		
		outputPane.setBottomComponent(bottomPane);			//Sets bottom component
		
		splitPane = new JSplitPane();
		
		splitPane.setResizeWeight(0.5);
		splitPane.setOneTouchExpandable(true);

		splitPane.setLeftComponent(taskManagerPane);	//Sets left component
		splitPane.setRightComponent(outputPane);			//Sets right component
		
		addStandardTab(StandardTabName.TaskManager, splitPane, false);
		

		splitPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1)); 
		splitPane.setBorder(null);
		
		/*
		 * EVENTS
		 */
		
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
		
		Window ancestor = SwingUtilities.getWindowAncestor(this);
		
		requestListeners.add(new RequestListener() {

			@Override
			public void onRequestReceived(Request request) {
				
				switch(request.getType()) {
				
				case CHART :
				
					SearchTask t;
					for(Identifier id : request.getIdentifiers()) {
						t = TaskManager.getTask(id);
						plot(t, ancestor);
					}
			
				default : 
					return;
					
				}
							
			}
			
		});
		
	}
	
	/*
	 * Initiates the dialog to load heating curve experimental data
	 */
	
	public void askToLoadData() {
		JFileChooser fileChooser = new JFileChooser();
		
		File workingDirectory = new File(System.getProperty("user.home")); //$NON-NLS-1$
		fileChooser.setCurrentDirectory(workingDirectory);
		fileChooser.setMultiSelectionEnabled(true);
		
		List<String> extensions = ReaderManager.getHeatingCurveExtensions();							
		String[] extArray = extensions.toArray(new String[extensions.size()]);			
		
		fileChooser.setFileFilter(new FileNameExtensionFilter(Messages.getString("TaskControlFrame.ExtensionDescriptor"), extArray)); //$NON-NLS-1$
		
		if(fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;							
		
		try {
			SearchTask[] t = null;
			
			for( File f : fileChooser.getSelectedFiles() )  
				t = TaskManager.generateTasks(f);	
			
			if(TaskManager.dataNeedsTruncation()) {
				
				Object[] options = {"Truncate", "Do not change"}; //$NON-NLS-1$ //$NON-NLS-2$
				Toolkit.getDefaultToolkit().beep();
				int answer = JOptionPane.showOptionDialog(
						reference,
								WordUtils.wrap("It appears that in some experiments the acquisition time was too long. If the number of points is low, this will lead to under-represented statistics in regions critical to thermal diffusivity evaluation, leading to biased estimates. It is recommended to allow PULSE to truncate this data. Would you like to proceed? ", 75), //$NON-NLS-1$
								"Potential Problem with Data", //$NON-NLS-1$
								JOptionPane.YES_NO_OPTION,
								JOptionPane.WARNING_MESSAGE,
								null,
								options,
								options[1]);
				if(answer == 0) {
					TaskManager.truncateData();
					
				}
				
			}
			
			System.gc();
			
			//select last added task
			if(t != null)
				if(t.length > 0)
					TaskManager.selectTask(t[t.length-1].getIdentifier(), this);
			
		} catch (IOException e) {
			Toolkit.getDefaultToolkit().beep();
			JOptionPane.showMessageDialog(this,
				    Messages.getString("TaskControlFrame.LoadError"), //$NON-NLS-1$
				    Messages.getString("TaskControlFrame.IOError"), //$NON-NLS-1$
				    JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		
	}

	
	/*
	 * Inititaes the dialog to load metadata
	 */
	
	public void askToLoadMetadata() {
		JFileChooser fileChooser = new JFileChooser();
		
		File workingDirectory = new File(System.getProperty("user.home")); //$NON-NLS-1$
		fileChooser.setCurrentDirectory(workingDirectory);
		fileChooser.setMultiSelectionEnabled(false);
		
		MetaFileReader reader = MetaFileReader.getInstance();
		
		String extension = reader.getSupportedExtension();										
		
		fileChooser.setFileFilter(new FileNameExtensionFilter(Messages.getString("TaskControlFrame.ExtensionDescriptor"), extension)); //$NON-NLS-1$
		
		if(fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;							
		
		try {
			
			Metadata met;				
			File f = fileChooser.getSelectedFile();
			
			for(SearchTask task : TaskManager.getTaskList()) {
				if(task.getExperimentalCurve() == null)
					continue;
				
				met = task.getExperimentalCurve().getMetadata();
				reader.populateMetadata(f, met);
 				
			}
			
			System.gc();

			
		} catch (IOException e) {
			Toolkit.getDefaultToolkit().beep();
			JOptionPane.showMessageDialog(this,
				    Messages.getString("TaskControlFrame.LoadError"), //$NON-NLS-1$
				    Messages.getString("TaskControlFrame.IOError"), //$NON-NLS-1$
				    JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		
	}
	
	public static void addRequestListener(RequestListener rl) {
		requestListeners.add(rl);
	}
	
	public static List<RequestListener> getRequestListeners() {
		return requestListeners;
	}
	
	public HeatingChartPanel activeChart() {
		final int total = tabbedPane.getTabCount();
		
		for(int i = 0; i < total; i++) {
			Component component = tabbedPane.getComponentAt(i);
			
			if(! (component instanceof HeatingChartPanel) )
				continue;

			if(! (	tabbedPane.getSelectedIndex() == i))
				continue;
			
			return (HeatingChartPanel) component;			
		}
		
		return null;
		
	}

	public HeatingChartPanel searchForPanel(Identifier taskIdentifier) {
		List<HeatingChartPanel> allPanels = allChartPanels();
		for(HeatingChartPanel p : allPanels)
			if(p.getIdentifier().equals(taskIdentifier))
				return p;
		return null;
	}
	
	public List<HeatingChartPanel> allChartPanels() {
		final int total = tabbedPane.getTabCount();
		List<HeatingChartPanel> chartList = new ArrayList<HeatingChartPanel>();
		
		for(int i = 0; i < total; i++) {
			Component component = tabbedPane.getComponentAt(i);
			if(! (component instanceof HeatingChartPanel) )
				continue;
			chartList.add((HeatingChartPanel)component);			
		}
		
		return chartList;		
	}
	
	public void addChartTab(HeatingChartPanel tab) {		
		tabbedPane.add(TaskManager.getSelectedTask().toString(), tab);
		tabbedPane.setSelectedComponent(tab);
		int lastAdded = tabbedPane.getTabCount() - 1;
		tabbedPane.setTabComponentAt(lastAdded, new ButtonTabComponent(tabbedPane));
	}
	
	public void plot(SearchTask task, Window ancestor) {

		if(task == null) {
			JOptionPane.showMessageDialog(ancestor,
				    Messages.getString("TaskControlFrame.NoTaskError"), //$NON-NLS-1$
				    Messages.getString("TaskControlFrame.ShowError"), //$NON-NLS-1$
				    JOptionPane.WARNING_MESSAGE);
			return;
		}

		HeatingCurve solutionCurve;
		
		if(task.getProblem() == null)
			solutionCurve = null;
		else
			solutionCurve = task.getProblem().getHeatingCurve();
		
		HeatingChartPanel chart = searchForPanel(task.getIdentifier());	
		
		if( chart != null ) {
			tabbedPane.setSelectedIndex(tabbedPane.indexOfComponent(chart));
			chart.setHeatingCurve(solutionCurve);
		}
		else {		
			chart = new HeatingChartPanel(task.getIdentifier(), 
				task.getExperimentalCurve(), 
				solutionCurve);
			addChartTab(chart);
		}
			
		Charting.plotUsing(chart);
		
	}

	protected enum StandardTabName {
		
		TaskManager(Messages.getString("TaskControlFrame.TaskManagerTitle")), WelcomeInfo("Welcome Screen"); //$NON-NLS-1$ //$NON-NLS-2$
		
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
				String title = taskTabbedPane.getTitleAt(taskTabbedPane.getSelectedIndex());
				TaskManager.selectTask(Identifier.identify(title), taskTabbedPane);
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
		
		protected ToolBarButton btnRemove, btnClr, btnReset, btnShowCurve, btnFitTask;
		protected ControlButton controlButton;

		public TaskToolBar() {
			super();
			setBackground(new Color(240, 248, 255));
			setForeground(Color.BLACK);
			setLayout(new GridLayout());
			
			btnFitTask = new ToolBarButton(Messages.getString("TaskControlFrame.LoadData")); //$NON-NLS-1$
				btnFitTask.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					askToLoadData();
					tabbedPane.setSelectedComponent(StandardTabName.TaskManager.getAssociatedComponent());
				}
			});
				
			add(btnFitTask);
			
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
				}
				
			});
			
			btnReset = new ToolBarButton(Messages.getString("TaskControlFrame.Reset")); //$NON-NLS-1$
			add(btnReset);
			btnReset.setEnabled(false);
			
			btnReset.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					TaskManager.reset();
				}
				
			});
				
			btnShowCurve = new ToolBarButton(Messages.getString("TaskControlFrame.Graph")); //$NON-NLS-1$
			btnShowCurve.setEnabled(false);
			btnShowCurve.addActionListener(new ActionListener() {
					
				public void actionPerformed(ActionEvent e) {
					int[] rows = taskTable.getSelectedRows();
					List<Identifier> ids = new ArrayList<Identifier>(); 
					
					for(int selected : rows) {
						ids.add( (Identifier) taskTable.getValueAt(selected, 0) );
						Request r = new Request(Request.Type.CHART, ids);
						for(RequestListener rl : requestListeners) 
							rl.onRequestReceived(r);
					}
				}
					
			});
				
			add(btnShowCurve);
				
			controlButton = new ControlButton();
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

		private Font MENU_FONT = new Font(Messages.getString("TaskControlFrame.MenuFont"), Font.BOLD, 14); //$NON-NLS-1$
		private Font MENU_ITEM_FONT = new Font(Messages.getString("TaskControlFrame.MenuFont"), Font.BOLD, 13); //$NON-NLS-1$
		
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
					changeDialog.setLocationRelativeTo(reference);
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
			
			JMenuItem mntmClearChart = new JMenuItem(Messages.getString("TaskControlFrame.MenuItemClear")); //$NON-NLS-1$
			mntmClearChart.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					HeatingChartPanel chartPanel = activeChart();
					
					if(chartPanel == null) {
						Toolkit.getDefaultToolkit().beep();
						JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()),
							    Messages.getString("TaskControlFrame.NoChartsError"), //$NON-NLS-1$
							    Messages.getString("TaskControlFrame.CreateChartTitle"), //$NON-NLS-1$
							    JOptionPane.WARNING_MESSAGE);
						return;
					}
					
					chartPanel.clearChart();
				
				}
			});
			
			mntmClearChart.setFont(MENU_ITEM_FONT); //$NON-NLS-1$
			mnChartControls.add(mntmClearChart);
			
			JMenuItem mntmShowClassicSolution = new JMenuItem(Messages.getString("TaskControlFrame.ShowParkerSolution")); //$NON-NLS-1$
			mntmShowClassicSolution.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					
					HeatingChartPanel chartPanel = activeChart();
					
					if(chartPanel == null) {
						Toolkit.getDefaultToolkit().beep();
						JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()),
							    Messages.getString("TaskControlFrame.ParkerNoChartsError"), //$NON-NLS-1$
							    Messages.getString("TaskControlFrame.CreateChartTitle"), //$NON-NLS-1$
							    JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					Charting.plotClassicSolution(chartPanel.getHeatingChart());				
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
						JOptionPane.showMessageDialog(reference, "No data to export!",
							    "No Data to Export",
							    JOptionPane.WARNING_MESSAGE);
						return; 
					}
					
					selectedTask.askToSave(reference);
					
					tabbedPane.setSelectedComponent(StandardTabName.TaskManager.getAssociatedComponent());
				}
			});
			
			JMenuItem mntmSaveAll = new JMenuItem(Messages.getString("TaskControlFrame.SaveAllButton")); //$NON-NLS-1$
			mntmSaveAs.setFont(MENU_ITEM_FONT); //$NON-NLS-1$
			mnChartControls.add(mntmSaveAll);
			mntmSaveAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					SaveableDirectory.askToSave(reference, 
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
					label.setIcon(new ImageIcon(logo.getScaledInstance(logo.getWidth()/6, logo.getHeight()/6, java.awt.Image.SCALE_SMOOTH)));
					JOptionPane.showMessageDialog(reference, label, Messages.getString("TaskControlFrame.AboutTitle"), JOptionPane.PLAIN_MESSAGE); //$NON-NLS-1$
				}
				
				
			});
			
		}
		
		
	}
	
	class ControlButton extends ToolBarButton {
		/**
		 * 
		 */
		private static final long serialVersionUID = -6317905957478587768L;

		private ExecutionState state = ExecutionState.EXECUTE;
		
		public ControlButton() {
			super();
			setBackground(state.getColor());
			setText(state.getMessage());
			
			this.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					
					/*
					 * STOP PRESSED?
					 */
					
					if(state == ExecutionState.STOP) {
						TaskManager.cancelAllTasks();
						setExecutionState(ExecutionState.EXECUTE);	
						return;
					}
					
					/*
					 * EXECUTE PRESSED?
					 */
					
					if(TaskManager.getTaskList().isEmpty()) {
						JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()),
							    Messages.getString("TaskControlFrame.PleaseLoadData"), //$NON-NLS-1$
							    "No Tasks", //$NON-NLS-1$
							    JOptionPane.ERROR_MESSAGE);			
						return;
					}
						
					for(SearchTask t : TaskManager.getTaskList())
						switch(t.checkStatus()) {
							case READY :
							case TERMINATED :
							case DONE :
								continue;
							default : 
								JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()),
										    t + " is " + t.getStatus().getMessage() , //$NON-NLS-1$
										    "Task Not Ready", //$NON-NLS-1$
										    JOptionPane.ERROR_MESSAGE);			
								return;
						}

					TaskManager.executeAll();
							
				}
				
			});
			
			TaskManager.addTaskRepositoryListener(new TaskRepositoryListener() {

				@Override
				public void onTaskListChanged(TaskRepositoryEvent e) {
					switch(e.getState()) {
					case SINGLE_TASK_SUBMITTED :
					case MULTIPLE_TASKS_SUBMITTED :
						setExecutionState(ExecutionState.STOP);
						break;
					case TASK_FINISHED :
						if(TaskManager.isTaskQueueEmpty()) 
							setExecutionState(ExecutionState.EXECUTE);
						else
							setExecutionState(ExecutionState.STOP);
						break;
					default : 
						return;
					}
		
				}
				
			});
			
		}
		
		public void setExecutionState(ExecutionState state) {
			this.state = state;
			this.setText(state.getMessage());
			this.setBackground(state.getColor());
		}
		
		
		public ExecutionState getExecutionState() {
			return state;
		}
		
	}
	
	public enum ExecutionState {
		EXECUTE("EXECUTE", Color.GREEN), STOP("STOP", Color.RED);
		
		private Color color;
		private String message;
		
		private ExecutionState(String message, Color clr) {
			this.color = clr;
			this.message = message;
		}
		
		public Color getColor() {
			return color;
		}
		
		public String getMessage() {
			return message;
		}
		
	}
		
}