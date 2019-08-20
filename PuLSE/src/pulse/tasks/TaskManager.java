package pulse.tasks;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import pulse.input.ExperimentalData;
import pulse.input.PropertyCurve;
import pulse.io.readers.ReaderManager;
import pulse.problem.statements.Problem;
import pulse.search.direction.PathSolver;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskRepositoryListener;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.listeners.TaskSelectionListener;
import pulse.ui.Launcher;
import pulse.util.Describable;
import pulse.util.SaveableDirectory;

public final class TaskManager implements Describable {	
	
private static TaskManager instance = new TaskManager();	
private static PathSolver pathSolver;
private static PropertyCurve specificHeatCurve, densityCurve;

private static ForkJoinPool taskPool;

private static List<SearchTask> tasks = new LinkedList<SearchTask>();
private static SearchTask selectedTask;

private static Map<SearchTask,Result> results = new HashMap<SearchTask,Result>();

private static final int threadsAvailable = Launcher.threadsAvailable();

private static List<TaskSelectionListener> selectionListeners = new CopyOnWriteArrayList<TaskSelectionListener>();
private static List<TaskRepositoryListener> taskRepositoryListeners = new CopyOnWriteArrayList<TaskRepositoryListener>();

private TaskManager() { }

public static TaskManager getInstance() {
	return instance;
}

public static void execute(SearchTask t) {
	removeResult(t); //remove old result	
	t.setStatus(Status.QUEUED); //notify listeners computation is about to start
	
	//notify listeners
	notifyListeners( new TaskRepositoryEvent(
			TaskRepositoryEvent.State.TASK_SUBMITTED, t.getIdentifier()) );	
	
	//run task t -- after task completed, write result and trigger listeners
	
	CompletableFuture.runAsync(t).thenRun(
					
					new Runnable() {

						@Override
						public void run() {
							
							if(t.getStatus() == Status.DONE) {
							
							try {
								results.put( t, new Result(t, ResultFormat.getFormat() ) );
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								System.err.println("Error retrieving result of task " + t);
								e.printStackTrace();
							}
							
							}
							
							TaskRepositoryEvent e = new TaskRepositoryEvent(
									TaskRepositoryEvent.State.TASK_FINISHED, t.getIdentifier());
							
							notifyListeners(e);
							
					}
																		
			}
					
	);

}

private static void notifyListeners(TaskRepositoryEvent e) {
	taskRepositoryListeners.stream().forEach(l -> l.onTaskListChanged(e));
}

public static void executeAll() {
	
	List<SearchTask> queue = tasks.stream().filter(t -> 
													{ switch(t.getStatus()) {
														case DONE : 
														case IN_PROGRESS :
														case EXECUTION_ERROR :
															return false;
														default: 
															return true; }
													}).collect(Collectors.toList());	
	
	taskPool = new ForkJoinPool(threadsAvailable - 1);
	
	try {
		taskPool.submit( () -> queue.parallelStream().forEach( t -> execute(t) ) ).get();
	} catch (InterruptedException | ExecutionException e) {
		System.err.println("Execution exception while running multiple tasks");
		e.printStackTrace();
	}
		
	System.gc();
		
}

public static boolean isTaskQueueEmpty() {
	return ! tasks.stream().anyMatch( t -> 
		t.getStatus() == Status.QUEUED || t.getStatus() == Status.IN_PROGRESS
	);	
}

public static void cancelAllTasks() {

	tasks.stream().forEach(t -> t.terminate() );
	
	TaskRepositoryEvent e = new TaskRepositoryEvent(
			TaskRepositoryEvent.State.SHUTDOWN, null);
	
	notifyListeners(e);
	
}

public static boolean dataNeedsTruncation() {
	
	return tasks.stream().anyMatch(
			t -> 
			
			!t.getExperimentalCurve().isAcquisitionTimeSensible() 
			
			);
		
}

public static void truncateData() {
	tasks.stream().forEach(t -> {
		t.getExperimentalCurve().truncate();
		t.updateTimeLimit();
	});
	
}

public static void selectFirstTask() {
	Optional<SearchTask> task = tasks.stream().filter(t -> t != null).findFirst();
	if(task.isPresent())
		selectTask(task.get().getIdentifier(), TaskManager.getInstance());
}

public static void clear() {
	for(SearchTask task : tasks) {
		TaskRepositoryEvent e = new TaskRepositoryEvent(
				TaskRepositoryEvent.State.TASK_REMOVED, task.getIdentifier());
		
		notifyListeners(e);
	}
	
	tasks.clear();
	selectTask(null, null);
}

public static String getSampleName() {
	return tasks.size() < 1 ? null : tasks.get(0).getExperimentalCurve().getMetadata().getSampleName();
}

public static void reset() {
	if(tasks.size() < 1)
		return;
	
	for(SearchTask task : tasks) {
		TaskRepositoryEvent e = new TaskRepositoryEvent(TaskRepositoryEvent.State.TASK_RESET, task.getIdentifier());
		
		task.reset();
		
		notifyListeners(e);	
	}
	
	PathSolver.reset();
			
}

public static SearchTask retrieveTask(double initialTemperature) {
	final double ZERO = 1E-10;
	
	return tasks.stream().filter( t -> 
		Math.abs((double)t.getTestTemperature().getValue() - initialTemperature) < ZERO ).
		findFirst().get();
	
}

public static SearchTask getTask(Identifier id) {
	return tasks.stream().filter( t -> 
		t.getIdentifier().equals(id) ).
			findFirst().get();
}

public static PropertyCurve getSpecificHeatCurve() {
	return specificHeatCurve;
}

public static PropertyCurve getDensityCurve() {
	return densityCurve;
}

public static void generateTask(File file) {
	List<ExperimentalData> curves = null;
	try {
		curves = ReaderManager.extractData(file);
	} catch (IOException e) {
		System.err.println("Error loading experimental data");
		e.printStackTrace();
	}
	curves.stream().forEach(curve -> addTask(new SearchTask(curve)) );		
}

public static void generateTasks(List<File> files) {
	if(files.size() == 1) {
		generateTask(files.get(0));
		return;
	}
	
	files.stream().forEach( f -> generateTask(f));

}

public static SearchTask addTask(SearchTask t)  {		

	if(tasks.stream().filter(task -> task.equals(t)).count() > 0)
		return null;
	
	tasks.add(t);
	
	TaskRepositoryEvent e = new TaskRepositoryEvent(TaskRepositoryEvent.State.TASK_ADDED, t.getIdentifier());
	
	notifyListeners(e);
	
	return t;
}

public static boolean removeTask(SearchTask t)  {			
	if(tasks.stream().filter(task -> task.equals(t)).count() < 1)
		return false;
		
	tasks.remove(t);
	
	TaskRepositoryEvent e = 
			new TaskRepositoryEvent(
					TaskRepositoryEvent.State.TASK_REMOVED, t.getIdentifier());
	
	notifyListeners(e);
	selectedTask = null;
	
	return true;
}	

public static int numberOfTasks() {
	return tasks.size();
}

public static void loadSpecificHeatData(File f) throws IOException {
	specificHeatCurve = ReaderManager.readPropertyTable(f);
	for(SearchTask t : tasks) 
		t.updateThermalProperties();
}

public static void loadDensityData(File f) throws IOException {
	densityCurve = ReaderManager.readPropertyTable(f);
	for(SearchTask t : tasks)
		t.updateThermalProperties();
}

public static void selectTask(Identifier id, Object src) {
	selectedTask = null;
	
	tasks.stream().filter( t -> t.getIdentifier().equals(id)).findAny().
			ifPresent( t -> {
				selectedTask = t;
				
				TaskSelectionEvent e = new TaskSelectionEvent(src);
				
				for(TaskSelectionListener l : selectionListeners)
					l.onSelectionChanged(e);
				
			});	
	
}

public static void addSelectionListener(TaskSelectionListener listener) {
	selectionListeners.add(listener);
}

public static void addTaskRepositoryListener(TaskRepositoryListener listener) {
	taskRepositoryListeners.add(listener);
}

public static TaskSelectionListener[] getSelectionListeners() {
	return (TaskSelectionListener[]) selectionListeners.toArray();
}

public static void removeSelectionListeners() {
	selectionListeners.clear();
}

public static int indexOfTask(SearchTask t) {
	return tasks.indexOf(t);
}

public static List<SearchTask> getTaskList() {
	return tasks;
}

public static SearchTask getSelectedTask() {
	return selectedTask;
}

public static PathSolver getPathSolver() {
	return pathSolver;
}

public static void setPathSolver(PathSolver pathSolver) {
	TaskManager.pathSolver = pathSolver;
}

public static List<TaskRepositoryListener> getTaskRepositoryListeners() {
	return taskRepositoryListeners;
}

@Override
public String describe() {
	return getSampleName();
}

public static List<SaveableDirectory> saveableContents() {
	
	List<SaveableDirectory> list = tasks
		    .stream()
		    .filter(SaveableDirectory.class::isInstance)
		    .map(SaveableDirectory.class::cast)
		    .collect(Collectors.toList());
	
	return list;
	
}

public static Result getResult(SearchTask t) {		
	return results.get(t);
}

public static Result getResult(Identifier id) {
	Optional<SearchTask> optional = tasks.stream().filter(t -> t.getIdentifier().equals(id)).findFirst();
	
	if(!optional.isPresent())
		return null;
	
	return results.get(optional.get());
}

public static void removeResult(SearchTask t) {
	if(! results.containsKey(t) )
		return;
	results.remove(t);
	t.setStatus(Status.READY);		
}

}