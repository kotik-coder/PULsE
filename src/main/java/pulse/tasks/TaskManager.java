package pulse.tasks;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_WEEK_DATE;
import static java.util.Objects.requireNonNull;
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
import static pulse.util.Group.contents;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import pulse.input.ExperimentalData;
import pulse.input.InterpolationDataset;
import pulse.input.listeners.DataEvent;
import pulse.input.listeners.DataEventType;
import pulse.input.listeners.ExternalDatasetListener;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.CONDUCTIVITY;
import static pulse.properties.NumericPropertyKeyword.DENSITY;
import static pulse.properties.NumericPropertyKeyword.EMISSIVITY;
import static pulse.properties.NumericPropertyKeyword.SPECIFIC_HEAT;

import pulse.properties.SampleName;
import pulse.search.direction.PathOptimiser;
import pulse.tasks.listeners.SessionListener;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskRepositoryListener;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.listeners.TaskSelectionListener;
import pulse.tasks.logs.Status;
import static pulse.tasks.logs.Status.AWAITING_TERMINATION;
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
public final class TaskManager extends UpwardsNavigable {

    /**
     *
     */
    private static final long serialVersionUID = -4255751786167667650L;
    private List<SearchTask> tasks;
    private SearchTask selectedTask;
    private boolean singleStatement = true;
    private HierarchyListener statementListener;

    private transient List<TaskSelectionListener> selectionListeners;
    private transient List<TaskRepositoryListener> taskRepositoryListeners;
    private transient List<ExternalDatasetListener> externalListeners;

    private static TaskManager instance = new TaskManager();
    
    private static List<SessionListener> globalListeners = new ArrayList<>();
    
    private InterpolationDataset cpDataset;
    private InterpolationDataset rhoDataset;

    private TaskManager() {
        tasks = new ArrayList<>();
        initListeners();
    }
    
    /**
     * Creates a list of property keywords that can be derived with help of the
     * loaded data. For example, if heat capacity and density data is available,
     * the returned list will contain {@code CONDUCTIVITY}.
     *
     * @return
     */
    public List<NumericPropertyKeyword> derivableProperties() {
        var list = new ArrayList<NumericPropertyKeyword>();
        if (cpDataset != null) {
            list.add(SPECIFIC_HEAT);
        }
        if (rhoDataset != null) {
            list.add(DENSITY);
        }
        if (rhoDataset != null && cpDataset != null) {
            list.add(CONDUCTIVITY);
            list.add(EMISSIVITY);
        }
        return list;
    }
    
    @Override
    public void initListeners() {
        super.initListeners();
        selectionListeners = new CopyOnWriteArrayList<>();
        taskRepositoryListeners = new CopyOnWriteArrayList<>();
        externalListeners = new CopyOnWriteArrayList<>();
        statementListener = e -> {

            if (!(e.getSource() instanceof PropertyHolder)) {

                var task = (SearchTask) e.getPropertyHolder().specificAncestor(SearchTask.class);
                for (SearchTask t : tasks) {
                    if (t == task) {
                        continue;
                    }
                    t.update(e.getProperty());
                }

            }

        };
        addHierarchyListener(statementListener);
    }

    /**
     * This class uses a singleton pattern, meaning there is only instance of
     * this class.
     *
     * @return the single (static) instance of this class
     */
    public static TaskManager getManagerInstance() {
        return instance;
    }

