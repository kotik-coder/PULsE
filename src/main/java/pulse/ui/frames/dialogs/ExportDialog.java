package pulse.ui.frames.dialogs;

import java.awt.Dimension;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.input.Metadata;
import pulse.io.export.ExportManager;
import pulse.io.export.Exporter;
import pulse.io.export.Extension;
import pulse.io.export.HeatingCurveExporter;
import pulse.io.export.LogExporter;
import pulse.io.export.MassExporter;
import pulse.io.export.MetadataExporter;
import pulse.io.export.RawDataExporter;
import pulse.io.export.ResidualStatisticExporter;
import pulse.io.export.ResultExporter;
import pulse.tasks.Log;
import pulse.tasks.Result;
import pulse.tasks.TaskManager;
import pulse.ui.Launcher;

@SuppressWarnings("serial")
public class ExportDialog extends JDialog {

	private static Map<Class<?>,Boolean> exportSettings = new HashMap<Class<?>,Boolean>();
	private final static int HEIGHT = 160;
	private final static int WIDTH = 650;
	
	private static ProgressDialog progressFrame = new ProgressDialog();
	
	static {
		progressFrame.setLocationRelativeTo(null);
		progressFrame.setAlwaysOnTop(true);
	}
	
	static {
		exportSettings.put(MetadataExporter.getInstance().target(), false);
		exportSettings.put(HeatingCurveExporter.getInstance().target(), true);
		exportSettings.put(ResidualStatisticExporter.getInstance().target(), true);
		exportSettings.put(RawDataExporter.getInstance().target(), true);
		exportSettings.put(ResultExporter.getInstance().target(), true);
		exportSettings.put(LogExporter.getInstance().target(), false);
	}
	private boolean createSubdirectories = false;
	
	private File dir;
	
	private JFileChooser fileChooser;
	
	private String projectName;
	
	public ExportDialog() {
		initComponents();
		setTitle("Export Dialog");
		setSize(new Dimension(WIDTH, HEIGHT));
	}
	
	private File directoryQuery() {
	    int returnVal = fileChooser.showSaveDialog(this);
	    
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
			dir = fileChooser.getSelectedFile();
			return dir;
	    }
	    
