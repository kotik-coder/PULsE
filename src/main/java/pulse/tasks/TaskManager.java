package pulse.tasks;

import static java.lang.System.gc;
import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_WEEK_DATE;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static pulse.io.readers.ReaderManager.curveReaders;
import static pulse.io.readers.ReaderManager.read;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.SHUTDOWN;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_ADDED;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_FINISHED;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_REMOVED;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_RESET;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_SUBMITTED;
import static pulse.tasks.logs.Status.DONE;
import static pulse.tasks.logs.Status.IN_PROGRESS;
import static pulse.tasks.logs.Status.QUEUED;
import static pulse.tasks.logs.Status.READY;
import static pulse.ui.Launcher.threadsAvailable;
import static pulse.util.Group.contents;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import pulse.input.InterpolationDataset;
import pulse.properties.SampleName;
import pulse.search.direction.PathOptimiser;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskRepositoryListener;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.listeners.TaskSelectionListener;
import pulse.tasks.processing.Result;
import pulse.tasks.processing.ResultFormat;
import pulse.util.Group;
import pulse.util.HierarchyListener;
import pulse.util.PropertyHolder;
import pulse.util.UpwardsNavigable;

/**
 * <p>
 * The {@code TaskManager} is a high-level class for dealing with operations of
 * creation, removal, storage, and execution of {@code SearchTask}s, as well as
 * with the associated {@code Result}s and {@code InterpolationDataset}s. Note
 * that {@code TaskManager} adopts a {@code PathSolver}.
 * </p>
 *
 */

public class TaskManager extends UpwardsNavigable {

	private static TaskManager instance = new TaskManager();

	private List<SearchTask> tasks;
	private SearchTask selectedTask;
	private Map<SearchTask, Result> results;

	private boolean singleStatement = true;

	private final int THREADS_AVAILABLE = threadsAvailable();
	private ForkJoinPool taskPool;

	private List<TaskSelectionListener> selectionListeners;
	private List<TaskRepositoryListener> taskRepositoryListeners;

	private final static String DEFAULT_NAME = "Project 1 - " + now().format(ISO_WEEK_DATE);

	private HierarchyListener statementListener = e -> {

		if (!(e.getSource() instanceof PropertyHolder)) {

			var task = (SearchTask) e.getPropertyHolder().specificAncestor(SearchTask.class);
			for (SearchTask t : tasks) {
				if (t == task)
					continue;
				t.update(e.getProperty());
			}

		}

	};

	private TaskManager() {
		tasks = new ArrayList<SearchTask>();
		results = new HashMap<SearchTask, Result>();
		taskPool = new ForkJoinPool(THREADS_AVAILABLE - 1);
		selectionListeners = new CopyOnWriteArrayList<TaskSelectionListener>();
		taskRepositoryListeners = new CopyOnWriteArrayList<TaskRepositoryListener>();
		this.addHierarchyListener(statementListener);
	}

	/**
	 * This class uses a singleton pattern, meaning there is only instance of this
	 * class.
	 * 
	 * @return the single (static) instance of this class
	 */

	public static TaskManager getManagerInstance() {
		return instance;
	}

	/**
	 * <t>Executes {@code t} asynchronously using a {@code CompletableFuture}. When
	 * done, creates a {@code Result} and puts it into the
	 * {@code Map(SearchTask,Result)} in this {@code TaskManager}.</t>
	 * 
	 * @param t a {@code SearchTask} that will be executed
	 */

	public void execute(SearchTask t) {
		removeResult(t); // remove old result
		t.setStatus(QUEUED); // notify listeners computation is about to start

		// notify listeners
		notifyListeners(new TaskRepositoryEvent(TASK_SUBMITTED, t.getIdentifier()));

		// run task t -- after task completed, write result and trigger listeners

		CompletableFuture.runAsync(t).thenRun(() -> {
			if (t.getStatus() == DONE) {
				results.put(t, new Result(t, ResultFormat.getInstance()));
			}
			var e = new TaskRepositoryEvent(TASK_FINISHED, t.getIdentifier());
			notifyListeners(e);
		});

	}

	/**
	 * Notifies the {@code TaskRepositoryListener}s of the {@code e}
	 * 
	 * @param e an event
	 */

