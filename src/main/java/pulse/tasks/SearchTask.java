package pulse.tasks;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.TIME_LIMIT;
import static pulse.search.direction.ActiveFlags.activeParameters;
import static pulse.search.direction.PathOptimiser.getInstance;
import static pulse.tasks.logs.Details.ABNORMAL_DISTRIBUTION_OF_RESIDUALS;
import static pulse.tasks.logs.Details.INCOMPATIBLE_OPTIMISER;
import static pulse.tasks.logs.Details.INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT;
import static pulse.tasks.logs.Details.MAX_ITERATIONS_REACHED;
import static pulse.tasks.logs.Details.MISSING_BUFFER;
import static pulse.tasks.logs.Details.MISSING_DIFFERENCE_SCHEME;
import static pulse.tasks.logs.Details.MISSING_HEATING_CURVE;
import static pulse.tasks.logs.Details.MISSING_OPTIMISER;
import static pulse.tasks.logs.Details.MISSING_PROBLEM_STATEMENT;
import static pulse.tasks.logs.Details.PARAMETER_VALUES_NOT_SENSIBLE;
import static pulse.tasks.logs.Details.SIGNIFICANT_CORRELATION_BETWEEN_PARAMETERS;
import static pulse.tasks.logs.Status.AMBIGUOUS;
import static pulse.tasks.logs.Status.DONE;
import static pulse.tasks.logs.Status.FAILED;
import static pulse.tasks.logs.Status.INCOMPLETE;
import static pulse.tasks.logs.Status.IN_PROGRESS;
import static pulse.tasks.logs.Status.READY;
import static pulse.tasks.logs.Status.TERMINATED;
import static pulse.tasks.processing.Buffer.getSize;
import static pulse.util.Reflexive.instantiate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import pulse.input.ExperimentalData;
import pulse.input.InterpolationDataset;
import pulse.math.ParameterVector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.direction.ActiveFlags;
import pulse.search.direction.IterativeState;
import pulse.search.statistics.CorrelationTest;
import pulse.search.statistics.NormalityTest;
import pulse.tasks.listeners.DataCollectionListener;
import pulse.tasks.listeners.StatusChangeListener;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.logs.CorrelationLogEntry;
import pulse.tasks.logs.DataLogEntry;
import pulse.tasks.logs.Details;
import pulse.tasks.logs.Log;
import pulse.tasks.logs.LogEntry;
import pulse.tasks.logs.StateEntry;
import pulse.tasks.logs.Status;
import pulse.tasks.processing.Buffer;
import pulse.tasks.processing.CorrelationBuffer;
import pulse.util.Accessible;

/**
 * A {@code SearchTask} is the most important class in {@code PULsE}. It
 * combines access to all other bits and can be executed by the
 * {@code TaskManager}. The execution consists in solving the reverse problem of
 * heat conduction, which is done using the {@code PathSolver}. A
 * {@code SearchTask} has an associated {@code ExperimentalData} object linked
 * to it.
 *
 * @see pulse.tasks.TaskManager
 */
public class SearchTask extends Accessible implements Runnable {

    private Calculation current;
    private List<Calculation> stored;
    private ExperimentalData curve;

    private IterativeState path;    //current sate
    private IterativeState best;    //best state 
    private Buffer buffer;
    private Log log;

    private CorrelationBuffer correlationBuffer;
    private CorrelationTest correlationTest;
    private NormalityTest normalityTest;

    private final Identifier identifier;
    /**
     * If {@code SearchTask} finishes, and its <i>R<sup>2</sup></i> value is
     * lower than this constant, the result will be considered
     * {@code AMBIGUOUS}.
     */
    private List<DataCollectionListener> listeners;
    private List<StatusChangeListener> statusChangeListeners;

