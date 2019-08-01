package pulse.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

public interface Saveable extends Describable {
	
	public default String getExportExtension() {
		return ".html";
	}
	
	public default void saveNow(File directory) {

		try {
				File newFile = new File(directory, describe() + getExportExtension());
				newFile.createNewFile();
	            FileOutputStream fos = new FileOutputStream(newFile);
	            printData(fos);
	            
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
		fileChooser.setSelectedFile(new File(describe() + ".html"));
		fileChooser.setFileFilter(new FileNameExtensionFilter(fileTypeLabel,".html"));
		 
	    int returnVal = fileChooser.showSaveDialog(parentWindow);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	        try {
	            File file = fileChooser.getSelectedFile();
	            FileOutputStream fos = new FileOutputStream(file);
	            
	            printData(fos);
	            
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	    }
	}
	
	public void printData(FileOutputStream fos);
	
}
