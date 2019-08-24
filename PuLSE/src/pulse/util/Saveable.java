package pulse.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

public interface Saveable extends Describable {
	
	public default Extension getDefaultExportExtension() {
		return Extension.HTML;
	}
	
	public default Extension[] getSupportedExtensions() {
		return new Extension[] {getDefaultExportExtension()};
	}
	
	public default void saveNow(File directory) {

		try {
				File newFile = new File(directory, describe() + "." + getDefaultExportExtension());
				newFile.createNewFile();
	            FileOutputStream fos = new FileOutputStream(newFile);
	            printData(fos, getDefaultExportExtension());
	            
		} catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
		}
		
	}
	
	public default void askToSave(JFrame parentWindow, String fileTypeLabel) {
		JFileChooser fileChooser = new JFileChooser();
		
		File workingDirectory = new File(System.getProperty("user.home"));
		fileChooser.setCurrentDirectory(workingDirectory);
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setSelectedFile(new File(describe()));
		
		for(Extension s : getSupportedExtensions())
			fileChooser.addChoosableFileFilter(
					new FileNameExtensionFilter(fileTypeLabel + " (." + s + ")", 
					s.toString().toLowerCase()));
		
		fileChooser.setAcceptAllFileFilterUsed(false);
		
	    int returnVal = fileChooser.showSaveDialog(parentWindow);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	        try {
	            File file = fileChooser.getSelectedFile();
	            String path = file.getPath();	            
	            
	            FileNameExtensionFilter currentFilter = (FileNameExtensionFilter)fileChooser.getFileFilter();
	            String ext = currentFilter.getExtensions()[0];
	            
	            if(!path.contains(".")) 	            
	            	file = new File(path + "." + ext);	             
	            
	            FileOutputStream fos = new FileOutputStream(file);
	            
	            printData(fos, Extension.valueOf(ext.toUpperCase()));
	            
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	    }
	}
	
	public void printData(FileOutputStream fos, Extension extension);
	
	public enum Extension {
		HTML, CSV;
		
		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}
		
	}
	
}