	public void notifyListeners(TaskRepositoryEvent e) {
		taskRepositoryListeners.stream().forEach(l -> l.onTaskListChanged(e));
	}

	/**
	 * <p>
	 * Creates a queue of {@code SearchTask}s based on their readiness and feeds
	 * that queue to a {@code ForkJoinPool} using a parallel stream. The size of the
	 * pool is usually limited by hardware, e.g. for a 4 core system with 2
	 * independent threads on each core, the limitation will be <math>4*2 - 1 =
	 * 7</math>, etc.
	 */

	public void executeAll() {

		var queue = tasks.stream().filter(t -> {
			switch (t.getStatus()) {
			case DONE:
			case IN_PROGRESS:
			case EXECUTION_ERROR:
				return false;
			default:
				return true;
			}
		}).collect(toList());

		try {
			taskPool.submit(() -> queue.parallelStream().forEach(t -> execute(t))).get();
		} catch (InterruptedException | ExecutionException e) {
			System.err.println("Execution exception while running multiple tasks");
			e.printStackTrace();
		}

		gc();

	}

	/**
	 * Checks if any of the tasks that this {@code TaskManager} manages is either
	 * {@code QUEUED} or {@code IN_PROGRESS}.
	 * 
	 * @return {@code false} if the status of the {@code SearchTask} is any of the
	 *         above; {@code false} otherwise.
	 */

	public boolean isTaskQueueEmpty() {
		return !tasks.stream().anyMatch(t -> t.getStatus() == QUEUED || t.getStatus() == IN_PROGRESS);
	}

	/**
	 * This will terminate all tasks in this {@code TaskManager} and trigger a
	 * {@code SHUTDOWN} {@code TaskRepositoryEvent}.
	 * 
	 * @see pulse.tasks.Task.terminate()
	 */

	public void cancelAllTasks() {

		tasks.stream().forEach(t -> t.terminate());

		var e = new TaskRepositoryEvent(SHUTDOWN, null);

		notifyListeners(e);

	}

	/**
	 * Checks whether the acquisition time recorded by the experimental setup has
	 * been chosen appropriately.
	 * 
	 * @return {@code false} if the acquisition time seems sensible for the
	 *         {@code ExperimentalData} in each of the tasks; {@code true}
	 *         otherwise.
	 * @see pulse.input.ExperimentalData.isAcquisitionTimeSensible()
	 */

	public boolean dataNeedsTruncation() {

		return tasks.stream().anyMatch(t ->

		!t.getExperimentalCurve().isAcquisitionTimeSensible()

		);

	}

	/**
	 * Calls {@code truncate()} on {@code ExperimentalData} for each
	 * {@code SearchTask}.
	 * 
	 * @see pulse.input.ExperimentalData.truncate()
	 */

	public void truncateData() {
		tasks.stream().forEach(t -> t.getExperimentalCurve().truncate());
	}

	private void fireTaskSelected(Object source) {
		var e = new TaskSelectionEvent(source);
		for (var l : selectionListeners) {
			l.onSelectionChanged(e);
		}
	}

	/**
	 * <p>
	 * Purges all tasks from this {@code TaskManager}. Generates a
	 * {@code TASK_REMOVED} {@code TaskRepositoryEvent} for each of the removed
	 * tasks. Clears task selection.
	 * </p>
	 */

	public void clear() {
		tasks.stream().sorted((t1, t2) -> -t1.getIdentifier().compareTo(t2.getIdentifier())).forEach(task -> {
			var e = new TaskRepositoryEvent(TASK_REMOVED, task.getIdentifier());
			notifyListeners(e);
		});

		tasks.clear();
		selectTask(null, null);
	}

	/**
	 * Uses the first non-{@code null} {@code SearchTask} to retrieve the sample
	 * name from the {@code Metadata} associated with its {@code ExperimentalData}.
	 * 
	 * @return a {@code String} with the sample name, or {@code null} if no suitable
	 *         task can be found.
	 */

	public SampleName getSampleName() {
		if (tasks.size() < 1)
			return null;

		var optional = tasks.stream().filter(t -> t != null).findFirst();

		if (!optional.isPresent())
			return null;

		return optional.get().getExperimentalCurve().getMetadata().getSampleName();
	}

