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
 * An {@code Exporter} defines a set of rules to enable exporting of a certain
 * type of PULsE objects (typically, instances of the PropertyHolder class).
 *
 */
public interface Exporter<T extends Descriptive> extends Reflexive {

    /**
     * Gets the default export extension.
     *
     * @return {@code Extension.CSV} by default
     */
    public static Extension getDefaultExportExtension() {
        return Extension.CSV;
    }

    /**
     * Returns an array of supported extensions, which by default contains only
     * the default extension.
     *
     * @return an array with {@code Extension} type objects.
     */
    public default Extension[] getSupportedExtensions() {
        return new Extension[]{getDefaultExportExtension()};
    }

    /**
     * Exports the available contents to {@code directory} without asking a
     * confirmation from the user.
     * <p>
     * A file is created with the name specified by the {@code describe()}
     * method of {@code target} with the extension equal to the third argument
     * of this method. A {@code FileOutputStream} writes the contents to the
     * file by invoking {@code printToStream} and is closed upon completion.
     * </p>
     *
     * @param directory the directory where the contents will be exported to.
     * @param target a {@code Descriptive} target
     * @param extension the file extension. If it is not supported, the exporter
     * will revert to its default extension
     * @throws IllegalArgumentException if {@code directory} is not really a
     * directory
     * @see printToStream
     */
    public default void export(T target, File directory, Extension extension) {

        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }

        var supportedExtension = extension;

        if (!Arrays.stream(getSupportedExtensions()).anyMatch(extension::equals)) {
            supportedExtension = getDefaultExportExtension(); // revert to default extension
        }
        try {
            var newFile = new File(directory, target.describe() + "." + supportedExtension);
            newFile.createNewFile();
            var fos = new FileOutputStream(newFile);
            printToStream(target, fos, supportedExtension);
            fos.close();
        } catch (IOException e) {
            System.err.println("An exception has been encountered while writing the contents of "
                    + target.getClass().getSimpleName() + " to " + directory);
            e.printStackTrace();
        }

    }

    /**
     * Provides a {@code JFileChooser} for the user to select the export
     * destination for {@code target}. The name of the file and its extension
     * come from the selection the user makes by interacting with the dialog.
     *
     * @param target the exported target
     * @param parentWindow the parent frame.
     * @param fileTypeLabel the label describing the specific type of files that
     * @param directory the default directory of the file will be saved.
     * @return the directory where files were exported
     */
    public default File askToExport(T target, JFrame parentWindow, String fileTypeLabel, File directory) {
        var fileChooser = new JFileChooser(directory);
        fileChooser.setMultiSelectionEnabled(true);

        FileNameExtensionFilter choosable = null;

        for (var s : getSupportedExtensions()) {
            choosable = new FileNameExtensionFilter(fileTypeLabel + " (." + s + ")", s.toString().toLowerCase());
            fileChooser.addChoosableFileFilter(choosable);
        }

        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(choosable);
        fileChooser.setSelectedFile(new File(target.describe() + "." + choosable.getExtensions()[0]));

        int returnVal = fileChooser.showSaveDialog(parentWindow);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            var file = fileChooser.getSelectedFile();
            var path = file.getPath();

            directory = file.isDirectory() ? file : file.getParentFile();

            if ((fileChooser.getFileFilter() instanceof FileNameExtensionFilter)) {

                var currentFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();
                var ext = currentFilter.getExtensions()[0];

                if (!path.contains(".")) {
                    file = new File(path + "." + ext);
                } else {
                    file = new File(path.substring(0, path.indexOf(".") + 1) + ext);
                }

                try {
                    var fos = new FileOutputStream(file);
                    printToStream(target, fos, Extension.valueOf(ext.toUpperCase()));
                    fos.close();
                } catch (IOException e) {
                    System.err.println("An exception has been encountered while writing the contents of "
                            + target.getClass().getSimpleName() + " to " + file);
                    e.printStackTrace();
                }

            }

        }

        return directory;

    }

    /**
     * Defines the class, instances of which can be fed into the exporter to
     * produce a result.
     *
     * @return a class implementing the {@code Descriptive} interface.
     */
    public Class<T> target();

    /**
     * The interface method is implemented by the subclasses to define the
     * exportable content in detail. Depending on the supported extensions, this
     * will typically involve a switch statement that will invoke private
     * methods defined in the subclass handling the different choices. The
     * stream {@code fos} is usually closed implicitly in a try-with-resource
     * clause.
     *
     * @param target the exported target
     * @param fos a FileOutputStream created by the {@code export} method
     * @param extension an extension of the file saved on disk
     * @see export
     * @see askToExport
     */
    public void printToStream(T target, FileOutputStream fos, Extension extension);

}
