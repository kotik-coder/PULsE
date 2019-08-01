package pulse.io.readers;

import java.io.File;

import pulse.util.Reflexive;

public interface AbstractReader extends Reflexive {
	
	public String getSupportedExtension();
	
	public static boolean checkExtensionSupported(File file, String extension) {
		if(file.isDirectory())
			return false;
		
		String name = file.getName();  

		if( endsWithIgnoreCase(name, extension) )
			return true;
		
		return false;		
	}
	
	public default boolean isExtensionSupported(File file) {
		return checkExtensionSupported(file, getSupportedExtension());		
	}		
	
	/**
	 * String helper functions.
	 *
	 * @author Gili Tzabari
	 */
	
    private static boolean endsWithIgnoreCase(String str, String suffix)
    {
        int suffixLength = suffix.length();
        return str.regionMatches(true, str.length() - suffixLength, suffix, 0, suffixLength);
    }
	
}