    /**
     * <t>Executes {@code t} asynchronously using a {@code CompletableFuture}.
     * When done, creates a {@code Result} and puts it into the
     * {@code Map(SearchTask,Result)} in this {@code TaskManager}.</t>
     *
     * @param t a {@code SearchTask} that will be executed
     */
    public void execute(SearchTask t) {
        t.checkProblems();

        // try to start computation 
        // notify listeners computation is about to start
        if (t.getStatus() != QUEUED && !t.setStatus(QUEUED)) {
            return;
        }

        // notify listeners calculation started
        notifyListeners(new TaskRepositoryEvent(TASK_SUBMITTED, t.getIdentifier()));

        // run task t -- after task completed, write result and trigger listeners
        CompletableFuture.runAsync(t).thenRun(() -> {
            Calculation current = (Calculation) t.getResponse();
            var e = new TaskRepositoryEvent(TASK_FINISHED, t.getIdentifier());
            if (null == current.getStatus()) {
                notifyListeners(e);
            } else {
                switch (current.getStatus()) {
                    case DONE:
                        current.setResult(new Result(t, ResultFormat.getInstance()));
                        //notify listeners before the task is re-assigned
                        notifyListeners(e);
                        t.storeCalculation();
                        break;
                    case AWAITING_TERMINATION:
                        t.setStatus(Status.TERMINATED);
                        break;
                    default:
                        notifyListeners(e);
                        break;
                }
            }
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
     * that queue to a {@code ForkJoinPool} using a parallel stream. The size of
     * the pool is usually limited by hardware, e.g. for a 4 core system with 2
     * independent threads on each core, the limitation will be <math>4*2 - 1 =
     * 7</math>, etc.
     */
    public void executeAll() {

        tasks.stream().filter(t -> {
            switch (t.getStatus()) {
                case IN_PROGRESS:
                case EXECUTION_ERROR:
                    return false;
                default:
                    return true;
            }
        }).forEach(t -> {
            execute(t);
        });

    }

    /**
     * Checks if any of the tasks that this {@code TaskManager} manages is
     * either {@code QUEUED} or {@code IN_PROGRESS}.
     *
     * @return {@code false} if the status of the {@code SearchTask} is any of
     * the above; {@code false} otherwise.
     */
    public boolean isTaskQueueEmpty() {
        return !tasks.stream().anyMatch(t -> {
            var status = t.getStatus();
            return status == QUEUED || status == IN_PROGRESS;
        });
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

    public void fireTaskSelected(Object source) {
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
     * name from the {@code Metadata} associated with its
     * {@code ExperimentalData}.
     *
     * @return a {@code String} with the sample name, or {@code null} if no
     * suitable task can be found.
     */
    public SampleName getSampleName() {
        if (tasks.size() < 1) {
            return null;
        }

        var optional = tasks.stream().filter(t -> t != null).findFirst();

        if (!optional.isPresent()) {
            return null;
        }

        return ((ExperimentalData) optional.get().getInput())
                .getMetadata().getSampleName();
    }

    /**
     * <p>
     * Clears any progress for all the tasks and resets everything. Triggers a
     * {@code TASK_RESET} event.
     * </p>
     */
    public void reset() {
        if (tasks.isEmpty()) {
            return;
        }

        for (var task : tasks) {
            var e = new TaskRepositoryEvent(TASK_RESET, task.getIdentifier());

            task.clear();

            notifyListeners(e);
        }

        PathOptimiser.getInstance().reset();

    }

    /**
     * Finds a {@code SearchTask} whose {@code Identifier} matches {@code id}.
     *
     * @param id the {@code Identifier} of the task.
     * @return the {@code SearchTask} associated with this {@code Identifier}.
     */
    public SearchTask getTask(Identifier id) {
        var o = tasks.stream().filter(t -> t.getIdentifier().equals(id)).findFirst();
        return o.isPresent() ? o.get() : null;
    }

    /**
     * Finds a {@code SearchTask} using the external identifier specified in its
     * metadata.
     *
     * @param externalId the external ID of the data.
     * @return the {@code SearchTask} associated with this {@code Identifier}.
     */
    public SearchTask getTask(int externalId) {
        var o = tasks.stream().filter(t
                -> Integer.compare(((ExperimentalData) t.getInput())
                        .getMetadata().getExternalID(),
                        externalId) == 0).findFirst();
        return o.isPresent() ? o.get() : null;
    }

    /**
     * <p>
     * Generates a {@code SearchTask} assuming that the {@code ExperimentalData}
     * is stored in the {@code file}. This will make the {@code ReaderManager}
     * attempt to read that {@code file}. If successful, invokes
     * {@code addTask(...)} on the created {@code SearchTask}. After the task is
     * generated, checks whether the acquisition time recorded by the
     * experimental setup has been chosen appropriately.
     *
     * @see pulse.input.ExperimentalData.isAcquisitionTimeSensible()
     *
     * </p>
     *
     * @param file the file to load the experimental data from
     * @see addTask(SearchTask)
     * @see pulse.io.readers.ReaderManager.extract(File)
     */
    public void generateTask(File file) {
        var curves = read(curveReaders(), file);
        //notify curves have been loaded
        curves.stream().forEach(c -> c.fireDataChanged(new DataEvent(
                DataEventType.DATA_LOADED, c
        )));
        //create tasks
        curves.stream().forEach((ExperimentalData curve) -> {
            var task = new SearchTask(curve);
            addTask(task);
            var data = (ExperimentalData) task.getInput();
            if (!data.isAcquisitionTimeSensible()) {
                data.truncate();
            }
        });
    }

    /**
     * Generates multiple tasks from multiple {@code files}.
     *
     * @param files a list of {@code File}s that can be parsed down to
     * {@code ExperimentalData}.
     */
    public void generateTasks(List<File> files) {
        requireNonNull(files, "Null list of files passed to generatesTasks(...)");
        
        //this is the loader runnable submitted to the executor service
        Runnable loader = () -> {
            var pool = Executors.newSingleThreadExecutor();
            files.stream().forEach(f -> pool.submit(() -> generateTask(f)));
            pool.shutdown();

            try {
                pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException ex) {
                Logger.getLogger(TaskManager.class.getName()).log(Level.SEVERE, null, ex);
            }

            //when pool has been shutdown
            selectFirstTask();

        };

        Executors.newSingleThreadExecutor().submit(loader);
    }

    /**
     * <p>
     * If a task {@code equal} to {@code t} has already been previously loaded,
     * does nothing. Otherwise, adds this {@code t} to the task repository and
     * triggers a {@code TASK_ADDED} event.
     * </p>
     *
     * @param t the {@code SearchTask} that needs to be added to the internal
     * repository
     * @return {@code null} if a task like {@code t} has already been added
     * previously, {@code t} otherwise.
     * @see pulse.tasks.SearchTask.equals(SearchTask)
     */
    public SearchTask addTask(SearchTask t) {

        if (tasks.stream().filter(task -> task.equals(t)).count() > 0) {
            return null;
        }

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
     * repository.
     * @return {@code true} if the operation is successful, {@code false}
     * otherwise.
     */
    public boolean removeTask(SearchTask t) {
        if (tasks.stream().filter(task -> task.equals(t)).count() < 1) {
            return false;
        }

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
     * @param id the {@code Identifier} of a task within this repository.
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
        if (!tasks.isEmpty() && selectedTask != tasks.get(0)) {
            selectTask(tasks.get(0).getIdentifier(), this);
        }
    }

    public final void addSelectionListener(TaskSelectionListener listener) {
        selectionListeners.add(listener);
    }

    public final void addTaskRepositoryListener(TaskRepositoryListener listener) {
        taskRepositoryListeners.add(listener);
    }

    public List<TaskSelectionListener> getSelectionListeners() {
        return  selectionListeners;
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
     *
     * @return the string descriptor
     */
    @Override
    public String describe() {
        var name = getSampleName();
        return name == null || name.getValue() == null
                ? "Measurement_" + now().format(ISO_WEEK_DATE)
                : name.toString();
    }

    public void evaluate() {
        tasks.stream().forEach(t -> {
            var properties = ((Calculation) t.getResponse()).getProblem().getProperties();
            var c = (ExperimentalData) t.getInput();
            properties.useTheoreticalEstimates(c);
        });
    }

    public Set<Group> allGrouppedContents() {

        return getTaskList().stream().map(t -> contents(t)).reduce((a, b) -> {
            a.addAll(b);
            return a;
        }).get();

    }

    /**
     * Checks whether changes in this {@code PropertyHolder} should
     * automatically be accounted for by other instances of this class.
     *
     * @return {@code true} if the user has specified so (set by default),
     * {@code false} otherwise
     */
    public boolean isSingleStatement() {
        return singleStatement;
    }
    
    public static void assumeNewState(TaskManager loaded) {
        TaskManager.instance = null;
        TaskManager.instance = loaded;
        globalListeners.stream().forEach(l -> l.onNewSessionLoaded());
    }
    
    public void addExternalDatasetListener(ExternalDatasetListener edl) {
        this.externalListeners.add(edl);
    }
    
    public static void addSessionListener(SessionListener sl) {
        globalListeners.add(sl);
    }
    
    public static void removeSessionListeners() {
        globalListeners.clear();
    }

    /**
     * Sets the flag to isolate or inter-connects changes in all instances of
     * {@code PropertyHolder}
     *
     * @param singleStatement {@code false} if other {@code PropertyHoder}s
     * should disregard changes, which happened to this instances. {@code true}
     * otherwise.
     */
    public void setSingleStatement(boolean singleStatement) {
        this.singleStatement = singleStatement;
        if (!singleStatement) {
            this.removeHierarchyListener(statementListener);
        } else {
            this.addHierarchyListener(statementListener);
        }
    }
    
    /*
    Serialization
    */
    
    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        // default deserialization
        ois.defaultReadObject();
    }
    
    public InterpolationDataset getDensityDataset() {
        return rhoDataset;
    }
    
    public InterpolationDataset getSpecificHeatDataset() {
        return cpDataset;
    }
    
    public void setDensityDataset(InterpolationDataset dataset) {
        this.rhoDataset = dataset;
        this.externalListeners.stream().forEach(l -> l.onDensityDataLoaded());
        evaluate();
    }
    
    public void setSpecificHeatDataset(InterpolationDataset dataset) {
        this.cpDataset = dataset;
        this.externalListeners.stream().forEach(l -> l.onSpecificHeatDataLoaded());
        evaluate();
    }

}