	    return null;
	    
	}
	
	private void export(Extension extension) {
			if(TaskManager.numberOfTasks() < 1)
				return; //nothing to export		
		
			var destination = new File(dir + File.separator + projectName);
			var subdirs	= TaskManager.getTaskList();

			if(subdirs.size() > 0 && !destination.exists())
				destination.mkdirs();
			
			final int threads = Launcher.threadsAvailable();
			
			if(createSubdirectories) {
				progressFrame.trackProgress(subdirs.size());
				var pool = Executors.newFixedThreadPool(threads - 1);
				subdirs.stream().forEach(s -> {
						pool.submit( () -> 
							{ MassExporter.exportGroup(s,destination,extension);
							 progressFrame.incrementProgress();
							} 
							);
				} );
			}
			else {				
				var groupped = ExportManager.allGrouppedContents();
				var pool = Executors.newFixedThreadPool(threads - 1);
				progressFrame.trackProgress(groupped.size());
				
				groupped.stream().forEach(
									individual -> pool.submit( () -> {
										Class<?> individualClass = individual.getClass();
										
										if(!exportSettings.containsKey(individualClass)) {
											
											var key = exportSettings.keySet().stream().filter(aClass -> 
														aClass.isAssignableFrom(individual.getClass())).findFirst();											
											
											if(key.isPresent())
												individualClass = key.get();
																																	
										}
										
										if(individualClass != null) {
											if(exportSettings.containsKey(individualClass))
												if(exportSettings.get(individualClass))
													ExportManager.export(individual, destination, extension);
										}

										progressFrame.incrementProgress();
										
								})
									
						);
			}
			
			if(exportSettings.get(Result.class))
				ExportManager.exportAllResults(destination, extension);
			
	}
	
	private void initComponents() {
		
		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		
		final String defaultProjectName = TaskManager.getInstance().describe();		
		projectName = defaultProjectName;
		
		var directoryLabel = new JLabel("Export to:");
		
		fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    	//Checkboxex
		dir = fileChooser.getCurrentDirectory();
		
		var directoryField = new JTextField(dir.getPath() + File.separator + projectName + File.separator);
		directoryField.setEditable(false);

		var formatLabel = new JLabel("Export format:");
		var formats = new JComboBox<Extension>( Exporter.getAllSupportedExtensions() );
		
		var projectLabel = new JLabel("Project name:");
		var projectText = new JTextField(projectName);
		
		projectText.getDocument().addDocumentListener(new DocumentListener() {
			  @Override
			public void changedUpdate(DocumentEvent e) {
			    	//
				  }
		      @Override
			public void insertUpdate(DocumentEvent e) {
				  	if(projectText.getText().trim().isEmpty())
				  		return;
			    	projectName = projectText.getText();
			    	directoryField.setText(dir.getPath() + File.separator + projectName + File.separator);
			 }
			  @Override
			public void removeUpdate(DocumentEvent e) {
				    if(projectText.getText().trim().isEmpty()) {
				    	projectName = TaskManager.getInstance().describe();
				    	directoryField.setText(dir.getPath() + File.separator + projectName + File.separator);
				    } else {
				    	projectName = projectText.getText();
				    	directoryField.setText(dir.getPath() + File.separator + projectName + File.separator);
				    }
				  }
		});
		
		var solutionCheckbox = new JCheckBox("Export Solution(s)"); 
		solutionCheckbox.setSelected(exportSettings.get(HeatingCurve.class));
		solutionCheckbox.addActionListener(e -> {
			exportSettings.put(HeatingCurve.class, solutionCheckbox.isSelected());
			exportSettings.put(ResidualStatisticExporter.class, solutionCheckbox.isSelected());
		});
		
		var rawDataCheckbox = new JCheckBox("Export Raw Data"); 
		rawDataCheckbox.setSelected(exportSettings.get(ExperimentalData.class));
		rawDataCheckbox.addActionListener(e -> exportSettings.put(ExperimentalData.class, rawDataCheckbox.isSelected()));
		
		var metadataCheckbox = new JCheckBox("Export Metadata");
		metadataCheckbox.setSelected(exportSettings.get(Metadata.class));
		metadataCheckbox.addActionListener(e -> exportSettings.put(Metadata.class, metadataCheckbox.isSelected()));
		
		var createDirCheckbox = new JCheckBox("Create Sub-Directories");
		createDirCheckbox.setSelected(createSubdirectories);
		
		var logCheckbox = new JCheckBox("Export log(s)");
		logCheckbox.setSelected(exportSettings.get(Log.class));
		logCheckbox.addActionListener(e -> exportSettings.put(Log.class, logCheckbox.isSelected()));
		
		createDirCheckbox.addActionListener(e -> { 
		metadataCheckbox.setEnabled(!createDirCheckbox.isSelected());
		rawDataCheckbox.setEnabled(!createDirCheckbox.isSelected());
		solutionCheckbox.setEnabled(!createDirCheckbox.isSelected());
		logCheckbox.setEnabled(!createDirCheckbox.isSelected());
		createSubdirectories = createDirCheckbox.isSelected();
		});
		
		var resultsCheckbox = new JCheckBox("Export Results");
		resultsCheckbox.setSelected(exportSettings.get(Result.class));
		resultsCheckbox.addActionListener(e -> exportSettings.put(Result.class, resultsCheckbox.isSelected()));
		
		var browseBtn = new JButton("Browse...");
		
		browseBtn.addActionListener(e -> {
				if(directoryQuery() != null) 
					directoryField.setText(dir.getPath() + File.separator + projectName + File.separator);
			}
		);
		
		var exportBtn = new JButton("Export");
		
		exportBtn.addActionListener(e -> 
			java.awt.EventQueue.invokeLater(() -> export(Extension.valueOf(
					formats.getSelectedItem().toString().toUpperCase())))
		);
		
		/*
		 * layout
		 */
		
		layout.setHorizontalGroup(layout.createSequentialGroup()
		    // #1
			.addComponent(directoryLabel)
		    // #2
		    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
		        .addComponent(directoryField)
		        // #2a
			    .addGroup(layout.createSequentialGroup() 
		            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
		                .addComponent(solutionCheckbox)
		                .addComponent(rawDataCheckbox)
		            		)
		            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
		                .addComponent(metadataCheckbox)
		                .addComponent(createDirCheckbox)
		                )
		            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					    .addComponent(logCheckbox)
			            .addComponent(resultsCheckbox)
			            )
		    		)
			    // #2b
			    //.addGroup(layout.createSequentialGroup()
				    	.addGroup(layout.createSequentialGroup()
								.addComponent(formatLabel)
				    			.addComponent(formats)	
								.addComponent(projectLabel)
								.addComponent(projectText)
							)
			    	//	)
			    )
		    // #3
			    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			        .addComponent(browseBtn)
			        .addComponent(exportBtn)
			        )
		);
		layout.linkSize(SwingConstants.HORIZONTAL, browseBtn, exportBtn);
	
		layout.setVerticalGroup(layout.createSequentialGroup()
			//#1
		    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
		        .addComponent(directoryLabel)
		        
		        .addComponent(directoryField)
		        .addComponent(browseBtn))
		    //#2
		    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
		        //#2a
		    	.addGroup(layout.createSequentialGroup()
		        	.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
			    		.addComponent(solutionCheckbox)
			        	.addComponent(metadataCheckbox)
	    				.addComponent(logCheckbox)
		        			)
				 	.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
			        	.addComponent(rawDataCheckbox)
			        	.addComponent(createDirCheckbox)
			        	.addComponent(resultsCheckbox) 
				 			)
				 	)
		    	//#2b
		        .addComponent(exportBtn)
		        )
		    //2b
		    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(formats)
		    		.addComponent(formatLabel)	
	    			.addComponent(projectLabel)
	    			.addComponent(projectText)
					 )
		);
		
	}
	
}