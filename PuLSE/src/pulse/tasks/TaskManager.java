package pulse.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import pulse.input.ExperimentalData;
import pulse.input.PropertyCurve;
import pulse.io.readers.ReaderManager;
import pulse.search.direction.PathSolver;
import pulse.tasks.listeners.TaskListener;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskRepositoryListener;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.listeners.TaskSelectionListener;
import pulse.tasks.listeners.TaskStateEvent;
import pulse.util.Describable;
import pulse.util.SaveableDirectory;
import pulse.tasks.listeners.TaskRepositoryEvent.State;

public final class TaskManager implements Describable {	
	
private static TaskManager instance = new TaskManager();	
	
private static List<SearchTask> tasks = new LinkedList<SearchTask>();
private static ExecutorService executor;
private static final int threadsAvailable = threadsAvailable();

private static SearchTask selectedTask;
private static List<TaskSelectionListener> selectionListeners = new ArrayList<TaskSelectionListener>();
private static List<TaskRepositoryListener> taskListeners = new ArrayList<TaskRepositoryListener>();

private static PathSolver pathSolver;

private static PropertyCurve specificHeatCurve, densityCurve;

private TaskManager() { }

public static TaskManager getInstance() {
	return instance;
}

public static void execute(SearchTask t) {
	TaskRepositoryEvent e = new TaskRepositoryEvent(TaskRepositoryEvent.State.SINGLE_TASK_SUBMITTED, t.getIdentifier());
	
	for(TaskRepositoryListener listener : taskListeners)
		listener.onTaskListChanged(e);
	
	t.setStatus(Status.QUEUED);
	
	executor	= Executors.newSingleThreadExecutor();
	executor.execute(t);
}

public static void executeAll() {
	executor	= Executors.newFixedThreadPool(threadsAvailable - 1);
	int submittedTasks = 0;	
	
	for(SearchTask t : tasks) {
		if(t.getStatus() != Status.READY)
			continue;
		
		t.setActive(true);
		t.setStatus(Status.QUEUED);
		
		executor.execute(t);
		submittedTasks++;
	
	}
	
	if(submittedTasks < 1)
		return;
	
	TaskRepositoryEvent e = new TaskRepositoryEvent(TaskRepositoryEvent.State.MULTIPLE_TASKS_SUBMITTED, null);
	
	for(TaskRepositoryListener listener : taskListeners)
		listener.onTaskListChanged(e);
	
}

public static boolean isTaskQueueEmpty() {
	if(executor == null)
		return true;
	
	for(SearchTask t : tasks)
		switch(t.getStatus()) {
			case QUEUED :
			case IN_PROGRESS :
				return false;
			default:
				continue;
		}

	return true;
}

public static void cancelAllTasks() {
	if(executor == null)
		return;
	
	executor.shutdownNow();
	
	for(SearchTask t : tasks)
		t.setActive(false);
	
}

public static boolean dataNeedsTruncation() {
	ExperimentalData dat;
	for(SearchTask t : tasks) {
		dat = t.getExperimentalCurve();
		if(!dat.isAcquisitionTimeSensible())
			return true;
	}
	return false;
}

public static void truncateData() {
	ExperimentalData dat;
	for(SearchTask t : tasks) {
		dat = t.getExperimentalCurve();	
		dat.truncate();
		t.updateTimeLimit();
	}
}

public static void clear() {
	for(SearchTask task : tasks) {
		TaskRepositoryEvent e = new TaskRepositoryEvent(TaskRepositoryEvent.State.TASK_REMOVED, task.getIdentifier());
		
		for(TaskRepositoryListener listener : taskListeners)
			listener.onTaskListChanged(e);
	}
	
	tasks.clear();

	selectedTask = null;
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
		
		for(TaskRepositoryListener listener : taskListeners)
			listener.onTaskListChanged(e);		
	}
			
	TaskManager.selectTask(tasks.get(0).getIdentifier(), TaskManager.getInstance());
}

public static SearchTask retrieveTask(double initialTemperature) {
	for(SearchTask t : tasks)
		if(Math.abs((double)t.getTestTemperature().getValue() - initialTemperature) < 1E-10)
			return t;
	
	return null;
}

public static SearchTask getTask(Identifier id) {
	for(SearchTask t : tasks)
		if(t.getIdentifier().equals(id))
			return t;
	return null;
}

public static PropertyCurve getSpecificHeatCurve() {
	return specificHeatCurve;
}

public static PropertyCurve getDensityCurve() {
	return densityCurve;
}

public static SearchTask[] generateTasks(File file) throws IOException {
	ExperimentalData[] curves = ReaderManager.extractData(file);
	SearchTask[] tasks = new SearchTask[curves.length];
	
	for(int i = 0; i < tasks.length; i++) {
		tasks[i] = new SearchTask(curves[i]);
		addTask(tasks[i]);
	}
	
	return tasks;
	
}

public static SearchTask addTask(SearchTask t)  {		

	for(SearchTask task : tasks)
		if(task.equals(t))
			return null;
	
	tasks.add(t);
	
	TaskRepositoryEvent e = new TaskRepositoryEvent(TaskRepositoryEvent.State.TASK_ADDED, t.getIdentifier());
	
	for(TaskRepositoryListener listener : taskListeners)
		listener.onTaskListChanged(e);
	
	t.addTaskListener(new TaskListener() {

		@Override
		public void onStatusChange(TaskStateEvent e) {
			if(e.getState() == Status.DONE)
				if(isTaskQueueEmpty())
					for(TaskRepositoryListener trl : taskListeners) 
						trl.onTaskListChanged(new TaskRepositoryEvent(State.ALL_TASKS_FINISHED, null));
		}

		@Override
		public void onDataCollected(TaskStateEvent e) {
			// TODO Auto-generated method stub	
		}
		
	});
	
	return t;
}

public static boolean removeTask(SearchTask t)  {		
	Identifier id = null;
	
	for(SearchTask task : tasks)
		if(task.equals(t)) 
			id = t.getIdentifier();
	
	if(id == null)
		return false;

	TaskRepositoryEvent e = new TaskRepositoryEvent(TaskRepositoryEvent.State.TASK_REMOVED, id);
	
	for(TaskRepositoryListener listener : taskListeners)
		listener.onTaskListChanged(e);
	
	tasks.remove(t);
	selectedTask = null;
	
	TaskManager.selectTask(tasks.get(0).getIdentifier(), TaskManager.getInstance());
	
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
	if(id == null) 
		return;
	
	for(SearchTask t : tasks) 
		if(id.equals(t.getIdentifier())) {
			selectedTask = t;
			
			TaskSelectionEvent e = new TaskSelectionEvent(src);
			
			for(TaskSelectionListener l : selectionListeners)
				l.onSelectionChanged(e);
			
			return;
		}
	
	selectedTask = null;
	
}

public static int threadsAvailable() {
	int number = Runtime.getRuntime().availableProcessors();
	return number > 1 ? (number - 1) : 1;
}

public static void addSelectionListener(TaskSelectionListener listener) {
	selectionListeners.add(listener);
}

public static void addTaskRepositoryListener(TaskRepositoryListener listener) {
	taskListeners.add(listener);
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

public static List<TaskRepositoryListener> getTaskListeners() {
	return taskListeners;
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

}