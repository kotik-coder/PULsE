package pulse.io.readers;

import java.io.File;

import pulse.util.Reflexive;

/**
 * There are two types of {@code AbstractHandler}s, which are used to either 
 * update/populate existing objects or convert data into new objects of a given type. The superclass
 * contains basic methods of checking compliance to a pre-set extension.
 *
 */

public interface AbstractHandler extends Reflexive {

	/**
	 * Retrieves the supported extension of files, which this
	 * {@code AbstractHandler} is able to process.
	 * 
	 * @return a {@code String} (usually, lower-case) containing the supported
	 *         extension.
	 */

	public String getSupportedExtension();

	/**
	 * Checks if the file suffix for {@code file} matches the {@code extension}.
	 * 
	 * @param file      the {@code File} to process
	 * @param extension a String, which needs to be checked against the suffix of
	 *                  {@code File}
	 * @return {@code false} if {@code file} is a directory or if it has a suffix
	 *         different from {@code extension}. True otherwise.
	 */

	public static boolean extensionsMatch(File file, String extension) {
		if (file.isDirectory())
			return false;

		String name = file.getName();

		/*
		 * The below code is based on string helper function by Gili Tzabari
		 */

		int suffixLength = extension.length();
		return name.regionMatches(true, name.length() - suffixLength, extension, 0, suffixLength);

	}

	/**
	 * Invokes {@code extensionMatch} with the second argument set as
	 * {@code getSupportedExtension()}.
	 * 
	 * @param file the file to be checked
	 * @return {@code true} if extensions match, false otherwise.
	 * @see extensionsMatch
	 * @see getSupportedExtension
	 */

	public default boolean isExtensionSupported(File file) {
		return extensionsMatch(file, getSupportedExtension());
	}

}