	/**
	 * <p>
	 * Clears any progress for all the tasks and resets everything. Triggers a
	 * {@code TASK_RESET} event.
	 * </p>
	 */

	public void reset() {
		if (tasks.isEmpty())
			return;

		for (var task : tasks) {
			var e = new TaskRepositoryEvent(TASK_RESET, task.getIdentifier());

			task.clear();

			notifyListeners(e);
		}

		PathOptimiser.reset();

	}

	/**
	 * Finds a {@code SearchTask} whose {@code Identifier} matches {@code id}.
	 * 
	 * @param id the {@code Identifier} of the task.
	 * @return the {@code SearchTask} associated with this {@code Identifier}.
	 */

	public SearchTask getTask(Identifier id) {
		return tasks.stream().filter(t -> t.getIdentifier().equals(id)).findFirst().get();
	}

	/**
	 * <p>
	 * Generates a {@code SearchTask} assuming that the {@code ExperimentalData} is
	 * stored in the {@code file}. This will make the {@code ReaderManager} attempt
	 * to read that {@code file}. If successful, invokes {@code addTask(...)} on the
	 * created {@code SearchTask}.
	 * </p>
	 * 
	 * @param file
	 * @see addTask(SearchTask)
	 * @see pulse.io.readers.ReaderManager.extract(File)
	 */

	public void generateTask(File file) {
		read(curveReaders(), file).stream().forEach(curve -> addTask(new SearchTask(curve)));
	}

	/**
	 * Generates multiple tasks from multiple {@code files}.
	 * 
	 * @param files a list of {@code File}s that can be parsed down to
	 *              {@code ExperimentalData}.
	 */

