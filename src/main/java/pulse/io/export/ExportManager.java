package pulse.io.export;

import static java.util.Arrays.asList;
import static pulse.io.export.Exporter.getDefaultExportExtension;
import static pulse.io.export.MassExporter.contents;
import static pulse.io.export.MassExporter.exportGroup;
import static pulse.tasks.TaskManager.getResult;
import static pulse.tasks.TaskManager.getSelectedTask;
import static pulse.tasks.TaskManager.getTaskList;
import static pulse.util.Reflexive.instancesOf;

import java.io.File;
import java.util.Set;

import javax.swing.JFrame;

import pulse.util.Descriptive;
import pulse.util.Group;

public class ExportManager {

	private ExportManager() {
		// intentionally blank
	}

	@SuppressWarnings("unchecked")
	public static <T extends Descriptive> Exporter<T> findExporter(T target) {
		return target == null ? null : (Exporter<T>) findExporter(target.getClass());
	}

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

	public static <T extends Descriptive> void askToExport(T target, JFrame parentWindow, String fileTypeLabel) {
		var exporter = findExporter(target);
		if (exporter != null)
			exporter.askToExport(target, parentWindow, fileTypeLabel);
		else
			throw new IllegalArgumentException("No exporter for " + target.getClass().getSimpleName());
	}

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

	public static void exportAllTasks(File directory, Extension extension) {

		getTaskList().stream().forEach(t -> exportGroup(t, directory, extension));

	}

	public static void exportCurrentTask(File directory, Extension extension) {
		exportGroup(getSelectedTask(), directory, extension);
	}

	public static void exportCurrentTask(File directory) {
		exportGroup(getSelectedTask(), directory, getDefaultExportExtension());
	}

	public static Set<Group> allGrouppedContents() {

		return getTaskList().stream().map(t -> contents(t)).reduce((a, b) -> {
			a.addAll(b);
			return a;
		}).get();

	}

	public static void exportAllResults(File directory, Extension extension) {

		getTaskList().stream().map(t -> getResult(t))
				.forEach(r -> export(r, directory, extension));

	}

}