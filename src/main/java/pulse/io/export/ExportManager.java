package pulse.io.export;

import static java.io.File.separator;
import static java.util.Arrays.asList;
import static pulse.io.export.Exporter.getDefaultExportExtension;
import static pulse.util.Group.contents;
import static pulse.util.Reflexive.instancesOf;

import java.io.File;
import java.util.Objects;

import javax.swing.JFrame;

import pulse.tasks.TaskManager;
import pulse.util.Descriptive;
import pulse.util.Group;

/**
 * Manages export operations. Provides tools to find a specific exporter
 * suitable for a given target and shortcuts for export operations.
 *
 */

public class ExportManager {

	private ExportManager() {
		// intentionally blank
	}

	/**
	 * Finds a suitable exporter for a non-null {@code target} by calling
	 * {@code findExporter(target.getClass())}.
	 * 
	 * @param <T>    an instance of {@code Descriptive}
	 * @param target the exported target
	 * @return an exporter that works for {@code target}
	 * @see findExporter
	 */

	@SuppressWarnings("unchecked")
	public static <T extends Descriptive> Exporter<T> findExporter(T target) {
		Objects.requireNonNull(target);
		return (Exporter<T>) findExporter(target.getClass());
	}

	/**
	 * Finds an exporter that can work with {@code target}.
	 * <p>
	 * Searches through available instances of the Exporter class contained in this
	 * package and checks if any of those have their target set to the argument of
	 * this method, return the first occurrence. If nothing matches exactly the same
	 * class as specified, searches for exporters of any classes assignable from
	 * {@code target}.
	 * </p>
	 * 
	 * @param <T>    an instance of {@code Descriptive}
	 * @param target the target glass
	 * @return an intancce of the Exporter class that can work worth the type T,
	 *         null if nothing has been found
	 */

	@SuppressWarnings({ "unchecked" })
	public static <T extends Descriptive> Exporter<T> findExporter(Class<T> target) {
		var allExporters = instancesOf(Exporter.class);
		var exporter = allExporters.stream().filter(e -> e.target() == target).findFirst();

		if (exporter.isPresent())
			return exporter.get();
		else {
			exporter = allExporters.stream().filter(e -> e.target().isAssignableFrom(target)).findFirst();
			return exporter.isPresent() ? exporter.get() : null;
		}
	}

	/**
	 * Finds an exporter matching to {@code target} and allows the user to select
	 * the location of export.
	 * 
	 * @param <T>           a {@code Descriptive} type
	 * @param target        the target to be exported
	 * @param parentWindow  a frame to which the file chooser dialog will be
	 *                      attached
	 * @param fileTypeLabel a brief description of the exported file types
	 * @see findExporter
	 * @see pulse.io.export.Exporter.askToExport()
	 * @throws IllegalArgumentException if no exporter can be found
	 */

	public static <T extends Descriptive> void askToExport(T target, JFrame parentWindow, String fileTypeLabel) {
		var exporter = findExporter(target);
		if (exporter != null)
			exporter.askToExport(target, parentWindow, fileTypeLabel);
		else
			throw new IllegalArgumentException("No exporter for " + target.getClass().getSimpleName());
	}

	/**
	 * Attempts to export the given {@code target} to the {@code directory} by
	 * saving the contents in a file with the given {@code Extension}.
	 * <p>
	 * The file is formatted according to the inherent format, i.e. if it is an
	 * {@code Extension.HTML} file, it will contain HTML tags, etc. If
	 * {@code extension} is not present in the list of supported extension of an
	 * exporter matching {@code target}, this will revert to the first supported
	 * extension. This method will not have any result if no exporter has been found
	 * fot {@code target}.
	 * </p>
	 * 
	 * @param <T>       the target type
	 * @param target    the exported target
	 * @param directory a pre-selected directory
	 * @param extension the desired extension
	 */

	public static <T extends Descriptive> void export(T target, File directory, Extension extension) {
		var exporter = findExporter(target);

		if (exporter != null) {
			var supportedExtensions = exporter.getSupportedExtensions();

			if (supportedExtensions.length > 0) {
				var confirmedExtension = asList(supportedExtensions).contains(extension) ? extension
						: supportedExtensions[0];
				exporter.export(target, directory, confirmedExtension);
			}

		}
	}

	/**
	 * This will invoke {@code exportGroup} on each task listed by the
	 * {@code TaskManager}.
	 * 
	 * @param directory a pre-selected directory
	 * @param extension the desired extension
	 * @see exportGroup
	 * @see pulse.tasks.TaskManager
	 */

	public static void exportAllTasks(File directory, Extension extension) {
		TaskManager.getInstance().getTaskList().stream().forEach(t -> exportGroup(t, directory, extension));
	}

	/**
	 * Exports the currently selected task as a group of objects.
	 * 
	 * @param directory a pre-selected directory
	 * @param extension the desired extension
	 * @see exportGroup
	 * @see pulse.tasks.TaskManager.getSelectedTask()
	 */

	public static void exportCurrentTask(File directory, Extension extension) {
		exportGroup(TaskManager.getInstance().getSelectedTask(), directory, extension);
	}

	/**
	 * Exports the currently selected task as a group of objects using the default
	 * export extension.
	 * 
	 * @param directory a pre-selected directory
	 * @see exportGroup
	 * @see pulse.tasks.TaskManager.getSelectedTask()
	 */

	public static void exportCurrentTask(File directory) {
		exportCurrentTask(directory, getDefaultExportExtension());
	}

	/**
	 * Exports all results generated previously during task execution for all tasks
	 * listed by the TaskManager, provided those tasks had the respective result
	 * assigned to them.
	 * 
	 * @param directory a pre-selected directory
	 * @param extension the desired extension
	 */

	public static void exportAllResults(File directory, Extension extension) {

		var instance = TaskManager.getInstance();
		instance.getTaskList().stream().map(t -> instance.getResult(t)).filter(Objects::nonNull)
				.forEach(r -> export(r, directory, extension));

	}

	/**
	 * Fully exports {@code group} and all its contents to the root
	 * {@code directory} requesting the files to be saved with the
	 * {@code extension}.
	 * <p>
	 * If an {@code Exporter} exists that accepts the {@code group} as its argument,
	 * this will create files in the root {@code directory} in accordance to the
	 * root {@code Exporter} rules. All contents of the {@code group} will then be
	 * processed in a similar manner and the output will be stored in an internal
	 * directory, the name of which conforms to the respective description. Note
	 * this method is NOT recursive and it calls the {@code export} method of the
	 * {@code ExportManager}.
	 * </p>
	 * 
	 * @param group     a group
	 * @param directory a pre-selected root directory
	 * @param extension the desired extension
	 * @throws IllegalArgumentException if {@code directory} is not a directory
	 */

	public static void exportGroup(Group group, File directory, Extension extension) {
		if (!directory.isDirectory())
			throw new IllegalArgumentException("Not a directory: " + directory);

		var internalDirectory = new File(directory + separator + group.describe() + separator);
		internalDirectory.mkdirs();

		export(group, directory, extension);
		contents(group).stream().forEach(internalHolder -> export(internalHolder, internalDirectory, extension));
	}

}