    /**
     * <p>
     * Creates a new {@code SearchTask} from {@code curve}. Generates a new
     * {@code Identifier}, sets the parent of {@code curve} to {@code this}, and
     * invokes clear(). If any changes to the {@code ExperimentalData} occur, a
     * listener will ensure the {@code DifferenceScheme} is modified
     * accordingly.
     * </p>
     *
     * @param curve the {@code ExperimentalData}
     */
    public SearchTask(ExperimentalData curve) {
        this.statusChangeListeners = new CopyOnWriteArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        current = new Calculation(this);
        this.identifier = new Identifier();
        this.curve = curve;
        curve.setParent(this);
        correlationBuffer = new CorrelationBuffer();
        clear();
        addListeners();
    }
    
    /**
     * Update the best state. The instance of this class stores two objects
     * of the type IterativeState: the current state of the optimiser and
     * the global best state. Calling this method will check if a new global
     * best is found, and if so, this will store its parameters in the corresponding
     * variable. This will then be used at the final stage of running the search task,
     * comparing the converged result to the global best, and selecting whichever
     * has the lowest cost. Such routine is required due to the possibility of 
     * some optimisers going uphill.
     */
    
    public void storeState() {
        if(best == null || best.getCost() > path.getCost())
            best = new IterativeState(path);
    }

    private void addListeners() {
        InterpolationDataset.addListener(e -> {
            var p = current.getProblem().getProperties();
            if (p.areThermalPropertiesLoaded()) {
                p.useTheoreticalEstimates(curve);
            }
        });                

        /**
         * Sets the difference scheme's time limit to the upper bound of the
         * range of {@code ExperimentalData} multiplied by a safety margin
         * {@value Calculation.RELATIVE_TIME_MARGIN}.
         */
        curve.addDataListener(dataEvent -> {
            var scheme = current.getScheme();
            if (scheme != null) {
                var hcurve = current.getProblem().getHeatingCurve();
                var startTime = (double) hcurve.getTimeShift().getValue();
                scheme.setTimeLimit(
                        derive(TIME_LIMIT, Calculation.RELATIVE_TIME_MARGIN * curve.timeLimit() - startTime));
            }
        });
    }

    /**
     * <p>
     * Resets everything to default values (for a list of default values please
     * see the {@code .xml} document. Sets the status of this task to
     * {@code INCOMPLETE}. curve.addDataListener(dataEvent -> { var scheme =
     * current.getScheme(); if (scheme != null) { var curve =
     * current.getProblem().getHeatingCurve(); var startTime = (double)
     * curve.getTimeShift().getValue(); scheme.setTimeLimit(derive(TIME_LIMIT,
     * RELATIVE_TIME_MARGIN * curve.timeLimit() - startTime)); } });
     * </p>
     */
    public void clear() {
        stored = new ArrayList<Calculation>();
        curve.resetRanges();
        buffer = new Buffer();
        correlationBuffer.clear();
        buffer.setParent(this);
        log = new Log(this);

        initCorrelationTest();
        initNormalityTest();

        this.path = null;
        current.clear();

        this.checkProblems(true);
    }

    /**
     * This will use the current {@code DifferenceScheme} to solve the
     * {@code Problem} for this {@code SearchTask} and calculate the SSR value
     * showing how well (or bad) the calculated solution describes the
     * {@code ExperimentalData}.
     *
     * @return the value of SSR (sum of squared residuals).
     * @throws SolverException
     */
    public double solveProblemAndCalculateCost() throws SolverException {
        current.process();
        var rs = current.getOptimiserStatistic();
        rs.evaluate(this);
        return (double) rs.getStatistic().getValue();
    }

    public List<NumericProperty> alteredParameters() {
        return activeParameters(this).stream().map(key -> this.numericProperty(key)).collect(Collectors.toList());
    }

