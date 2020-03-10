package pulse.ui.frames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.NumberFormatter;

import org.jfree.chart.ChartPanel;

import pulse.input.ExperimentalData;
import pulse.input.Metadata;
import pulse.input.Range;
import pulse.io.export.ExportManager;
import pulse.io.readers.MetaFileReader;
import pulse.io.readers.ReaderManager;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.statistics.CorrelationTest;
import pulse.search.statistics.NormalityTest;
import pulse.search.statistics.ResidualStatistic;
import pulse.tasks.Buffer;
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
import pulse.ui.components.Chart;
import pulse.ui.components.ExecutionButton;
import pulse.ui.components.LogPane;
import pulse.ui.components.ResultTable;
import pulse.ui.components.TaskTable;
import pulse.ui.components.TaskTable.TaskTableModel;
import pulse.ui.components.models.ResultTableModel;
import pulse.util.Reflexive;

@SuppressWarnings("serial")
public class TaskControlFrame extends JFrame {

	private final static int WIDTH = 1035;
	private final static int HEIGHT = 730;
	
	private int ICON_SIZE = 16;
	
	private static TaskControlFrame instance = new TaskControlFrame();
	
	private static File dir;
	
    private final static float SLIDER_A_COEF = 0.01f;
    private final static float SLIDER_B_COEF = 0.04605f;        
	
	/**
	 * Create the frame.
	 */
	
