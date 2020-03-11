package pulse.io.export;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;

import pulse.tasks.TaskManager;
import pulse.util.Describable;
import pulse.util.Group;
import pulse.util.Reflexive;

public class ExportManager {

	private ExportManager() {
		// intentionally blank
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Describable> Exporter<T> findExporter(T target) {		
		return target == null ? null : (Exporter<T>) findExporter(target.getClass());
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends Describable> Exporter<T> findExporter(Class<T> target) {
		List<Exporter> allExporters = Reflexive.instancesOf(Exporter.class);
		var exporter = allExporters.stream().filter(e -> 
													e.target() == target ).findFirst();
		
		if(exporter.isPresent())
			return exporter.get();
		else {
			exporter = allExporters.stream().filter( e -> e.target().isAssignableFrom(target) ).findFirst();
			return exporter.isPresent() ? exporter.get() : null;
		}
	}
	
	public static <T extends Describable> void askToExport(T target, JFrame parentWindow, String fileTypeLabel) {
		Exporter<T> exporter = ExportManager.findExporter(target);
		if(exporter != null)
			exporter.askToExport(target, parentWindow, fileTypeLabel);
		else
			throw new IllegalArgumentException("No exporter for " + target.getClass().getSimpleName());
	}
	
	public static <T extends Describable>  void export(T target, File directory, Extension extension) {
		var exporter = findExporter(target);
	
		if(exporter != null) {
			Extension[] supportedExtensions = exporter.getSupportedExtensions();
			
			if(supportedExtensions.length > 0) {	
				Extension confirmedExtension = 
						Arrays.asList(supportedExtensions).contains(extension) ? 
						extension : supportedExtensions[0];
				exporter.export(target, directory, confirmedExtension);
			}
			
		}
	}
	
	public static void exportAllTasks(File directory, Extension extension) {
		
		TaskManager.getTaskList().stream().forEach(t -> MassExporter.exportGroup(t, directory, extension));
		
	}
	
	public static void exportCurrentTask(File directory, Extension extension) {
		 MassExporter.exportGroup(TaskManager.getSelectedTask(), directory, extension);
	}
	
	public static void exportCurrentTask(File directory) {
		 MassExporter.exportGroup(TaskManager.getSelectedTask(), directory, Exporter.getDefaultExportExtension());
	}
	
	public static List<Group> allGrouppedContents() {

		return TaskManager.getTaskList().stream().map(t -> MassExporter.contents(t))
									.reduce((a, b) -> {
										a.addAll(b);
										return a;
									}).get();
			
	}
	
	public static void exportAllResults(File directory, Extension extension) {
		
		TaskManager.getTaskList().stream().map(t -> TaskManager.getResult(t)).
				forEach(r -> ExportManager.export(r, directory, extension));
		
	}

}