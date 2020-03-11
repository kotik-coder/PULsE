package pulse.ui.frames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
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

import pulse.input.ExperimentalData;
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

	private final static int HEIGHT = 730;
	private final static int WIDTH = 1035;
	
	private final static float SLIDER_A_COEF = 0.01f;
    private final static float SLIDER_B_COEF = 0.04605f;                                        

	private final static int ICON_SIZE = 16;	    	
	
	private static FormattedInputDialog averageWindowDialog;
	private static File dir;
	
	private static GridBagConstraints globalConstraints;
    private static GridBagLayout globalLayout;
	
	private static JInternalFrame graphFrame;	

	private static TaskControlFrame instance = new TaskControlFrame();
	
    private static JInternalFrame logFrame;
    
    private static LogPane logTextPane;
    
    private static PreviewFrame previewFrame;
		
	private static JInternalFrame resultsFrame;
	
	private static ResultTable resultTable;
	  
    private static JInternalFrame taskManagerFrame;
    
	private static JPanel assignResultToolbar() {
    	var resultToolbar = new JPanel();
        var deleteEntryBtn = new JButton(Launcher.loadIcon("remove.png", ICON_SIZE));
        var mergeBtn = new JButton(Launcher.loadIcon("merge.png", ICON_SIZE));
        var undoBtn = new JButton(Launcher.loadIcon("reset.png", ICON_SIZE));
        var previewBtn = new JButton(Launcher.loadIcon("preview.png", ICON_SIZE));
        var saveResultsBtn = new JButton(Launcher.loadIcon("save.png", ICON_SIZE));
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
        
		resultTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				int[] selection = resultTable.getSelectedRows();
				deleteEntryBtn.setEnabled(selection.length > 0);
			}
					
			}
		);
		
		resultTable.getModel().addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent arg0) {
				previewBtn.setEnabled(	resultTable.getRowCount() > 2 );
				mergeBtn.setEnabled(	resultTable.getRowCount() > 1 );				
				saveResultsBtn.setEnabled( resultTable.getRowCount() > 0 );
				undoBtn.setEnabled( resultTable.getRowCount() > 0 );
			}
			
		});
    	
		deleteEntryBtn.setEnabled(false);
		deleteEntryBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ResultTableModel rtm = (ResultTableModel) resultTable.getModel();
				
				int[] selection = resultTable.getSelectedRows();								
				
				if(selection.length < 0)
					return;
			
				for(int i = selection.length - 1; i >= 0; i--) 
					rtm.remove( 
							rtm.getResults().get(resultTable.convertRowIndexToModel(selection[i])) );
				
			}
			
		});
				
		mergeBtn.setEnabled(false);
		mergeBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(resultTable.getRowCount() < 1)
					return;
				
				showInputDialog();
			}
			
		});		
		
		undoBtn.setEnabled(false);
		undoBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ResultTableModel dtm = (ResultTableModel) resultTable.getModel();
				
				for(int i = dtm.getRowCount()-1; i >= 0; i--) 
					dtm.remove( dtm.getResults().get(resultTable.convertRowIndexToModel(i)) );
				
				TaskManager.getTaskList().stream().map(t -> TaskManager.getResult(t)).forEach(r -> dtm.addRow(r));				
			}
			
		});
				
		previewBtn.setEnabled(false);
		previewBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				if(resultTable.getModel().getRowCount() < 1) {
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
				if(resultTable.getRowCount() < 1) {
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) e.getSource()),
						    Messages.getString("ResultsToolBar.7"),
						    Messages.getString("ResultsToolBar.8"), 
						    JOptionPane.ERROR_MESSAGE);			
					return;
				}
				
				ExportManager.askToExport(resultTable, instance, "Calculation results");
				
			}
			
			
		});
		return resultToolbar;
	}
	
	private static File directory() {
		if(dir != null)
			return dir;
		else
			try {
				return new File(TaskControlFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			} catch (URISyntaxException e) {
				System.err.println("Cannot determine current working directory.");
				e.printStackTrace();
			}
		return null;
	}
	
	public static TaskControlFrame getInstance() {
		return instance;
	}
	private static JPanel initChartToolbar() {		
		var chartToolbar = new JPanel();
        chartToolbar.setLayout(new java.awt.GridBagLayout());
		
		var lowerLimitField = new JFormattedTextField(new NumberFormatter());                        
        var upperLimitField = new JFormattedTextField(new NumberFormatter());
        
        var limitRangeBtn = new JButton();                        
        var adiabaticSolutionBtn = new JToggleButton();
        var residualsBtn = new JToggleButton();
		
		var gbc = new java.awt.GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.25;
        
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
        
        limitRangeBtn.addActionListener(e -> 
        {
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
        } );        
        
        gbc.weightx = 0.25;
        chartToolbar.add(limitRangeBtn,gbc);

        adiabaticSolutionBtn.setToolTipText("Sanity check (original adiabatic solution)");
        adiabaticSolutionBtn.setIcon(Launcher.loadIcon("parker.png", ICON_SIZE));
        
        adiabaticSolutionBtn.addActionListener(e -> {
        	Chart.setZeroApproximationShown(adiabaticSolutionBtn.isSelected());
        	plot();
        });
   
        gbc.weightx = 0.125;
        chartToolbar.add(adiabaticSolutionBtn,gbc);
        
        residualsBtn.setToolTipText("Plot residuals");
        residualsBtn.setIcon(Launcher.loadIcon("residuals.png", ICON_SIZE));
        residualsBtn.setSelected(true);
        
        residualsBtn.addActionListener(e -> {
        	Chart.setResidualsShown(residualsBtn.isSelected());
        	plot();
        });
        
        gbc.weightx = 0.125;
        chartToolbar.add(residualsBtn,gbc);
        return chartToolbar;
	}

	private static JInternalFrame initGraphFrame() {
		var graphFrame = new JInternalFrame("", false, false, false, true);
		var chart = Chart.createEmptyPanel();
        graphFrame.getContentPane().add(chart, BorderLayout.CENTER);
        graphFrame.setTitle("Time-temperature profile(s)");                
        
        chart.setMaximumDrawHeight(2000);
        chart.setMaximumDrawWidth(2000);
        chart.setMinimumDrawWidth(10);
        chart.setMinimumDrawHeight(10);       
        
        graphFrame.getContentPane().add(initOpacitySlider(), BorderLayout.LINE_END);                               
        graphFrame.getContentPane().add(initChartToolbar(), java.awt.BorderLayout.PAGE_END);
        
        return graphFrame;
	}
	
	private static JPanel initLogToolbar() {

        var logToolbar = new JPanel();
        logToolbar.setLayout(new GridLayout());
        
        var saveLogBtn = new JButton(Launcher.loadIcon("save.png", ICON_SIZE));
        saveLogBtn.setToolTipText("Save");
        
		var verboseCheckBox = new JCheckBox(Messages.getString("LogToolBar.Verbose")); //$NON-NLS-1$
		verboseCheckBox.setSelected(Log.isVerbose());
		verboseCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		
		verboseCheckBox.addActionListener( event -> Log.setVerbose(verboseCheckBox.isSelected()) ) ;
		
		saveLogBtn.addActionListener(e -> {
			if(logTextPane.getDocument().getLength() > 0)
				ExportManager.askToExport(logTextPane, instance, Messages.getString("LogToolBar.FileFormatDescriptor"));
		});
        
        logToolbar.add(saveLogBtn);
        logToolbar.add(verboseCheckBox);
        return logToolbar;
        
	}
	
	private static JSlider initOpacitySlider() {
        JSlider opacitySlider = new JSlider();
        opacitySlider.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        opacitySlider.setOrientation(SwingConstants.VERTICAL);
        opacitySlider.setToolTipText("Slide to change the dataset opacity");
        
        opacitySlider.addChangeListener(e -> {	
        	Chart.setOpacity( (float) (SLIDER_A_COEF*Math.exp(SLIDER_B_COEF*opacitySlider.getValue())) );
	        plot();
        });
        return opacitySlider;
    }	
	private static JInternalFrame initResultsFrame() {
		var resultsFrame = new JInternalFrame("", false, false, false, true);				
        JScrollPane resultsScroller = new JScrollPane();
        resultTable = new ResultTable(ResultFormat.DEFAULT_FORMAT);
        JPanel resultToolbar = assignResultToolbar();                
        
        resultsScroller.setViewportView(resultTable);
        resultsFrame.getContentPane().add(resultsScroller, java.awt.BorderLayout.CENTER);
       
        resultsFrame.setTitle("Results");
        resultsFrame.setVisible(true);              

        resultsFrame.getContentPane().add(resultToolbar, java.awt.BorderLayout.EAST);        		
        
        return resultsFrame;                       
	}
	
	public static void loadDataDialog() {
		var files = userInput( Messages.getString("TaskControlFrame.ExtensionDescriptor"), ReaderManager.getCurveExtensions() ); 
		
		if(files != null) {		
			TaskManager.generateTasks(files);
			TaskManager.selectFirstTask();
		}
				
	}
	
	public static void loadMetadataDialog() {
		MetaFileReader reader = MetaFileReader.getInstance();
		var file = userInputSingle(Messages.getString("TaskControlFrame.ExtensionDescriptor"), reader.getSupportedExtension());

		//attempt to fill metadata and problem 
		try {											
			
			for(SearchTask task : TaskManager.getTaskList()) {
				ExperimentalData data = task.getExperimentalCurve();				
				 
				reader.populateMetadata(file, data.getMetadata());
					
				Problem p = task.getProblem();					
				if(p != null) p.retrieveData(data);								
 				
			}
			
		} catch (IOException e) {
			JOptionPane.showMessageDialog(instance,
				    Messages.getString("TaskControlFrame.LoadError"), 
				    Messages.getString("TaskControlFrame.IOError"), 
				    JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		
		//check if the data loaded needs truncation		
		if(TaskManager.dataNeedsTruncation())
			truncateDataDialog();
				
		//select first of the generated task
		TaskManager.selectFirstTask();
		
	}
	public static void plot() {
		Chart.plot(TaskManager.getSelectedTask(), false);		
	}
	
    private static void showInputDialog() {
		averageWindowDialog.setLocationRelativeTo(null);
		averageWindowDialog.setVisible(true);
		averageWindowDialog.setConfirmAction( () -> resultTable.merge(averageWindowDialog.value().doubleValue()) );
	}
    private static void showPreviewFrame() {
		previewFrame.update(
		    		((ResultTableModel)resultTable.getModel()).getFormat()
		    		, resultTable.data());
		
		globalConstraints.fill = GridBagConstraints.BOTH;
		globalConstraints.gridx = 0;
		globalConstraints.gridy = 0;
		globalConstraints.weightx = 1.0;
		globalConstraints.weighty = 0.65;
		
		globalLayout.setConstraints(previewFrame, globalConstraints);
		
		globalConstraints.fill = GridBagConstraints.BOTH;
		globalConstraints.gridx = 0;
		globalConstraints.gridy = 1;
		globalConstraints.weightx = 1.0;
		globalConstraints.weighty = 0.35;
		
		globalLayout.setConstraints(resultsFrame, globalConstraints);
		
		previewFrame.setVisible(true);				
		resultsFrame.setVisible(true);		
		taskManagerFrame.setVisible(false);
		graphFrame.setVisible(false);
		logFrame.setVisible(false);				
	}
    private static void truncateDataDialog() {								
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
    private static List<File> userInput(String descriptor, List<String> extensions) {
    	JFileChooser fileChooser = new JFileChooser();
		 
		fileChooser.setCurrentDirectory( directory());		
		fileChooser.setMultiSelectionEnabled(true);

		String[] extArray = extensions.toArray(new String[extensions.size()]);					
		fileChooser.setFileFilter(new FileNameExtensionFilter( descriptor, extArray)); 
		
		boolean approve = fileChooser.showOpenDialog(instance) == JFileChooser.APPROVE_OPTION;
		dir = fileChooser.getCurrentDirectory();
		
		return approve ? Arrays.asList(fileChooser.getSelectedFiles()) : null;
    }
    private static File userInputSingle(String descriptor,  List<String> extensions) {
    	JFileChooser fileChooser = new JFileChooser();
		 
		fileChooser.setCurrentDirectory( directory());		
		fileChooser.setMultiSelectionEnabled(false);

		String[] extArray = extensions.toArray(new String[extensions.size()]);					
		fileChooser.setFileFilter(new FileNameExtensionFilter( descriptor, extArray)); 
		
		boolean approve = fileChooser.showOpenDialog(instance) == JFileChooser.APPROVE_OPTION;
		dir = fileChooser.getCurrentDirectory();
		
		return approve ? fileChooser.getSelectedFile() : null;
    }
    private static File userInputSingle(String descriptor, String... extensions) {
    	return userInputSingle(descriptor, Arrays.asList(extensions));				
    }
    private static boolean userSaysRevert(JFormattedTextField ftf) {
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
    private static void validateRange(double a, double b) {
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
				(getInstance(), 
						sb.toString(), 
						"Confirm chocie", JOptionPane.YES_NO_OPTION);
		
		if(dialogResult == JOptionPane.YES_OPTION)			
			expCurve.setRange(new Range(a, b));	
		
    }
    private JMenuItem aboutItem;
    private JButton clearBtn;
    private JMenu dataControlsMenu;
    private JButton execBtn;
    private JMenuItem exitItem;
    private JMenuItem exportAllItem;
    private JMenuItem exportCurrentItem;
    private ExportDialog exportDialog;
    private JButton graphBtn;
    private JMenuItem loadDataItem;	
	
    private JMenuItem loadMetadataItem;
    private JMenuBar mainMenu;
    private JMenuItem modelSettingsItem;
   
    private ProblemStatementFrame problemStatementFrame;
    
    private JButton removeBtn;
    
    private JButton resetBtn;
	private JMenuItem resultFormatItem;
    private SearchOptionsFrame searchOptionsFrame;
    private JMenuItem searchSettingsItem;
    private JMenu settingsMenu;
    
    private TaskTable taskTable;
    
    /**
	 * Create the frame.
	 */
	
	private TaskControlFrame() {
        setTitle(Messages.getString("TaskControlFrame.SoftwareTitle"));
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
		initComponents();
		assignMenuFunctions();
		scheduleLogEvents();
		setIconImage(Launcher.loadIcon("logo.png", 32).getImage());
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
		
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();
		
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
				resultTable.clear();
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
				resultTable.removeAll();
			}
			
		});
		
		graphBtn.addActionListener(e -> plot() );				
		
	}
    
	public void adjustEnabledControls(TaskTable taskTable) {
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
						 ( (ResultTableModel)resultTable.getModel()).changeFormat(res);
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
			exportDialog.setLocationRelativeTo(null);
			exportDialog.setAlwaysOnTop(true);
			exportDialog.setVisible(true);
		}
				 );
		
		aboutItem.addActionListener(e -> {
				JDialog aboutDialog = new AboutDialog();
				aboutDialog.setLocationRelativeTo(instance);
				aboutDialog.setAlwaysOnTop(true);
				aboutDialog.setVisible(true);
			});
		
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
	private void exitItemActionPerformed(java.awt.event.ActionEvent evt) {                                         
        if(exitConfirmed((Component) evt.getSource()))
            System.exit(0);
    }
	private void hidePreviewFrame() {
		previewFrame.setVisible(false);				
		resultsFrame.setVisible(true);		
		taskManagerFrame.setVisible(true);
		graphFrame.setVisible(true);
		logFrame.setVisible(true);				
	}
	private void hideProblemStatementFrame() {
		previewFrame.setVisible(false);
		problemStatementFrame.setVisible(false);
		resultsFrame.setVisible(true);		
		taskManagerFrame.setVisible(true);
		graphFrame.setVisible(true);
		logFrame.setVisible(true);				
	}
	private void hideSearchOptionsFrame() {		
		searchOptionsFrame.setVisible(false);
		previewFrame.setVisible(false);
		problemStatementFrame.setVisible(false);
		resultsFrame.setVisible(true);		
		taskManagerFrame.setVisible(true);
		graphFrame.setVisible(true);
		logFrame.setVisible(true);				
	}
	/**
     * This method is called from within the constructor to initialize the form.    
     */           
    
    private void initComponents() {

        var desktopPane = new JDesktopPane();
        
        globalLayout = new GridBagLayout();
        desktopPane.setLayout(globalLayout);

        globalConstraints = new java.awt.GridBagConstraints();        
        globalConstraints.fill = GridBagConstraints.BOTH;
        globalConstraints.insets = new Insets(5,5,5,5);
        
        taskManagerFrame = new JInternalFrame("", false, false, false, true);
        var taskScrollPane = new JScrollPane();
        
        var taskTable = new TaskTable();
		logTextPane	= new LogPane();
        var taskToolbar = new JPanel();
        
        removeBtn = new JButton(Launcher.loadIcon("remove.png", ICON_SIZE));
        clearBtn = new JButton(Launcher.loadIcon("clear.png", ICON_SIZE));
        resetBtn = new JButton(Launcher.loadIcon("reset.png", ICON_SIZE));
        graphBtn = new JButton(Launcher.loadIcon("graph.png", ICON_SIZE));
        execBtn = new ExecutionButton();
        
        graphFrame = initGraphFrame();
        desktopPane.add(graphFrame, globalConstraints);
        graphFrame.setVisible(true);       
                
        logFrame = new JInternalFrame("", false, false, false, true);                
        var logScroller = new JScrollPane();
                
        resultsFrame = initResultsFrame();
        
        mainMenu = new JMenuBar();
        dataControlsMenu = new JMenu();
        loadDataItem = new JMenuItem();
        loadMetadataItem = new JMenuItem();
        exportCurrentItem = new JMenuItem();
        exportAllItem = new JMenuItem();
        exitItem = new JMenuItem();
        settingsMenu = new JMenu();
        modelSettingsItem = new JMenuItem();
        searchSettingsItem = new JMenuItem();
        resultFormatItem = new JMenuItem();
        var infoMenu = new JMenu();
        aboutItem = new JMenuItem();

        exportDialog = new ExportDialog();
        averageWindowDialog = new FormattedInputDialog(NumericProperty.theDefault(NumericPropertyKeyword.WINDOW));
                
        taskManagerFrame.setTitle("Task Manager");
        taskManagerFrame.setVisible(true);
     
        taskScrollPane.setViewportView(taskTable);
		adjustEnabledControls(taskTable);

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

        taskManagerFrame.getContentPane().add(taskToolbar, BorderLayout.PAGE_START);
        
        desktopPane.add(taskManagerFrame, globalConstraints);

        logFrame.setTitle("Log");
        logFrame.setVisible(true);

        logScroller.setViewportView(logTextPane);

        logFrame.getContentPane().setLayout(new BorderLayout());
        logFrame.getContentPane().add(logScroller, java.awt.BorderLayout.CENTER);                		       
        
        GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;             
        
        logFrame.getContentPane().add( 
        		initSystemPanel(globalLayout, globalConstraints) 
        		, java.awt.BorderLayout.PAGE_END);
        
        logFrame.getContentPane().add(initLogToolbar(), java.awt.BorderLayout.NORTH);

        desktopPane.add(logFrame, globalConstraints);
        
        desktopPane.add(resultsFrame,globalConstraints);

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
        dataControlsMenu.add(new JSeparator());

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
        dataControlsMenu.add(new JSeparator());

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
        settingsMenu.add(new JSeparator());
        settingsMenu.add(resultFormatItem);
        settingsMenu.add(selectBuffer);
        
        mainMenu.add(settingsMenu);

        infoMenu.setText("Info");
        infoMenu.setFont(menuFont);

        aboutItem.setText("About...");
        infoMenu.add(aboutItem);

        mainMenu.add(infoMenu);

        setJMenuBar(mainMenu);

        previewFrame = new PreviewFrame();
        desktopPane.add(previewFrame, globalConstraints);
        
        previewFrame.addInternalFrameListener(new InternalFrameAdapter() {
        	
        	@Override
        	public void internalFrameClosing(InternalFrameEvent e) {
        		resetConstraints(globalLayout, globalConstraints);
        		hidePreviewFrame();
        	}
        	
        });
        
		problemStatementFrame = 
				new ProblemStatementFrame();
		
		problemStatementFrame.addInternalFrameListener(new InternalFrameAdapter() {
        	
        	@Override
        	public void internalFrameClosing(InternalFrameEvent e) {
        		resetConstraints(globalLayout, globalConstraints);
        		hideProblemStatementFrame();
        	}
        	
        });
		
		desktopPane.add(problemStatementFrame, globalConstraints);
        		
		searchOptionsFrame = 
				new SearchOptionsFrame(  );
		
		searchOptionsFrame.addInternalFrameListener(new InternalFrameAdapter() {
        	
        	@Override
        	public void internalFrameClosing(InternalFrameEvent e) {
        		resetConstraints(globalLayout, globalConstraints);
        		hideSearchOptionsFrame();
        	}
        	
        });
		
		desktopPane.add(searchOptionsFrame, globalConstraints);
		
        /*
         * CONSTRAIN ADJUSTMENT
         */
		
		resetConstraints(globalLayout, globalConstraints);	
        
		var ifa = new InternalFrameAdapter() {
			
			@Override
			public void internalFrameDeiconified(InternalFrameEvent e) {
				resetConstraints(globalLayout,globalConstraints);
			}
			
		};
		
        taskManagerFrame.addInternalFrameListener(ifa);        
        graphFrame.addInternalFrameListener(ifa);
        logFrame.addInternalFrameListener(ifa);
        resultsFrame.addInternalFrameListener(ifa);
        
    }
	
	private JPanel initSystemPanel(GridBagLayout gridBagLayout, GridBagConstraints gridBagConstraints) {
        
        var systemStatusBar = new JPanel();
        systemStatusBar.setLayout(new java.awt.GridBagLayout());
		
		var cpuLabel = new JLabel();        
        var memoryLabel = new JLabel();
        var coresLabel = new JLabel();
        
		cpuLabel.setHorizontalAlignment(SwingConstants.LEFT);
        cpuLabel.setText("CPU:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weightx = 2.5;
        systemStatusBar.add(cpuLabel, gridBagConstraints);

        memoryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        memoryLabel.setText("Memory:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weightx = 2.5;
        systemStatusBar.add(memoryLabel, gridBagConstraints);

        coresLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        coresLabel.setText("{n cores} ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weightx = 2.5;
        systemStatusBar.add(coresLabel, gridBagConstraints);

		startSystemMonitors(coresLabel,cpuLabel,memoryLabel);
		
		return systemStatusBar;
	}
	
	private void resetConstraints(GridBagLayout layout, GridBagConstraints gc) {
		gc.fill = GridBagConstraints.BOTH;
		gc.gridx = 1;
		gc.gridy = 1;
		gc.weightx = 0.6;
		gc.weighty = 0.35;
		
		layout.setConstraints(resultsFrame, gc);
		
		gc.fill = GridBagConstraints.BOTH;
		gc.gridx = 1;
		gc.gridy = 0;
		gc.weightx = 0.6;
		gc.weighty = 0.65;
		
		layout.setConstraints(graphFrame, gc);
		
		gc.fill = GridBagConstraints.BOTH;
		gc.gridx = 0;
		gc.gridy = 1;
		gc.weightx = 0.4;
		gc.weighty = 0.35;
		
		layout.setConstraints(logFrame, gc);
		
		gc.fill = GridBagConstraints.BOTH;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 0.4;
		gc.weighty = 0.65;
		
		layout.setConstraints(taskManagerFrame, gc);
		
		gc.fill = GridBagConstraints.BOTH;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1.0;
		gc.weighty = 0.65;
		
		layout.setConstraints(previewFrame, gc);
		
		gc.fill = GridBagConstraints.BOTH;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1.0;
		gc.weighty = 0.65;
		
		layout.setConstraints(problemStatementFrame, gc);
		
		gc.fill = GridBagConstraints.BOTH;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1.0;
		gc.weighty = 1.0;
		
		layout.setConstraints(problemStatementFrame, gc);
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
				
				@Override
				public void onNewEntry(LogEntry e) {
					if(TaskManager.getSelectedTask() == task) 
						logTextPane.callUpdate();	
				}
				
			}
			
			);
			
			}
		);
	}
	
	private void showProblemStatementFrame() {	
		problemStatementFrame.update();
		problemStatementFrame.setVisible(true);
		
		globalConstraints.fill = GridBagConstraints.BOTH;
		globalConstraints.gridx = 0;
		globalConstraints.gridy = 1;
		globalConstraints.weightx = 1.0;
		globalConstraints.weighty = 0.35;
		
		globalLayout.setConstraints(graphFrame, globalConstraints);
		
		globalConstraints.fill = GridBagConstraints.BOTH;
		globalConstraints.gridx = 0;
		globalConstraints.gridy = 0;
		globalConstraints.weightx = 1.0;
		globalConstraints.weighty = 0.65;
		
		globalLayout.setConstraints(problemStatementFrame, globalConstraints);
		
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
		
		globalConstraints.fill = GridBagConstraints.BOTH;
		globalConstraints.gridx = 0;
		globalConstraints.gridy = 0;
		globalConstraints.weightx = 1.0;
		globalConstraints.weighty = 1.0;
		
		globalLayout.setConstraints(searchOptionsFrame, globalConstraints);
		
		problemStatementFrame.setVisible(false);
		previewFrame.setVisible(false);				
		resultsFrame.setVisible(false);		
		taskManagerFrame.setVisible(false);
		graphFrame.setVisible(false);
		logFrame.setVisible(false);		
	}
	
	private void startSystemMonitors(JLabel coresLabel, JLabel cpuLabel, JLabel memoryLabel) {		
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
		
}