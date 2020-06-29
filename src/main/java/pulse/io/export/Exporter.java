package pulse.io.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import pulse.util.Descriptive;
import pulse.util.Reflexive;

/**
 * A {@code Saveable} is any individual {@code PULsE} entity that can be saved
 * using a {@code FileOutputStream}.
 *
 */

public interface Exporter<T extends Descriptive> extends Reflexive {

	/**
	 * Gets the default export extension for this {@code Saveable}.
	 * 
	 * @return {@code .html}, if not stated otherwise by overriding this method.
	 */

	public static Extension getDefaultExportExtension() {
		return Extension.CSV;
	}

	public static Extension[] getAllSupportedExtensions() {
		return Extension.values();
	}

	/**
	 * Returns an array of supported extensions, which by default contains only the
	 * default extension.
	 * 
	 * @return an array with {@code Extension} type objects.
	 */

	public default Extension[] getSupportedExtensions() {
		return new Extension[] { getDefaultExportExtension() };
	}

	/**
	 * Saves the contents to {@code directory} without asking a confirmation from
	 * the user.
	 * 
	 * @param directory the directory where this {@code Saveable} needs to be saved.
	 */

	public default void export(T target, File directory, Extension extension) {

		var supportedExtension = extension;

		if (!Arrays.stream(getSupportedExtensions()).anyMatch(extension::equals))
			supportedExtension = getDefaultExportExtension(); // revert to default extension

		try {
			var newFile = new File(directory, target.describe() + "." + supportedExtension);
			newFile.createNewFile();
			var fos = new FileOutputStream(newFile);
			printToStream(target, fos, supportedExtension);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Provides a {@code JFileChooser} for the user to select the export destiation
	 * for this {@code Saveable}.
	 * 
	 * @param parentWindow  the parent frame.
	 * @param fileTypeLabel the label describing the specific type of files that
	 *                      will be saved.
	 */

	public default void askToExport(T target, JFrame parentWindow, String fileTypeLabel) {
		var fileChooser = new JFileChooser();
		var workingDirectory = new File(System.getProperty("user.home"));
		fileChooser.setCurrentDirectory(workingDirectory);
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setSelectedFile(new File(target.describe()));

		for (var s : getSupportedExtensions()) {
                    fileChooser.addChoosableFileFilter(
                            new FileNameExtensionFilter(fileTypeLabel + " (." + s + ")", s.toString().toLowerCase()));
                }

		fileChooser.setAcceptAllFileFilterUsed(false);

		int returnVal = fileChooser.showSaveDialog(parentWindow);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				var file = fileChooser.getSelectedFile();
				var path = file.getPath();
				var currentFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();
				var ext = currentFilter.getExtensions()[0];

				if (!path.contains("."))
					file = new File(path + "." + ext);

				var fos = new FileOutputStream(file);

				printToStream(target, fos, Extension.valueOf(ext.toUpperCase()));

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public Class<T> target();

	public void printToStream(T target, FileOutputStream fos, Extension extension);

}