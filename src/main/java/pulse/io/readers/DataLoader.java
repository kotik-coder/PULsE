package pulse.io.readers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import pulse.input.ExperimentalData;
import pulse.input.InterpolationDataset;
import pulse.input.InterpolationDataset.Type;
import pulse.problem.statements.Problem;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.ui.Messages;

public class DataLoader {

	private static File dir;
		
	private DataLoader() { }
	
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
			JOptionPane.showMessageDialog(null,
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
	
	private static void truncateDataDialog() {								
		Object[] options = {"Truncate", "Do not change"}; 
		int answer = JOptionPane.showOptionDialog(
				null,
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
		
		boolean approve = fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION;
		dir = fileChooser.getCurrentDirectory();
		
		return approve ? Arrays.asList(fileChooser.getSelectedFiles()) : null;
    }
    private static File userInputSingle(String descriptor,  List<String> extensions) {
    	JFileChooser fileChooser = new JFileChooser();
		 
		fileChooser.setCurrentDirectory( directory());		
		fileChooser.setMultiSelectionEnabled(false);

		String[] extArray = extensions.toArray(new String[extensions.size()]);					
		fileChooser.setFileFilter(new FileNameExtensionFilter( descriptor, extArray)); 
		
		boolean approve = fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION;
		dir = fileChooser.getCurrentDirectory();
		
		return approve ? fileChooser.getSelectedFile() : null;
    }
    private static File userInputSingle(String descriptor, String... extensions) {
    	return userInputSingle(descriptor, Arrays.asList(extensions));				
    }
    
	private static File directory() {
		if(dir != null)
			return dir;
		else
			try {
				return new File(DataLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			} catch (URISyntaxException e) {
				System.err.println("Cannot determine current working directory.");
				e.printStackTrace();
			}
		return null;
	}
	
	/**
	 * Uses the {@code ReaderManager} to create an {@code InterpolationDataset} from {@code f} 
	 * and updates the thermal properties of each task.
	 * @param f a {@code File} containing the specific heat (or the heat capacity) data [J/kg/K].
	 * @throws IOException if file cannot be read
	 * @see pulse.tasks.SearchTask.calculateThermalProperties()
	 */
	
	public static void load(Type type, File f) throws IOException {
		Objects.requireNonNull(f);
		
		switch(type) {
			case SPECIFIC_HEAT : InterpolationDataset.setSpecificHeatData( ReaderManager.readDataset(f) ); break; 
			case DENSITY : InterpolationDataset.setDensityData(ReaderManager.readDataset(f)); break;
			default : throw new IllegalArgumentException("Unrecognized data type: " + type);
		}
		TaskManager.evaluate();
	}
	
}