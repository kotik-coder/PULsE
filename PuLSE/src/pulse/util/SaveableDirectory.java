package pulse.util;

import java.io.File;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

public interface SaveableDirectory extends Describable {	

	public List<Saveable> saveableContents();
	
	public default void askToSave(JFrame parent) {

		JFileChooser fileChooser = new JFileChooser();
		
		File workingDirectory = new File(System.getProperty("user.home"));
		fileChooser.setCurrentDirectory(workingDirectory);
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
	    int returnVal = fileChooser.showSaveDialog(parent);
	    
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
			File newDirectory = new File(
					fileChooser.getSelectedFile(),
					describe() + File.separator);
			newDirectory.mkdirs();

			for(Saveable saveable : saveableContents()) 
				saveable.saveNow(newDirectory);
			
	    }
		
		
	}
	
	public default void saveNow(JFrame parent, File parentDirectory) {
		
		File newDirectory = new File(
				parentDirectory, describe() + File.separator);
		newDirectory.mkdirs();
		
		saveableContents().parallelStream().forEach(s -> s.saveNow(newDirectory));				
					
	}
	

	public static void askToSave(JFrame parent, 
			String descriptor, 
			List<? extends SaveableDirectory> directories) {

		JFileChooser fileChooser = new JFileChooser();
		
		File workingDirectory = new File(System.getProperty("user.home"));
		fileChooser.setCurrentDirectory(workingDirectory);
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
	    int returnVal = fileChooser.showSaveDialog(parent);
	    
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
			File newDirectory = new File(
					fileChooser.getSelectedFile(),
					descriptor + File.separator);
			newDirectory.mkdirs();
					
			for(SaveableDirectory directory : directories) 
				directory.saveNow(parent, newDirectory);		
			
	    }
		
	}		

}
