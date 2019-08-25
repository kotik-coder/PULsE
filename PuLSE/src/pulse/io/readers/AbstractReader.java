package pulse.io.readers;

import java.io.File;

import pulse.util.Reflexive;

public interface AbstractReader extends Reflexive {
	
	public String getSupportedExtension();
	
	public static boolean checkExtensionSupported(File file, String extension) {
		if(file.isDirectory())
			return false;
		
		String name = file.getName();  

		/**
		 * String helper functions.
		 *
		 * @author Gili Tzabari
		 */
		
		int suffixLength = extension.length();
        return name.regionMatches(true, name.length() - suffixLength, 
        		extension, 0, suffixLength);
		
	}
	
	public default boolean isExtensionSupported(File file) {
		return checkExtensionSupported(file, getSupportedExtension());		
	}		
	
}