	public void generateTasks(List<File> files) {
		requireNonNull(files, "Null list of files passed to generatesTasks(...)");

		var pool = Executors.newSingleThreadExecutor();
		files.stream().forEach(f -> pool.submit(() -> generateTask(f)));
		pool.shutdown();
		try {
			pool.awaitTermination(2, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			System.err.println("Failed to load all tasks within 2 minutes. Details:");
			e.printStackTrace();
		}

		selectFirstTask();

	}

	/**
	 * <p>
	 * If a task {@code equal} to {@code t} has already been previously loaded, does
	 * nothing. Otherwise, adds this {@code t} to the task repository and triggers a
	 * {@code TASK_ADDED} event.
	 * </p>
	 * 
	 * @param t the {@code SearchTask} that needs to be added to the internal
	 *          repository
	 * @return {@code null} if a task like {@code t} has already been added
	 *         previously, {@code t} otherwise.
	 * @see pulse.tasks.SearchTask.equals(SearchTask)
	 */

	public SearchTask addTask(SearchTask t) {

		if (tasks.stream().filter(task -> task.equals(t)).count() > 0)
			return null;

		tasks.add(t);

		var e = new TaskRepositoryEvent(TASK_ADDED, t.getIdentifier());
		t.setParent(getManagerInstance());
		notifyListeners(e);

		return t;
	}

	/**
	 * If {@code t} is found in the local repository, removes it and triggers a
	 * {@code TASK_REMOVED} event.
	 * 
	 * @param t a {@code SearchTask} that has been previously loaded to this
	 *          repository.
	 * @return {@code true} if the operation is successful, {@code false} otherwise.
	 */

	public boolean removeTask(SearchTask t) {
		if (tasks.stream().filter(task -> task.equals(t)).count() < 1)
			return false;

		tasks.remove(t);

		var e = new TaskRepositoryEvent(TASK_REMOVED, t.getIdentifier());

		notifyListeners(e);
		selectedTask = null;

		return true;
	}

	/**
	 * Gets the current number of tasks in the repository.
	 * 
	 * @return the number of available tasks.
	 */

	public int numberOfTasks() {
		return tasks.size();
	}

	/**
	 * <p>
	 * Selects a {@code SearchTask} within this repository with the specified
	 * {@code id} (if present). Informs the listeners this selection has been
	 * triggered by {@code src}.
	 * </p>
	 * 
	 * @param id  the {@code Identifier} of a task within this repository.
	 * @param src the source of the selection.
	 */

	public void selectTask(Identifier id, Object src) {
		tasks.stream().filter(t -> t.getIdentifier().equals(id)).filter(t -> t != selectedTask).findAny()
				.ifPresent(t -> {
					selectedTask = t;
					fireTaskSelected(src);
				});
	}

	public void selectFirstTask() {
		if (!tasks.isEmpty())
			selectTask(tasks.get(0).getIdentifier(), this);
	}

	public void addSelectionListener(TaskSelectionListener listener) {
		selectionListeners.add(listener);
	}

	public void addTaskRepositoryListener(TaskRepositoryListener listener) {
		taskRepositoryListeners.add(listener);
	}

	public TaskSelectionListener[] getSelectionListeners() {
		return (TaskSelectionListener[]) selectionListeners.toArray();
	}

	public void removeSelectionListeners() {
		selectionListeners.clear();
	}

	public void removeTaskRepositoryListener(TaskRepositoryListener trl) {
		taskRepositoryListeners.remove(trl);
	}

	public int indexOfTask(SearchTask t) {
		return tasks.indexOf(t);
	}

	public List<SearchTask> getTaskList() {
		return tasks;
	}

	public SearchTask getSelectedTask() {
		return selectedTask;
	}

	public List<TaskRepositoryListener> getTaskRepositoryListeners() {
		return taskRepositoryListeners;
	}

	/**
	 * This {@code TaskManager} will be described by the sample name for the
	 * experiment.
	 */

	@Override
	public String describe() {
		return tasks.size() > 0 ? getSampleName().toString() : DEFAULT_NAME;
	}

	public Result getResult(SearchTask t) {
		return results.get(t);
	}

	/**
	 * Assigns {@code r} as the {@code Result} for {@code t}.
	 * 
	 * @param t the {@code Result}
	 * @param r the {@code SearchTask}.
	 */

	public void useResult(SearchTask t, Result r) {
		results.put(t, r);
	}

	/**
	 * Searches for a {@code Result} for a {@code SearchTask} with a specific
	 * {@code id}.
	 * 
	 * @param id the {@code Identifier} of a {@code SearchTask}
	 * @return {@code null} if such {@code Result} cannot be found. Otherwise,
	 *         returns the found {@code Result}.
	 */

	public Result getResult(Identifier id) {
		var optional = tasks.stream().filter(t -> t.getIdentifier().equals(id)).findFirst();
		return optional.isPresent() ? results.get(optional.get()) : null;
	}

	/**
	 * Removes the results of the task {@code t} and sets its status to
	 * {@code READY}.
	 * 
	 * @param t a {@code SearchTask} contained in the repository
	 */

	public void removeResult(SearchTask t) {
		if (!results.containsKey(t))
			return;
		results.remove(t);
		t.setStatus(READY);
	}

	public void evaluate() {
		tasks.stream().forEach(t -> {
			var properties = t.getProblem().getProperties();
			InterpolationDataset.fill(properties);
			properties.useTheoreticalEstimates(t.getExperimentalCurve());
		});
	}

	public Set<Group> allGrouppedContents() {

		return getTaskList().stream().map(t -> contents(t)).reduce((a, b) -> {
			a.addAll(b);
			return a;
		}).get();

	}

	/**
	 * Checks whether changes in this {@code PropertyHolder} should automatically be
	 * accounted for by other instances of this class.
	 * 
	 * @return {@code true} if the user has specified so (set by default),
	 *         {@code false} otherwise
	 */

	public boolean isSingleStatement() {
		return singleStatement;
	}

	/**
	 * Sets the flag to isolate or inter-connects changes in all instances of
	 * {@code PropertyHolder}
	 * 
	 * @param singleStatement {@code false} if other {@code PropertyHoder}s should
	 *                        disregard changes, which happened to this instances.
	 *                        {@code true} otherwise.
	 */

	public void setSingleStatement(boolean singleStatement) {
		this.singleStatement = singleStatement;
		if (!singleStatement)
			this.removeHierarchyListener(statementListener);
		else
			this.addHierarchyListener(statementListener);
	}

}