    /**
     * Generates a search vector (= optimisation vector) using the search flags
     * set by the {@code PathSolver}.
     *
     * @return an {@code IndexedVector} with search parameters of this
     * {@code SearchTaks}
     * @see pulse.search.direction.PathSolver.getSearchFlags()
     * @see pulse.problem.statements.Problem.optimisationVector(List<Flag>)
     */
    public ParameterVector searchVector() {
        var flags = ActiveFlags.getAllFlags();
        var keywords = activeParameters(this);
        var optimisationVector = new ParameterVector(keywords);

        current.getProblem().optimisationVector(optimisationVector, flags);
        curve.getRange().optimisationVector(optimisationVector, flags);

        return optimisationVector;
    }

    /**
     * Assigns the values of the parameters of this {@code SearchTask} to
     * {@code searchParameters}.
     *
     * @param searchParameters an {@code IndexedVector} with relevant search
     * parameters
     * @see pulse.problem.statements.Problem.assign(IndexedVector)
     */
    public void assign(ParameterVector searchParameters) {
        try {
            current.getProblem().assign(searchParameters);
            curve.getRange().assign(searchParameters);
        } catch (SolverException e) {
            var status = FAILED;
            status.setDetails(Details.PARAMETER_VALUES_NOT_SENSIBLE);
            setStatus(status);
            e.printStackTrace();
        }
    }

