package pulse.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * A {@code Saveable} is any individual {@code PULsE} entity that can be saved using a {@code FileOutputStream}.
 *
 */

public interface Saveable extends Describable {
	
	/**
	 * Gets the default export extension for this {@code Saveable}.
	 * @return {@code .html}, if not stated otherwise by overriding this method.
	 */
	
	public default Extension getDefaultExportExtension() {
		return Extension.HTML;
	}
	
	/**
	 * Returns an array of supported extensions, which by default contains only the default extension.
	 * @return an array with {@code Extension} type objects. 
	 */
	
	public default Extension[] getSupportedExtensions() {
		return new Extension[] {getDefaultExportExtension()};
	}
	
	/**
	 * Saves the contents to {@code directory} without asking a confirmation from the user. 
	 * @param directory the directory where this {@code Saveable} needs to be saved.
	 */
	
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
	
	/**
	 * Provides a {@code JFileChooser} for the user to select the export destiation for this {@code Saveable}.
	 * @param parentWindow the parent frame.
	 * @param fileTypeLabel the label describing the specific type of files that will be saved.
	 */
	
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
		
		/**
		 * The result will be an html-document with tags that can be opened with any web browser.
		 * Useful for complex formatting, but not for data manipulation, as it contains tags
		 * and special symbols.
		 */
		
		HTML, 
		
		/**
		 * The result will be a tab-delimited CSV document. Usefult for data manipulations and plotting.
		 */
		
		CSV;
		
		/**
		 * This will return the lower-case characters for the extension. 
		 */
		
		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}
		
	}
	
}