	private TaskControlFrame() {						
		initComponents();
		adjustEnabledControls();
		assignMenuFunctions();
		scheduleLogEvents();
		setIconImage(Launcher.loadIcon("logo.png", 32).getImage());
		startSystemMonitors();
		assignResultToolbar();
		adjustTablesToolbars();
		addButtonListeners();
		
		addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(java.awt.event.WindowEvent evt) {                               
			        JFrame closingWindow = (JFrame) evt.getSource();
			        if(!exitConfirmed(closingWindow)) {
			            closingWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			        } else
			            closingWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			}
			
		});
		
	}
	
	private void adjustTablesToolbars() {
		resultsTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				int[] selection = resultsTable.getSelectedRows();
				deleteEntryBtn.setEnabled(selection.length > 0);
			}
					
			}
		);
		
		resultsTable.getModel().addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent arg0) {
				previewBtn.setEnabled(	resultsTable.getRowCount() > 2 );
				mergeBtn.setEnabled(	resultsTable.getRowCount() > 1 );				
				saveResultsBtn.setEnabled( resultsTable.getRowCount() > 0 );
				undoBtn.setEnabled( resultsTable.getRowCount() > 0 );
			}
			
		});
	}
	

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        desktopPane = new javax.swing.JDesktopPane();

        globalLayout = new GridBagLayout();
        desktopPane.setLayout(globalLayout);

        global = new java.awt.GridBagConstraints();
        
        global.fill = GridBagConstraints.BOTH;
        global.insets = new Insets(5,5,5,5);
        
        taskManagerFrame = new javax.swing.JInternalFrame("", false, false, false, true);
        taskScrollPane = new javax.swing.JScrollPane();
        taskTable= new TaskTable();
		logTextPane	= new LogPane();
        taskToolbar = new javax.swing.JPanel();
        
        removeBtn = new javax.swing.JButton(Launcher.loadIcon("remove.png", ICON_SIZE));
        clearBtn = new javax.swing.JButton(Launcher.loadIcon("clear.png", ICON_SIZE));
        resetBtn = new javax.swing.JButton(Launcher.loadIcon("reset.png", ICON_SIZE));
        graphBtn = new javax.swing.JButton(Launcher.loadIcon("graph.png", ICON_SIZE));
        execBtn = new ExecutionButton();
        
        graphFrame = new javax.swing.JInternalFrame("", false, false, false, true);
        
        ChartPanel chart = Chart.createEmptyPanel();
        graphFrame.getContentPane().add(chart, BorderLayout.CENTER);        
        
        chart.setMaximumDrawHeight(2000);
        chart.setMaximumDrawWidth(2000);
        chart.setMinimumDrawWidth(10);
        chart.setMinimumDrawHeight(10);
        
        jSlider1 = new JSlider();
        jSlider1.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        jSlider1.setOrientation(SwingConstants.VERTICAL);
        jSlider1.setToolTipText("Slide to change the dataset opacity");
        
        jSlider1.addChangeListener(e -> {	
        	Chart.setOpacity( (float) (SLIDER_A_COEF*Math.exp(SLIDER_B_COEF*jSlider1.getValue())) );
	        plot();
        });
        
        graphFrame.getContentPane().add(jSlider1, java.awt.BorderLayout.LINE_END);
        
        chartToolbar = new javax.swing.JPanel();        
        lowerLimitField = new javax.swing.JFormattedTextField(new NumberFormatter());                        
        upperLimitField = new javax.swing.JFormattedTextField(new NumberFormatter());
        limitRangeBtn = new javax.swing.JButton();                        
        adiabaticSolutionBtn = new javax.swing.JToggleButton();
        residualsBtn = new javax.swing.JToggleButton();
        logFrame = new javax.swing.JInternalFrame("", false, false, false, true);                
        logScroller = new javax.swing.JScrollPane();
        logToolbar = new javax.swing.JPanel();
        
        saveLogBtn = new javax.swing.JButton(Launcher.loadIcon("save.png", ICON_SIZE));
        saveLogBtn.setToolTipText("Save");
        
        systemStatusBar = new javax.swing.JPanel();
        logToolbar = new javax.swing.JPanel();
        cpuLabel = new javax.swing.JLabel();
        memoryLabel = new javax.swing.JLabel();
        coresLabel = new javax.swing.JLabel();
        resultsFrame = new javax.swing.JInternalFrame("", false, false, false, true);
        resultsScroller = new javax.swing.JScrollPane();
        resultToolbar = new javax.swing.JPanel();
        
        deleteEntryBtn = new javax.swing.JButton(Launcher.loadIcon("remove.png", ICON_SIZE));
        mergeBtn = new javax.swing.JButton(Launcher.loadIcon("merge.png", ICON_SIZE));
        undoBtn = new javax.swing.JButton(Launcher.loadIcon("reset.png", ICON_SIZE));
        previewBtn = new javax.swing.JButton(Launcher.loadIcon("preview.png", ICON_SIZE));
        saveResultsBtn = new javax.swing.JButton(Launcher.loadIcon("save.png", ICON_SIZE));
        
        mainMenu = new javax.swing.JMenuBar();
        dataControlsMenu = new javax.swing.JMenu();
        loadDataItem = new javax.swing.JMenuItem();
        loadMetadataItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        exportCurrentItem = new javax.swing.JMenuItem();
        exportAllItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        exitItem = new javax.swing.JMenuItem();
        settingsMenu = new javax.swing.JMenu();
        modelSettingsItem = new javax.swing.JMenuItem();
        searchSettingsItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        resultFormatItem = new javax.swing.JMenuItem();
        InfoMenu = new javax.swing.JMenu();
        aboutItem = new javax.swing.JMenuItem();

        ed = new ExportDialog();
        
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(Messages.getString("TaskControlFrame.SoftwareTitle"));
        setPreferredSize(new java.awt.Dimension(WIDTH, HEIGHT));
        taskManagerFrame.setTitle("Task Manager");
        taskManagerFrame.setVisible(true);
     
        taskScrollPane.setViewportView(taskTable);

        taskManagerFrame.getContentPane().add(taskScrollPane, java.awt.BorderLayout.CENTER);

        taskToolbar.setLayout(new java.awt.GridLayout(1, 0));
        
		removeBtn.setEnabled(false);
		clearBtn.setEnabled(false);
		resetBtn.setEnabled(false);
		graphBtn.setEnabled(false);
		execBtn.setEnabled(false);
        
        removeBtn.setToolTipText("Remove Task");
        taskToolbar.add(removeBtn);

        clearBtn.setToolTipText("Clear All Tasks");
        taskToolbar.add(clearBtn);

        resetBtn.setToolTipText("Reset All Tasks");
        taskToolbar.add(resetBtn);

        graphBtn.setToolTipText("Show Graph");
        taskToolbar.add(graphBtn);

        execBtn.setToolTipText("Execute All Tasks");
        taskToolbar.add(execBtn);

        taskManagerFrame.getContentPane().add(taskToolbar, java.awt.BorderLayout.PAGE_START);
        
        desktopPane.add(taskManagerFrame, global);
        taskManagerFrame.setBounds(new Rectangle(10, 10, 420, 430));
        taskManagerFrame.setNormalBounds(new Rectangle(10, 10, 420, 430));

        graphFrame.setTitle("Time-temperature profile(s)");
        graphFrame.setVisible(true);
        
        chartToolbar.setLayout(new java.awt.GridBagLayout());
        
        var gbc = new java.awt.GridBagConstraints();
        //gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.2;
        
        lowerLimitField.setValue(0.0);
        
        String ghostText1 = "Lower bound";
        lowerLimitField.setText(ghostText1);
        
        String ghostText2 = "Upper bound";
        
        chartToolbar.add(lowerLimitField,gbc);

        upperLimitField.setValue(1.0);
        upperLimitField.setText(ghostText2);
        
        chartToolbar.add(upperLimitField,gbc);

        limitRangeBtn.setText("Limit Range To");
        
        lowerLimitField.setForeground(Color.GRAY);
        upperLimitField.setForeground(Color.GRAY);
        
        var ftfFocusListener = new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                JTextField src = (JTextField)e.getSource();
                if(src.getText().length() > 0) 
	                src.setForeground(Color.black);                
            }

            @Override
            public void focusLost(FocusEvent e) {
            	JFormattedTextField src = (JFormattedTextField)e.getSource();
                if(src.getValue() == null) {
                    src.setText(ghostText1);
                    src.setForeground(Color.gray);                    
                }
            }
        	
        };
        
        TaskManager.addSelectionListener(event -> 
        {
        	SearchTask t = event.getSelection();
        	
        	ExperimentalData expCurve = t.getExperimentalCurve();        	
        	
        	lowerLimitField.setValue(expCurve.getRange().getSegment().getMinimum());
        	upperLimitField.setValue(expCurve.getRange().getSegment().getMaximum());        	        	
        	
        });
        
        TaskManager.addTaskRepositoryListener(e -> {
        	
        	if(e.getState() == TaskRepositoryEvent.State.TASK_FINISHED) {
        		
        		SearchTask t = TaskManager.getSelectedTask();
        		
        		if(e.getId().equals(t.getIdentifier())) {
	        		lowerLimitField.setValue(t.getExperimentalCurve().getRange().getSegment().getMinimum());
	            	upperLimitField.setValue(t.getExperimentalCurve().getRange().getSegment().getMaximum());
	            	plot();
        		}
        		
        	}
        	
        });
        
        lowerLimitField.addFocusListener(ftfFocusListener);
        upperLimitField.addFocusListener(ftfFocusListener);
        
        limitRangeBtn.addActionListener(e -> processRange() );        
        
        chartToolbar.add(limitRangeBtn,gbc);

        adiabaticSolutionBtn.setToolTipText("Sanity check (original adiabatic solution)");
        adiabaticSolutionBtn.setIcon(Launcher.loadIcon("parker.png", ICON_SIZE));
        
        adiabaticSolutionBtn.addActionListener(e -> {
        	Chart.setZeroApproximationShown(adiabaticSolutionBtn.isSelected());
        	plot();
        });
   
        gbc.weightx = 0.1;
        chartToolbar.add(adiabaticSolutionBtn,gbc);
        
        residualsBtn.setToolTipText("Plot residuals");
        residualsBtn.setIcon(Launcher.loadIcon("residuals.png", ICON_SIZE));
        residualsBtn.setSelected(true);
        
        residualsBtn.addActionListener(e -> {
        	Chart.setResidualsShown(residualsBtn.isSelected());
        	plot();
        });
        
        gbc.weightx = 0.1;
        chartToolbar.add(residualsBtn,gbc);

        graphFrame.getContentPane().add(chartToolbar, java.awt.BorderLayout.PAGE_END);

        desktopPane.add(graphFrame, global);
        graphFrame.setBounds(new Rectangle(440, 10, 570, 430));
        graphFrame.setNormalBounds(new Rectangle(440, 10, 570, 430));

        logFrame.setTitle("Log");
        logFrame.setVisible(true);

        logScroller.setViewportView(logTextPane);

        logFrame.getContentPane().setLayout(new BorderLayout());
        logFrame.getContentPane().add(logScroller, java.awt.BorderLayout.CENTER);

        logToolbar.setLayout(new GridLayout());

        logToolbar.add(saveLogBtn);
        logFrame.getContentPane().add(logToolbar, java.awt.BorderLayout.LINE_END);

        logToolbar.add(saveLogBtn);
        
		verboseCheckBox = new JCheckBox(Messages.getString("LogToolBar.Verbose")); //$NON-NLS-1$
		verboseCheckBox.setSelected(Log.isVerbose());
		verboseCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        logToolbar.add(verboseCheckBox);
        
        systemStatusBar.setLayout(new java.awt.GridBagLayout());
        
        GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        
        cpuLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        cpuLabel.setText("CPU:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weightx = 2.5;
        systemStatusBar.add(cpuLabel, gridBagConstraints);

        memoryLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        memoryLabel.setText("Memory:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weightx = 2.5;
        systemStatusBar.add(memoryLabel, gridBagConstraints);

        coresLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        coresLabel.setText("{n cores} ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weightx = 2.5;
        systemStatusBar.add(coresLabel, gridBagConstraints);

        logFrame.getContentPane().add(systemStatusBar, java.awt.BorderLayout.PAGE_END);
        logFrame.getContentPane().add(logToolbar, java.awt.BorderLayout.NORTH);

        desktopPane.add(logFrame, global);
        logFrame.setBounds(new Rectangle(10, 450, 420, 220));
        logFrame.setNormalBounds(new Rectangle(10, 450, 420, 220));

        resultsFrame.setTitle("Results");
        resultsFrame.setVisible(true);

        resultsTable = new ResultTable(ResultFormat.DEFAULT_FORMAT);
        resultsScroller.setViewportView(resultsTable);

        resultsFrame.getContentPane().add(resultsScroller, java.awt.BorderLayout.CENTER);

        resultToolbar.setLayout(new java.awt.GridLayout(5, 0));

        deleteEntryBtn.setToolTipText("Delete Entry");
        resultToolbar.add(deleteEntryBtn);

        mergeBtn.setToolTipText("Merge (Auto)");
        resultToolbar.add(mergeBtn);

        undoBtn.setToolTipText("Undo");
        resultToolbar.add(undoBtn);

        previewBtn.setToolTipText("Preview");
        resultToolbar.add(previewBtn);

        saveResultsBtn.setToolTipText("Save");
        resultToolbar.add(saveResultsBtn);

        resultsFrame.getContentPane().add(resultToolbar, java.awt.BorderLayout.EAST);

        desktopPane.add(resultsFrame,global);
        resultsFrame.setBounds(new Rectangle(440, 450, 570, 220));
        resultsFrame.setNormalBounds(new Rectangle(440, 450, 570, 220));

        getContentPane().add(desktopPane, java.awt.BorderLayout.CENTER);

        dataControlsMenu.setMnemonic('f');
        dataControlsMenu.setText("File");

        loadDataItem.setMnemonic('h');
        loadDataItem.setIcon(Launcher.loadIcon("load.png", ICON_SIZE));
        loadDataItem.setText("Load Heating Curve(s)...");
        dataControlsMenu.add(loadDataItem);

        loadMetadataItem.setMnemonic('m');
        loadMetadataItem.setIcon(Launcher.loadIcon("metadata.png", ICON_SIZE));
        loadMetadataItem.setText("Load Metadata...");
        loadMetadataItem.setEnabled(false);
        dataControlsMenu.add(loadMetadataItem);
        dataControlsMenu.add(jSeparator2);

        exportCurrentItem.setText("Export Current");
        exportCurrentItem.setMnemonic('c');
        exportCurrentItem.setIcon(Launcher.loadIcon("save.png", ICON_SIZE));
        exportCurrentItem.setEnabled(false);
        
        exportCurrentItem.addActionListener(e -> {
			SearchTask selectedTask = TaskManager.getSelectedTask();
			
			if(selectedTask == null) {
				JOptionPane.showMessageDialog(instance, "No data to export!",
					    "No Data to Export",
					    JOptionPane.WARNING_MESSAGE);
				return; 
			}
			
			var fileChooser = new JFileChooser();
			fileChooser.setMultiSelectionEnabled(false);
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			
		    int returnVal = fileChooser.showSaveDialog(this);
		    
		    if (returnVal == JFileChooser.APPROVE_OPTION) {
				dir = new File(fileChooser.getSelectedFile() + File.separator + TaskManager.getInstance().describe());
				dir.mkdirs();
				ExportManager.exportCurrentTask(dir);
		    }
			
        });
       
        dataControlsMenu.add(exportCurrentItem);
        
        Font menuFont = new Font("Arial", Font.PLAIN, 14);
        dataControlsMenu.setFont(menuFont);

        exportAllItem.setText("Export...");
        exportAllItem.setMnemonic('a');
        exportAllItem.setIcon(Launcher.loadIcon("save.png", ICON_SIZE));
        exportAllItem.setEnabled(false);

        dataControlsMenu.add(exportAllItem);
        dataControlsMenu.add(jSeparator3);

        exitItem.setMnemonic('x');
        exitItem.setText("Exit");
        exitItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitItemActionPerformed(evt);
            }
        });
        dataControlsMenu.add(exitItem);

        mainMenu.add(dataControlsMenu);

        settingsMenu.setMnemonic('e');
        settingsMenu.setText("Calculation Settings");
        settingsMenu.setFont(menuFont);

        modelSettingsItem.setText("Heat Problem: Statement & Solution");
        modelSettingsItem.setIcon(Launcher.loadIcon("heat_problem.png", ICON_SIZE));
        modelSettingsItem.setEnabled(false);
        settingsMenu.add(modelSettingsItem);

        searchSettingsItem.setText("Parameter Estimation: Method & Settings");
        searchSettingsItem.setIcon(Launcher.loadIcon("inverse_problem.png", ICON_SIZE));
        searchSettingsItem.setEnabled(false);        

        resultFormatItem.setIcon(Launcher.loadIcon("result_format.png", ICON_SIZE));
        resultFormatItem.setText("Change Result Format...");
        
        JMenu analysisSubMenu = new JMenu("Statistical Analysis");
        
        JMenu statisticsSubMenu = new JMenu("Normality tests");
        statisticsSubMenu.setIcon(Launcher.loadIcon("normality_test.png", ICON_SIZE));
        
        ButtonGroup statisticItems = new ButtonGroup();
        
        JRadioButtonMenuItem item = null;                                
        
        for(String statisticName : Reflexive.allDescriptors(NormalityTest.class)) {
        	item = new JRadioButtonMenuItem(statisticName);
        	statisticItems.add(item);
            statisticsSubMenu.add(item);
            item.addItemListener(e -> {
            	
	            if( ( (JRadioButtonMenuItem) e.getItem() ).isSelected() ) {
	            	var text = ((JMenuItem)e.getItem()).getText();
	            	NormalityTest.setSelectedTestDescriptor( text );
	            	
	            	TaskManager.getTaskList().stream().forEach(t -> t.initNormalityTest());
	            	
	            }
            	
            });
        }
        
        var significanceDialog = new FormattedInputDialog(NumericProperty.theDefault(NumericPropertyKeyword.SIGNIFICANCE));
        
        significanceDialog.setConfirmAction( () -> NormalityTest.setStatisticalSignificance(
        		NumericProperty.derive(NumericPropertyKeyword.SIGNIFICANCE, significanceDialog.value()) ) );
        
        JMenuItem sigItem = new JMenuItem("Change significance...");
        statisticsSubMenu.add(new JSeparator());
        statisticsSubMenu.add(sigItem);
        sigItem.addActionListener(e ->     	
        	significanceDialog.setVisible(true)      	
        );
    
        statisticsSubMenu.getItem(0).setSelected(true);
        analysisSubMenu.add(statisticsSubMenu);
        
        JMenu optimisersSubMenu = new JMenu("Optimiser statistics");
        optimisersSubMenu.setIcon(Launcher.loadIcon("optimiser.png", ICON_SIZE));
        
        ButtonGroup optimisersItems = new ButtonGroup();
        
        item = null;                            
        
        var set = Reflexive.allDescriptors(ResidualStatistic.class);
        set.removeAll(Reflexive.allDescriptors(NormalityTest.class));
        
        for(String statisticName : set) {
        	item = new JRadioButtonMenuItem(statisticName);
        	optimisersItems.add(item);
            optimisersSubMenu.add(item);
            item.addItemListener(e -> {
            	
	            if( ( (JRadioButtonMenuItem) e.getItem() ).isSelected() ) {
	            	var text = ((JMenuItem)e.getItem()).getText();
	            	ResidualStatistic.setSelectedOptimiserDescriptor( text );	            	
	            	TaskManager.getTaskList().stream().forEach(t -> t.initOptimiser());	            	
	            }
            	
            });
        }
        
        optimisersSubMenu.getItem(0).setSelected(true);        
        analysisSubMenu.add(optimisersSubMenu);
        
        //
        
        JMenu correlationsSubMenu = new JMenu("Correlation tests");
        correlationsSubMenu.setIcon(Launcher.loadIcon("correlation.png", ICON_SIZE));
        
        ButtonGroup corrItems = new ButtonGroup();
        
        JRadioButtonMenuItem corrItem = null;                                
        
        for(String corrName : Reflexive.allDescriptors(CorrelationTest.class)) {
        	corrItem = new JRadioButtonMenuItem(corrName);
        	corrItems.add(corrItem);
            correlationsSubMenu.add(corrItem);
            corrItem.addItemListener(e -> {
            	
	            if( ( (JRadioButtonMenuItem) e.getItem() ).isSelected() ) {
	            	var text = ((JMenuItem)e.getItem()).getText();
	            	CorrelationTest.setSelectedTestDescriptor( text );	            	
	            	TaskManager.getTaskList().stream().forEach(t -> t.initCorrelationTest());	            	
	            }
            	
            });
        }
        
        var thresholdDialog = new FormattedInputDialog(NumericProperty.theDefault(NumericPropertyKeyword.CORRELATION_THRESHOLD));
        
        thresholdDialog.setConfirmAction( () -> CorrelationTest.setThreshold(
        		NumericProperty.derive(NumericPropertyKeyword.CORRELATION_THRESHOLD, thresholdDialog.value()) ) );
        
        JMenuItem thrItem = new JMenuItem("Change threshold...");
        correlationsSubMenu.add(new JSeparator());
        correlationsSubMenu.add(thrItem);
        thrItem.addActionListener(e ->     	
        	thresholdDialog.setVisible(true)      	
        );
    
        correlationsSubMenu.getItem(0).setSelected(true);
        
        JMenuItem selectBuffer = new JMenuItem("Buffer size...");
        selectBuffer.setIcon(Launcher.loadIcon("buffer.png", ICON_SIZE));
        
        var bufferDialog = new FormattedInputDialog(NumericProperty.theDefault(NumericPropertyKeyword.BUFFER_SIZE));
        
        bufferDialog.setConfirmAction( () -> Buffer.setSize(
        		NumericProperty.derive(NumericPropertyKeyword.BUFFER_SIZE, bufferDialog.value()) ) );
        
        selectBuffer.addActionListener(e -> 
        	bufferDialog.setVisible(true)
        );  
        
        analysisSubMenu.add(correlationsSubMenu);
        
        settingsMenu.add(searchSettingsItem);
        settingsMenu.add(analysisSubMenu);
        settingsMenu.add(jSeparator1);
        settingsMenu.add(resultFormatItem);
        settingsMenu.add(selectBuffer);
        
        mainMenu.add(settingsMenu);

        InfoMenu.setText("Info");
        InfoMenu.setFont(menuFont);

        aboutItem.setText("About...");
        InfoMenu.add(aboutItem);

        mainMenu.add(InfoMenu);

        setJMenuBar(mainMenu);

        previewFrame = new PreviewFrame();
        desktopPane.add(previewFrame, global);
        
        previewFrame.addInternalFrameListener(new InternalFrameAdapter() {
        	
        	@Override
        	public void internalFrameClosing(InternalFrameEvent e) {
        		hidePreviewFrame();
        	}
        	
        });
        
		problemStatementFrame = 
				new ProblemStatementFrame();
		
		problemStatementFrame.addInternalFrameListener(new InternalFrameAdapter() {
        	
        	@Override
        	public void internalFrameClosing(InternalFrameEvent e) {
        		hideProblemStatementFrame();
        	}
        	
        });
		
		desktopPane.add(problemStatementFrame, global);
        		
		searchOptionsFrame = 
				new SearchOptionsFrame(  );
		
		searchOptionsFrame.addInternalFrameListener(new InternalFrameAdapter() {
        	
        	@Override
        	public void internalFrameClosing(InternalFrameEvent e) {
        		hideSearchOptionsFrame();
        	}
        	
        });
		
		desktopPane.add(searchOptionsFrame, global);
		
        /*
         * CONSTRAIN ADJUSTMENT
         */
		
		resetConstraints();	
        
		var ifa = new InternalFrameAdapter() {
			
			@Override
			public void internalFrameDeiconified(InternalFrameEvent e) {
				resetConstraints();
			}
			
		};
		
        taskManagerFrame.addInternalFrameListener(ifa);        
        graphFrame.addInternalFrameListener(ifa);
        logFrame.addInternalFrameListener(ifa);
        resultsFrame.addInternalFrameListener(ifa);
        
        pack();
    }// </editor-fold>                        

    private void exitItemActionPerformed(java.awt.event.ActionEvent evt) {                                         
        if(exitConfirmed((Component) evt.getSource()))
            System.exit(0);
    }                                        

    private boolean exitConfirmed(Component closingComponent) {
        Object[] options = {"Yes", "No"};
	return JOptionPane.showOptionDialog(
                                    closingComponent,
                                    Messages.getString("TaskControlFrame.ExitMessage"), 
                                    Messages.getString("TaskControlFrame.ExitTitle"), 
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.WARNING_MESSAGE,
                                    null,
                                    options,
                                    options[1]) == JOptionPane.YES_OPTION;
    }
	
	public void scheduleLogEvents() {
		TaskManager.addSelectionListener(new TaskSelectionListener() {

			@Override
			public void onSelectionChanged(TaskSelectionEvent e) {				
				logTextPane.callPrintAll();
				plot();
			}
			
		});
		
		TaskManager.addTaskRepositoryListener( event -> { 
			if(event.getState() != TaskRepositoryEvent.State.TASK_ADDED)
				return;
						
			exportCurrentItem.setEnabled(true);
			exportAllItem.setEnabled(true);
			
			SearchTask task = TaskManager.getTask(event.getId());
			
			task.getLog().addListener( new LogEntryListener() {

				@Override
				public void onNewEntry(LogEntry e) {
					if(TaskManager.getSelectedTask() == task) 
						logTextPane.callUpdate();	
				}
				
				@Override
				public void onLogFinished(Log log) {
					if(TaskManager.getSelectedTask() == task) {
						
						try {
							logTextPane.getUpdateExecutor().awaitTermination(10, TimeUnit.MILLISECONDS);
						} catch (InterruptedException e) {
							System.err.println("Log not finished in time");
							e.printStackTrace();
						}
						
						logTextPane.printTimeTaken(log);
						
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
							clearBtn.setEnabled(false);
							resetBtn.setEnabled(false);
							execBtn.setEnabled(false);
						} else {
							clearBtn.setEnabled(true);
							resetBtn.setEnabled(true);
							execBtn.setEnabled(true);
						}
					}						
					
					
		});
		
		taskTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				int[] selection = taskTable.getSelectedRows();
				if(taskTable.getSelectedRow() < 0) {
					removeBtn.setEnabled(false);
					graphBtn.setEnabled(false);				
				} else {
					if(selection.length > 1) {
						removeBtn.setEnabled(false);
						graphBtn.setEnabled(false);
					}
					else if(selection.length > 0) {
						removeBtn.setEnabled(true);
						graphBtn.setEnabled(true);
					}
				}
			}
		
		});
	}
	
	public static TaskControlFrame getInstance() {
		return instance;
	}
	
	private void startSystemMonitors() {		
		String coresAvailable = String.format("{" + (Launcher.threadsAvailable()+1) + " cores}");
		coresLabel.setText(coresAvailable);
		
		ScheduledExecutorService executor =
			    Executors.newSingleThreadScheduledExecutor();

			Runnable periodicTask = new Runnable() {
			    @Override
				public void run() {
			    	double cpuUsage = Launcher.cpuUsage();
			    	double memoryUsage = Launcher.getMemoryUsage();			    
			    	
					String cpuString = String.format("CPU usage: %3.1f%%", cpuUsage);
					cpuLabel.setText(cpuString);
					String memoryString = String.format("Memory usage: %3.1f%%", memoryUsage);
					memoryLabel.setText(memoryString);
			        
			        if(cpuUsage > 75)
			        	cpuLabel.setForeground(Color.red);
			        else if(cpuUsage > 50) 			        	
			        	cpuLabel.setForeground(Color.yellow);
			        else
			        	cpuLabel.setForeground(Color.black);
			        
			        
			        /*
			         * 
			         */
			        
			        if(memoryUsage > 75)
			        	memoryLabel.setForeground(Color.red);
			        else if(memoryUsage > 50) 			        	
			        	memoryLabel.setForeground(Color.yellow);
			        else
			        	memoryLabel.setForeground(Color.black);
			        
			    }
			};
			
		executor.scheduleAtFixedRate(periodicTask, 0, 2, TimeUnit.SECONDS);
	}
	
	/*
	 * Initiates the dialog to load experimental data
	 */
	
	public static void loadDataDialog() {
		JFileChooser fileChooser = new JFileChooser();
		if(dir != null) 
			fileChooser.setCurrentDirectory(dir);
		
		fileChooser.setMultiSelectionEnabled(true);
		
		List<String> extensions = ReaderManager.getCurveExtensions();							
		String[] extArray = extensions.toArray(new String[extensions.size()]);			
		
		fileChooser.setFileFilter(new FileNameExtensionFilter(Messages.getString("TaskControlFrame.ExtensionDescriptor"), extArray)); 
		
		boolean approve = fileChooser.showOpenDialog(instance) == JFileChooser.APPROVE_OPTION;
		dir = fileChooser.getCurrentDirectory();
		
		if(!approve)
			return;
		
		TaskManager.generateTasks(Arrays.asList(fileChooser.getSelectedFiles()));
		TaskManager.selectFirstTask();									
				
	}
	
	/*
	 * Initiates the dialog to truncate experimental data
	 */

	public static void truncateDataDialog() {								
		Object[] options = {"Truncate", "Do not change"}; 
		int answer = JOptionPane.showOptionDialog(
				instance,
						("The acquisition time for some experiments appears to be too long.\nIf time resolution is low, the model estimates will be biased.\n\nIt is recommended to allow PULSE to truncate this data.\n\nWould you like to proceed? "), //$NON-NLS-1$
						"Potential Problem with Data", 
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
	
	public static void loadMetadataDialog() {
		JFileChooser fileChooser = new JFileChooser();
		
		if(dir != null)
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
			JOptionPane.showMessageDialog(instance,
				    Messages.getString("TaskControlFrame.LoadError"), 
				    Messages.getString("TaskControlFrame.IOError"), 
				    JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		
		if(TaskManager.dataNeedsTruncation())
			truncateDataDialog();
				
		TaskManager.selectFirstTask();
		
	}

	public void plot() {
		Chart.plot(TaskManager.getSelectedTask(), false);
	}
	
	private void addButtonListeners() {
		removeBtn.addActionListener(new ActionListener() {

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
		
		/*
		 * 
		 */
		
		clearBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				TaskManager.clear();
				logTextPane.clear();
				resultsTable.clear();
			}
			
		});
		
		/*
		 * 
		 */
		
		resetBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				TaskManager.reset();
				logTextPane.clear();
				resultsTable.removeAll();
			}
			
		});
		
		graphBtn.addActionListener(e -> plot() );		
		verboseCheckBox.addActionListener( event -> Log.setVerbose(verboseCheckBox.isSelected()) ) ;
		
		saveLogBtn.addActionListener(e -> {
			if(logTextPane.getDocument().getLength() > 0)
				ExportManager.askToExport(logTextPane, instance, Messages.getString("LogToolBar.FileFormatDescriptor"));
		});
		
	}
	
	public void assignMenuFunctions() {
		loadDataItem.addActionListener(e -> loadDataDialog());			
		loadMetadataItem.setEnabled(false);
		loadMetadataItem.addActionListener(e -> loadMetadataDialog());		
		
		modelSettingsItem.setEnabled(false);
		modelSettingsItem.addActionListener(e -> showProblemStatementFrame());
		
		searchSettingsItem.setEnabled(false);
		searchSettingsItem.addActionListener(e -> showSearchOptionsFrame() );
		
		resultFormatItem.addActionListener(e -> {
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
							
		});
		
		TaskManager.addTaskRepositoryListener(new TaskRepositoryListener() {

			@Override
			public void onTaskListChanged(TaskRepositoryEvent e) {
				if(TaskManager.getTaskList().size() > 0) {
					loadMetadataItem.setEnabled(true);
					modelSettingsItem.setEnabled(true);
					searchSettingsItem.setEnabled(true);
				} else {
					loadMetadataItem.setEnabled(false);
					modelSettingsItem.setEnabled(false);
					searchSettingsItem.setEnabled(false);					}
			}				
			
		});
		
		exportAllItem.setEnabled(true);
		exportAllItem.addActionListener(e -> {
			ed.setLocationRelativeTo(null);
			ed.setAlwaysOnTop(true);
			ed.setVisible(true);
		}
				 );
		
		aboutItem.addActionListener(e -> {
				JDialog aboutDialog = new AboutDialog();
				aboutDialog.setLocationRelativeTo(instance);
				aboutDialog.setAlwaysOnTop(true);
				aboutDialog.setVisible(true);
			});
		
	}
	
	/*
	 * 
	 */
	
	public void assignResultToolbar() {
		deleteEntryBtn.setEnabled(false);
		deleteEntryBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ResultTableModel rtm = (ResultTableModel) resultsTable.getModel();
				
				int[] selection = resultsTable.getSelectedRows();								
				
				if(selection.length < 0)
					return;
			
				for(int i = selection.length - 1; i >= 0; i--) 
					rtm.remove( 
							rtm.getResults().get(resultsTable.convertRowIndexToModel(selection[i])) );
				
			}
			
		});
				
		mergeBtn.setEnabled(false);
		mergeBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(resultsTable.getRowCount() < 1)
					return;
				
				inputDialog.setLocationRelativeTo(null);
				inputDialog.setVisible(true);
				inputDialog.setConfirmAction( () -> resultsTable.merge(inputDialog.value().doubleValue()) );
			}
			
		});		
		
		undoBtn.setEnabled(false);
		undoBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ResultTableModel dtm = (ResultTableModel) resultsTable.getModel();
				
				for(int i = dtm.getRowCount()-1; i >= 0; i--) 
					dtm.remove( dtm.getResults().get(resultsTable.convertRowIndexToModel(i)) );
				
				TaskManager.getTaskList().stream().map(t -> TaskManager.getResult(t)).forEach(r -> dtm.addRow(r));				
			}
			
		});
				
		previewBtn.setEnabled(false);
		previewBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				if(resultsTable.getModel().getRowCount() < 1) {
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()),
						    Messages.getString("ResultsToolBar.NoDataError"), //$NON-NLS-1$
						    Messages.getString("ResultsToolBar.NoResultsError"), //$NON-NLS-1$
						    JOptionPane.ERROR_MESSAGE);			
					return;
				}						
				
				showPreviewFrame();
				
			}
			
		});
		
		saveResultsBtn.setEnabled(false);
		saveResultsBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(resultsTable.getRowCount() < 1) {
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()),
						    Messages.getString("ResultsToolBar.7"),
						    Messages.getString("ResultsToolBar.8"), 
						    JOptionPane.ERROR_MESSAGE);			
					return;
				}
				
				ExportManager.askToExport(resultsTable, instance, "Calculation results");
				
			}
			
			
		});
	}
	
	/*
	 * 
	 */
	
	private void showPreviewFrame() {
		previewFrame.update(
		    		((ResultTableModel)resultsTable.getModel()).getFormat()
		    		, resultsTable.data());
		
		global.fill = GridBagConstraints.BOTH;
		global.gridx = 0;
		global.gridy = 0;
		global.weightx = 1.0;
		global.weighty = 0.65;
		
		globalLayout.setConstraints(previewFrame, global);
		
		global.fill = GridBagConstraints.BOTH;
		global.gridx = 0;
		global.gridy = 1;
		global.weightx = 1.0;
		global.weighty = 0.35;
		
		globalLayout.setConstraints(resultsFrame, global);
		
		previewFrame.setVisible(true);				
		resultsFrame.setVisible(true);		
		taskManagerFrame.setVisible(false);
		graphFrame.setVisible(false);
		logFrame.setVisible(false);				
	}
	
	private void resetConstraints() {
		global.fill = GridBagConstraints.BOTH;
		global.gridx = 1;
		global.gridy = 1;
		global.weightx = 0.6;
		global.weighty = 0.35;
		
		globalLayout.setConstraints(resultsFrame, global);
		
		global.fill = GridBagConstraints.BOTH;
		global.gridx = 1;
		global.gridy = 0;
		global.weightx = 0.6;
		global.weighty = 0.65;
		
		globalLayout.setConstraints(graphFrame, global);
		
		global.fill = GridBagConstraints.BOTH;
		global.gridx = 0;
		global.gridy = 1;
		global.weightx = 0.4;
		global.weighty = 0.35;
		
		globalLayout.setConstraints(logFrame, global);
		
		global.fill = GridBagConstraints.BOTH;
		global.gridx = 0;
		global.gridy = 0;
		global.weightx = 0.4;
		global.weighty = 0.65;
		
		globalLayout.setConstraints(taskManagerFrame, global);
		
		global.fill = GridBagConstraints.BOTH;
		global.gridx = 0;
		global.gridy = 0;
		global.weightx = 1.0;
		global.weighty = 0.65;
		
		globalLayout.setConstraints(previewFrame, global);
		
		global.fill = GridBagConstraints.BOTH;
		global.gridx = 0;
		global.gridy = 0;
		global.weightx = 1.0;
		global.weighty = 0.65;
		
		globalLayout.setConstraints(problemStatementFrame, global);
		
		global.fill = GridBagConstraints.BOTH;
		global.gridx = 0;
		global.gridy = 0;
		global.weightx = 1.0;
		global.weighty = 1.0;
		
		globalLayout.setConstraints(problemStatementFrame, global);
	}
	
	private void hidePreviewFrame() {
		resetConstraints();		
		previewFrame.setVisible(false);				
		resultsFrame.setVisible(true);		
		taskManagerFrame.setVisible(true);
		graphFrame.setVisible(true);
		logFrame.setVisible(true);				
	}
	
	private void hideProblemStatementFrame() {
		resetConstraints();		
		previewFrame.setVisible(false);
		problemStatementFrame.setVisible(false);
		resultsFrame.setVisible(true);		
		taskManagerFrame.setVisible(true);
		graphFrame.setVisible(true);
		logFrame.setVisible(true);				
	}
	
	private void hideSearchOptionsFrame() {
		resetConstraints();		
		searchOptionsFrame.setVisible(false);
		previewFrame.setVisible(false);
		problemStatementFrame.setVisible(false);
		resultsFrame.setVisible(true);		
		taskManagerFrame.setVisible(true);
		graphFrame.setVisible(true);
		logFrame.setVisible(true);				
	}
	
	private void showProblemStatementFrame() {	
		problemStatementFrame.update();
		problemStatementFrame.setVisible(true);
		
		global.fill = GridBagConstraints.BOTH;
		global.gridx = 0;
		global.gridy = 1;
		global.weightx = 1.0;
		global.weighty = 0.35;
		
		globalLayout.setConstraints(graphFrame, global);
		
		global.fill = GridBagConstraints.BOTH;
		global.gridx = 0;
		global.gridy = 0;
		global.weightx = 1.0;
		global.weighty = 0.65;
		
		globalLayout.setConstraints(problemStatementFrame, global);
		
		searchOptionsFrame.setVisible(false);
		previewFrame.setVisible(false);				
		resultsFrame.setVisible(false);		
		taskManagerFrame.setVisible(false);
		graphFrame.setVisible(true);
		logFrame.setVisible(false);		
	}
	
	private void showSearchOptionsFrame() {	
		searchOptionsFrame.update();
		searchOptionsFrame.setVisible(true);
		
		global.fill = GridBagConstraints.BOTH;
		global.gridx = 0;
		global.gridy = 0;
		global.weightx = 1.0;
		global.weighty = 1.0;
		
		globalLayout.setConstraints(searchOptionsFrame, global);
		
		problemStatementFrame.setVisible(false);
		previewFrame.setVisible(false);				
		resultsFrame.setVisible(false);		
		taskManagerFrame.setVisible(false);
		graphFrame.setVisible(false);
		logFrame.setVisible(false);		
	}
	
    private boolean userSaysRevert(JFormattedTextField ftf) {
        Toolkit.getDefaultToolkit().beep();
        ftf.selectAll();
        Object[] options = {Messages.getString("NumberEditor.EditText"), 
                            Messages.getString("NumberEditor.RevertText")};
        int answer = JOptionPane.showOptionDialog(
            SwingUtilities.getWindowAncestor(ftf),            
            "<html>Time domain should be consistent with the experimental data range.<br>" 
            + Messages.getString("NumberEditor.MessageLine1") 
            + Messages.getString("NumberEditor.MessageLine2") + "</html>",
            Messages.getString("NumberEditor.InvalidText"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.ERROR_MESSAGE,
            null,
            options,
            options[1]);
         
        if (answer == 1) { //Revert!
            ftf.setValue(ftf.getValue());
        return true;
        }
    return false;
    }
    
    private void processRange() {
        if ( (!lowerLimitField.isEditValid()) || (!upperLimitField.isEditValid()) ) { //The text is invalid.
            if (userSaysRevert(lowerLimitField)) { //reverted
            	lowerLimitField.postActionEvent(); //inform the editor
            }
        }                       
        
        else {            	
        	double lower = ((Number)lowerLimitField.getValue()).doubleValue();            	
        	double upper = ((Number)upperLimitField.getValue()).doubleValue();            	            	
        	validateRange(lower,upper); 
        	plot();
        }
    }
        
    private void validateRange(double a, double b) {
    	SearchTask task = TaskManager.getSelectedTask();
    	
    	if(task == null)
    		return;
    	
    	ExperimentalData expCurve = task.getExperimentalCurve();
    	
    	if(expCurve == null)
    		return;
    			
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html><p>");
		sb.append(Messages.getString("RangeSelectionFrame.ConfirmationMessage1"));
		sb.append("</p><br>");
		sb.append(Messages.getString("RangeSelectionFrame.ConfirmationMessage2"));
		sb.append(expCurve.getEffectiveStartTime());
		sb.append(" to ");
		sb.append(expCurve.getEffectiveEndTime());
		sb.append("<br><br>");
		sb.append(Messages.getString("RangeSelectionFrame.ConfirmationMessage3"));
		sb.append(String.format("%3.4f", a) + " to " + String.format("%3.4f", b));
		sb.append("</html>");
		
		int dialogResult = JOptionPane.showConfirmDialog 
				(this, 
						sb.toString(), 
						"Confirm chocie", JOptionPane.YES_NO_OPTION);
		
		if(dialogResult == JOptionPane.YES_OPTION)			
			expCurve.setRange(new Range(a, b));	
		
    }        
	
    // Variables declaration - do not modify                     
    private javax.swing.JMenu InfoMenu;
    private javax.swing.JMenuItem aboutItem;
    private javax.swing.JPanel chartToolbar;
    private javax.swing.JButton clearBtn;
    private javax.swing.JLabel coresLabel;
    private javax.swing.JLabel cpuLabel;
    private javax.swing.JMenu dataControlsMenu;
    private javax.swing.JButton deleteEntryBtn;
    private javax.swing.JDesktopPane desktopPane;
    private javax.swing.JButton execBtn;
    private javax.swing.JMenuItem exitItem;
    private javax.swing.JMenuItem exportAllItem;
    private javax.swing.JMenuItem exportCurrentItem;
    private javax.swing.JButton graphBtn;
    private javax.swing.JInternalFrame graphFrame;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JButton limitRangeBtn;
    private javax.swing.JMenuItem loadDataItem;
    private javax.swing.JMenuItem loadMetadataItem;
    private javax.swing.JInternalFrame logFrame;
    private javax.swing.JScrollPane logScroller;
    private javax.swing.JPanel logToolbar;
    private LogPane logTextPane;
    private javax.swing.JFormattedTextField lowerLimitField;
    private javax.swing.JMenuBar mainMenu;
    private javax.swing.JLabel memoryLabel;
    private javax.swing.JButton mergeBtn;
    private javax.swing.JMenuItem modelSettingsItem;
    private javax.swing.JButton previewBtn;
    private javax.swing.JButton removeBtn;
    private javax.swing.JButton resetBtn;
    private javax.swing.JMenuItem resultFormatItem;	
	private ResultTable resultsTable;	
    private javax.swing.JPanel resultToolbar;
    private javax.swing.JInternalFrame resultsFrame;
    private javax.swing.JScrollPane resultsScroller;
    private javax.swing.JButton saveLogBtn;
    private javax.swing.JButton saveResultsBtn;
    private javax.swing.JMenuItem searchSettingsItem;
    private javax.swing.JMenu settingsMenu;
    private javax.swing.JPanel systemStatusBar;
    private javax.swing.JInternalFrame taskManagerFrame;
    private javax.swing.JScrollPane taskScrollPane;
	private TaskTable taskTable;
    private javax.swing.JPanel taskToolbar;
    private javax.swing.JButton undoBtn;
    private javax.swing.JFormattedTextField upperLimitField;
    private javax.swing.JToggleButton adiabaticSolutionBtn;
    private javax.swing.JCheckBox verboseCheckBox;
    private javax.swing.JToggleButton residualsBtn;
	private FormattedInputDialog inputDialog = new FormattedInputDialog(NumericProperty.theDefault(NumericPropertyKeyword.WINDOW));
	private JSlider jSlider1;
	private PreviewFrame previewFrame;
	private ProblemStatementFrame problemStatementFrame;
	private SearchOptionsFrame searchOptionsFrame;
	private GridBagConstraints global;
	private GridBagLayout globalLayout;
	private ExportDialog ed;
    // End of variables declaration                   
		
}