    /**
     * <p>
     * Runs this task if is either {@code READY} or {@code QUEUED}. Otherwise,
     * will do nothing. After making some preparatory steps, will initiate a
     * loop with successive calls to {@code PathSolver.iteration(this)}, filling
     * the buffer and notifying any data change listeners in parallel. This loop
     * will go on until either converging results are obtained, or a timeout is
     * reached, or if an execution error happens. Whether the run has been
     * successful will be determined by comparing the associated
     * <i>R</i><sup>2</sup> value with the {@code SUCCESS_CUTOFF}.
     * </p>
     */
    @Override
    public void run() {

        current.setResult(null);

        /* check of status */
        switch (current.getStatus()) {
            case READY:
            case QUEUED:
                setStatus(IN_PROGRESS);
                break;
            default:
                return;
        }

        /* preparatory steps */
        current.getProblem().parameterListChanged(); // get updated list of parameters

        var optimiser = getInstance();

        path = optimiser.initState(this);

        var errorTolerance = (double) optimiser.getErrorTolerance().getValue();
        int bufferSize = (Integer) getSize().getValue();
        buffer.init();
        correlationBuffer.clear();

        /* search cycle */

        /* sets an independent thread for manipulating the buffer */
        List<CompletableFuture<Void>> bufferFutures = new ArrayList<>(bufferSize);
        var singleThreadExecutor = Executors.newSingleThreadExecutor();

        try {
            solveProblemAndCalculateCost();
        } catch (SolverException e1) {
            System.err.println("Failed on first calculation. Details:");
            e1.printStackTrace();
        }

        final int maxIterations = (int) getInstance().getMaxIterations().getValue();

        outer:
        do {

            bufferFutures.clear();

            for (var i = 0; i < bufferSize; i++) {

                if (current.getStatus() != IN_PROGRESS) {
                    break outer;
                }

                int iter = 0;

                try {
                    for (boolean finished = false; !finished && iter < maxIterations; iter++) {
                        finished = optimiser.iteration(this);
                    }
                } catch (SolverException e) {
                    setStatus(FAILED);
                    System.err.println(this + " failed during execution. Details: ");
                    e.printStackTrace();
                    break outer;
                }

                if (iter >= maxIterations) {
                    var fail = FAILED;
                    fail.setDetails(MAX_ITERATIONS_REACHED);
                    setStatus(fail);
                }
                
                //if global best is better than the converged value
                if(best != null && best.getCost() < path.getCost()) {
                    //assign the global best parameters
                    assign(path.getParameters());
                    //and try to re-calculate
                    try {
                        solveProblemAndCalculateCost();
                    } catch (SolverException ex) {
                        Logger.getLogger(SearchTask.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                final var j = i;

                bufferFutures.add(CompletableFuture.runAsync(() -> {
                    buffer.fill(this, j);
                    correlationBuffer.inflate(this);
                    notifyDataListeners(new DataLogEntry(this));
                }, singleThreadExecutor));

            }

            bufferFutures.forEach(future -> future.join());

        } while (buffer.isErrorTooHigh(errorTolerance));

        singleThreadExecutor.shutdown();

        if (current.getStatus() == IN_PROGRESS) {
            runChecks();
        }

    }

    private void runChecks() {

        if (!normalityTest.test(this)) { // first, check if the residuals are normally-distributed
            var status = FAILED;
            status.setDetails(ABNORMAL_DISTRIBUTION_OF_RESIDUALS);
            setStatus(status);
        } else {

            var test = correlationBuffer.test(correlationTest); // second, check there are no unexpected
            // correlations
            notifyDataListeners(new CorrelationLogEntry(this));

            if (test) {
                var status = AMBIGUOUS;
                status.setDetails(SIGNIFICANT_CORRELATION_BETWEEN_PARAMETERS);
                setStatus(status);
            } else {
                // lastly, check if the parameter values estimated in this procedure are
                // reasonable

                var properties = alteredParameters();

                if (properties.stream().anyMatch(np -> !np.validate())) {
                    var status = FAILED;
                    status.setDetails(PARAMETER_VALUES_NOT_SENSIBLE);
                    setStatus(status);
                } else {
                    current.getModelSelectionCriterion().evaluate(this);
                    setStatus(DONE);
                }

            }

        }

    }

    public void addTaskListener(DataCollectionListener toAdd) {
        listeners.add(toAdd);
    }

    public void addStatusChangeListener(StatusChangeListener toAdd) {
        statusChangeListeners.add(toAdd);
    }

    public void removeTaskListeners() {
        listeners.clear();
    }

    public void removeStatusChangeListeners() {
        statusChangeListeners.clear();
    }

    @Override
    public String toString() {
        return getIdentifier().toString();
    }

    public ExperimentalData getExperimentalCurve() {
        return curve;
    }

    public IterativeState getIterativeState() {
        return path;
    }

    /**
     * Adopts the {@code curve} by this {@code SearchTask}.
     *
     * @param curve the {@code ExperimentalData}.
     */
    public void setExperimentalCurve(ExperimentalData curve) {
        this.curve = curve;

        if (curve != null) {
            curve.setParent(this);
        }

    }
    
    /**
     * Will return {@code true} if status could be updated. 
     * @param status the status of the task
     * @return {@code} true if status has been updated. {@code false} if 
     * the status was already set to {@code status} previously, or if it could 
     * not be updated at this time.
     * @see Calculation.setStatus()
     */

    public boolean setStatus(Status status) {
        Objects.requireNonNull(status);
        
        Status oldStatus = current.getStatus();
        boolean changed = current.setStatus(status) 
                && (oldStatus != current.getStatus());
        if (changed) {
            notifyStatusListeners(new StateEntry(this, status));
         }
        
        return changed;
    }

    /**
     * <p>
     * Checks if this {@code SearchTask} is ready to be run. Performs basic
     * check to see whether the user has uploaded all necessary data. If not,
     * will create a status update with information about the missing data.
     * </p>
     *
     * @return {@code READY} if the task is ready to be run, {@code DONE} if has
     * already been done previously, {@code INCOMPLETE} if some problems exist.
     * For the latter, additional details will be available using the
     * {@code status.getDetails()} method.
     * </p>
     */
    public void checkProblems(boolean updateStatus) {
        var status = current.getStatus();

        if (status == DONE) {
            return;
        }

        var pathSolver = getInstance();
        var s = INCOMPLETE;

        if (current.getProblem() == null) {
            s.setDetails(MISSING_PROBLEM_STATEMENT);
        } else if (!current.getProblem().isReady()) {
            s.setDetails(INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT);
        } else if (current.getScheme() == null) {
            s.setDetails(MISSING_DIFFERENCE_SCHEME);
        } else if (curve == null) {
            s.setDetails(MISSING_HEATING_CURVE);
        } else if (pathSolver == null) {
            s.setDetails(MISSING_OPTIMISER);
        } else if (buffer == null) {
            s.setDetails(MISSING_BUFFER);
        } else if (!getInstance().compatibleWith(current.getOptimiserStatistic())) {
            s.setDetails(INCOMPATIBLE_OPTIMISER);
        } else {
            s = READY;
        }

        if (updateStatus) {
            setStatus(s);
        }
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public Log getLog() {
        return log;
    }

    private void notifyDataListeners(LogEntry e) {
        for (var l : listeners) {
            l.onDataCollected(e);
        }
    }

    private void notifyStatusListeners(StateEntry e) {
        for (var l : statusChangeListeners) {
            l.onStatusChange(e);
        }
    }

    @Override
    public String describe() {

        var sb = new StringBuilder();
        sb.append(TaskManager.getManagerInstance().getSampleName());
        sb.append("_Task_");
        var extId = curve.getMetadata().getExternalID();
        if (extId < 0) {
            sb.append("IntID_" + identifier.getValue());
        } else {
            sb.append("ExtID_" + extId);
        }

        return sb.toString();

    }

    /**
     * If the current task is either {@code IN_PROGRESS}, {@code QUEUED}, or
     * {@code READY}, terminates it by setting its status to {@code TERMINATED}.
     * This change of status will then force the {@code run()} loop to stop (if
     * running).
     */
    public void terminate() {
        switch (current.getStatus()) {
            case IN_PROGRESS:
            case QUEUED:
            case READY:
                setStatus(TERMINATED);
                break;
            default:
                return;
        }
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        // intentionally left blank
    }

    /**
     * A {@code SearchTask} is deemed equal to another one if it has the same
     * {@code ExperimentalData}.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof SearchTask)) {
            return false;
        }

        return curve.equals(((SearchTask) o).getExperimentalCurve());

    }

    public NormalityTest getNormalityTest() {
        return normalityTest;
    }

    public void initNormalityTest() {
        normalityTest = instantiate(NormalityTest.class, NormalityTest.getSelectedTestDescriptor());
        normalityTest.setParent(this);
    }

    public void initCorrelationTest() {
        correlationTest = CorrelationTest.init();
        correlationTest.setParent(this);
    }

    public CorrelationBuffer getCorrelationBuffer() {
        return correlationBuffer;
    }

    public CorrelationTest getCorrelationTest() {
        return correlationTest;
    }

    public Calculation getCurrentCalculation() {
        return current;
    }

    public List<Calculation> getStoredCalculations() {
        return this.stored;
    }
    
    public void storeCalculation() {
        var copy = new Calculation(current);
        stored.add(copy);
    }

    public void switchTo(Calculation calc) {
        current.setParent(null);
        current = calc;
        current.setParent(this);
        var e = new TaskRepositoryEvent(TaskRepositoryEvent.State.TASK_MODEL_SWITCH, this.getIdentifier());
        fireRepositoryEvent(e);
    }
    
    /**
     * Finds the best calculation by comparing those already stored by their
     * model selection statistics.
     * @return the calculation showing the optimal value of the model selection statistic.
     */
    
    public Calculation findBestCalculation() {
        var c = stored.stream().reduce((c1, c2) -> c1.compareTo(c2) > 0 ? c2 : c1);
        return c.isPresent() ? c.get() : null;
    }

    public void switchToBestModel() {
        this.switchTo(findBestCalculation());
        var e = new TaskRepositoryEvent(TaskRepositoryEvent.State.BEST_MODEL_SELECTED, this.getIdentifier());
        fireRepositoryEvent(e);
    }

    private void fireRepositoryEvent(TaskRepositoryEvent e) {
        var instance = TaskManager.getManagerInstance();
        for (var l : instance.getTaskRepositoryListeners()) {
            l.onTaskListChanged(e);
        }
    }

}