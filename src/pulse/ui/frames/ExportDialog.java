package pulse.ui.frames;

import java.awt.Dimension;
import java.io.File;

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
import pulse.tasks.Log;
import pulse.tasks.TaskManager;
import pulse.util.Extension;
import pulse.util.Saveable;

@SuppressWarnings("serial")
public class ExportDialog extends JDialog {

	private GroupLayout layout;
	private File dir;
	private String projectName;
	private JFileChooser fileChooser;
	
	private final static int WIDTH = 650;
	private final static int HEIGHT = 160;
	
	private boolean createSubdirectories = false;
	private boolean exportMetadata		 = false;
	private boolean exportRawData		 = true;
	private boolean exportSolutions		 = true;
	private boolean exportResults		 = true;
	private boolean exportLogs			 = false;
	
	public ExportDialog() {
		initComponents();
		setTitle("Export Dialog");
		setSize(new Dimension(WIDTH, HEIGHT));
	}
	
	private void initComponents() {
		
		layout = new GroupLayout(getContentPane());
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
		var formats = new JComboBox<Extension>( Saveable.getAllSupportedExtensions() );
		
		var projectLabel = new JLabel("Project name:");
		var projectText = new JTextField(projectName);
		
		projectText.getDocument().addDocumentListener(new DocumentListener() {
			  @Override
			public void changedUpdate(DocumentEvent e) {
			    	//
				  }
		      @Override
			public void removeUpdate(DocumentEvent e) {
				    if(projectText.getText().trim().isEmpty()) {
				    	projectName = defaultProjectName;
				    	directoryField.setText(dir.getPath() + File.separator + projectName + File.separator);
				    } else {
				    	projectName = projectText.getText();
				    	directoryField.setText(dir.getPath() + File.separator + projectName + File.separator);
				    }
				  }
			  @Override
			public void insertUpdate(DocumentEvent e) {
				  	if(projectText.getText().trim().isEmpty())
				  		return;
			    	projectName = projectText.getText();
			    	directoryField.setText(dir.getPath() + File.separator + projectName + File.separator);
			 }
		});
		
		var solutionCheckbox = new JCheckBox("Export Solution(s)"); 
		solutionCheckbox.setSelected(exportSolutions);
		solutionCheckbox.addActionListener(e -> exportSolutions = solutionCheckbox.isSelected());
		
		var rawDataCheckbox = new JCheckBox("Export Raw Data"); 
		rawDataCheckbox.setSelected(exportRawData);
		rawDataCheckbox.addActionListener(e -> exportRawData = rawDataCheckbox.isSelected());
		
		var metadataCheckbox = new JCheckBox("Export Metadata");
		metadataCheckbox.setSelected(exportMetadata);
		metadataCheckbox.addActionListener(e -> exportMetadata = metadataCheckbox.isSelected());
		
		var createDirCheckbox = new JCheckBox("Create Sub-Directories");
		createDirCheckbox.setSelected(createSubdirectories);
		
		var logCheckbox = new JCheckBox("Export log(s)");
		logCheckbox.setSelected(exportLogs);
		logCheckbox.addActionListener(e -> { 
			exportLogs = logCheckbox.isSelected();
			});
		
		createDirCheckbox.addActionListener(e -> { 
		metadataCheckbox.setEnabled(!createDirCheckbox.isSelected());
		rawDataCheckbox.setEnabled(!createDirCheckbox.isSelected());
		solutionCheckbox.setEnabled(!createDirCheckbox.isSelected());
		logCheckbox.setEnabled(!createDirCheckbox.isSelected());
		createSubdirectories = createDirCheckbox.isSelected();
		});
		
		var resultsCheckbox = new JCheckBox("Export Results");
		resultsCheckbox.setSelected(exportResults);
		resultsCheckbox.addActionListener(e -> { 
			exportResults = resultsCheckbox.isSelected();
			});
		
		
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
	
	private void export(Extension extension) {
			
			var destination = new File(dir + File.separator + projectName);
			var subdirs	= TaskManager.contents(); 
			var results = TaskManager.saveableResults();

			if(subdirs.size() > 0 )
				if(!destination.exists())
					destination.mkdirs();
			
			if(createSubdirectories) 
				subdirs.stream().forEach(s -> s.saveCategory(destination,extension));
			else {
				subdirs.stream().forEach(
						directory -> 
							directory.contents().stream().forEach(
									individual -> {
										if(individual instanceof Metadata) {
											if(exportMetadata)
												individual.save(destination, extension);
										}
										else if(individual instanceof ExperimentalData) {
											if(exportRawData)
												individual.save(destination, extension);
										}
										else if(individual instanceof HeatingCurve) {
											if(exportSolutions) 
												individual.save(destination, extension);
										}
										else if(individual instanceof Log) {
											if(exportLogs) 
												individual.save(destination, extension);
										}
									})
						);
			}
			
			results.stream().forEach(r -> 
				{
				if(exportResults) 
					r.save(destination, extension);
				}
			);
			
	}
	
	private File directoryQuery() {
	    int returnVal = fileChooser.showSaveDialog(this);
	    
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
			dir = fileChooser.getSelectedFile();
			return dir;
	    }
	    
	    return null;
	    
	}
	
}