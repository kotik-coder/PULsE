package pulse.io.readers;

import java.io.File;

import pulse.util.Reflexive;

/**
 * Basic interface for readers in {@code PULsE}. <p> The only functionality 
 * this interfaces offers is to check if a certain files conforms to the 
 * extension requirement. All file readers used in {@code PULsE} should 
 * implement this interface.</p>
 */

public interface AbstractReader extends Reflexive {
	
	/**
	 * Returns the supported extension of files, which this {@code AbstractReader} is able to process.
	 * @return a {@code String} (usually, lower-case) containing the supported extension.
	 */
	
	public String getSupportedExtension();
	
	/**
	 * Checks if the file suffix for {@code file} matches the {@code extension}.
	 * @param file the {@code File} to process
	 * @param extension a String, which needs to be checked against the suffix of {@code File} 
	 * @return {@code false} if {@code file} is a directory or if it has a suffix different from {@code extension}. True otherwise.
	 */
	
	public static boolean extensionsMatch(File file, String extension) {
		if(file.isDirectory())
			return false;
		
		String name = file.getName();  

		/*
		 * The below code is based on string helper function by Gili Tzabari
		 */
		
		int suffixLength = extension.length();
        return name.regionMatches(true, name.length() - suffixLength, 
        		extension, 0, suffixLength);
		
	}
	
	/**
	 * Invokes {@code extensionMatch} with the second argument set as {@code getSupportedExtension()}. 
	 * @param file the file to be checked
	 * @return {@code true} if extensions match, false otherwise.
	 * @see extensionsMatch 
	 * @see getSupportedExtension
	 */
	
	public default boolean isExtensionSupported(File file) {
		return extensionsMatch(file, getSupportedExtension());		
	}		
	
}
