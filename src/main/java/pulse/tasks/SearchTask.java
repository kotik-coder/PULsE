package pulse.tasks;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.TIME_LIMIT;
import static pulse.search.direction.PathOptimiser.getInstance;
import static pulse.tasks.logs.Details.ABNORMAL_DISTRIBUTION_OF_RESIDUALS;
import static pulse.tasks.logs.Details.INCOMPATIBLE_OPTIMISER;
import static pulse.tasks.logs.Details.INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT;
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
import static pulse.util.Reflexive.instantiate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import pulse.input.ExperimentalData;
import pulse.input.InterpolationDataset;
import pulse.math.ParameterIdentifier;
import pulse.math.ParameterVector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.GeneralTask;
import pulse.search.direction.ActiveFlags;
import pulse.search.statistics.CorrelationTest;
import pulse.search.statistics.NormalityTest;
import pulse.tasks.listeners.DataCollectionListener;
import pulse.tasks.listeners.StatusChangeListener;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.logs.CorrelationLogEntry;
import pulse.tasks.logs.DataLogEntry;
import pulse.tasks.logs.Log;
import pulse.tasks.logs.LogEntry;
import pulse.tasks.logs.StateEntry;
import pulse.tasks.logs.Status;
import pulse.tasks.processing.CorrelationBuffer;
import static pulse.tasks.logs.Status.AWAITING_TERMINATION;

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
public class SearchTask extends GeneralTask {

    private Calculation current;
    private List<Calculation> stored;
    private ExperimentalData curve;
    private Log log;

    private final CorrelationBuffer correlationBuffer;
    private CorrelationTest correlationTest;
    private NormalityTest normalityTest;

    private final Identifier identifier;
    /**
     * If {@code SearchTask} finishes, and its <i>R<sup>2</sup></i> value is
     * lower than this constant, the result will be considered
     * {@code AMBIGUOUS}.
     */
    private final List<DataCollectionListener> listeners;
    private final List<StatusChangeListener> statusChangeListeners;

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
        super();
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
    
    private void addListeners() {
        InterpolationDataset.addListener(e -> {
            if (current.getProblem() != null) {
                var p = current.getProblem().getProperties();
                if (p.areThermalPropertiesLoaded()) {
                    p.useTheoreticalEstimates(curve);
                }
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
        stored = new ArrayList<>();
        curve.resetRanges();
        correlationBuffer.clear();
        log = new Log(this);

        initCorrelationTest();
        initNormalityTest();

        //this.path = null;
        current.clear();

        this.checkProblems(true);
    }
    
    public List<NumericProperty> alteredParameters() {
        return activeParameters().stream().map(key -> 
                this.numericProperty(key)).collect(Collectors.toList());
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
     * <p>
     * Checks if this {@code SearchTask} is ready to be run.Performs basic check
     * to see whether the user has uploaded all necessary data. If not, will
     * create a status update with information about the missing data.
     * </p>
     *
     * Status will be set to {@code READY} if the task is ready to be run,
     * {@code DONE} if has already been done previously, {@code INCOMPLETE} if
     * some problems exist. For the latter, additional details will be available
     * using the {@code status.getDetails()} method.
     * </p>
     *
     * @param updateStatus
     */
    public void checkProblems(boolean updateStatus) {
        var status = getStatus();

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
        } else if (getBuffer() == null) {
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
    public void run() {
        correlationBuffer.clear();
        current.setResult(null);
        
        /* check of status */
        switch (getStatus()) {
            case READY:
            case QUEUED:
                setStatus(IN_PROGRESS);
                break;
            default:
                return;
        }
        
        current.getProblem().parameterListChanged(); // get updated list of parameters
        setDefaultOptimiser();
                    
        super.run();
    }
    
    /**
     * If the current task is either {@code IN_PROGRESS}, {@code QUEUED}, or
     * {@code READY}, terminates it by setting its status to {@code TERMINATED}.
     * This change of status will then force the {@code run()} loop to stop (if
     * running).
     */
    public void terminate() {
        switch (getStatus()) {
            case IN_PROGRESS:
            case QUEUED:
                setStatus(AWAITING_TERMINATION);
                break;
            default:
        }
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
    
    public List<Calculation> getStoredCalculations() {
        return this.stored;
    }    

    public void storeCalculation() {
        var copy = new Calculation(current);
        stored.add(copy);
    }   

    public void switchTo(Calculation calc) {
        current = calc;
        current.setParent(this);
        var e = new TaskRepositoryEvent(TaskRepositoryEvent.State.TASK_MODEL_SWITCH, this.getIdentifier());
        fireRepositoryEvent(e);
    }

    /**
     * Finds the best calculation by comparing those already stored by their
     * model selection statistics.
     *
     * @return the calculation showing the optimal value of the model selection
     * statistic.
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
    
    @Override
    public boolean isInProgress() {
        return getStatus() == IN_PROGRESS;
    }
   
    @Override
    public void intermediateProcessing() {
        correlationBuffer.inflate(this);
        notifyDataListeners(new DataLogEntry(this));
    }
    
    @Override 
    public void onSolverException(SolverException e) {
        setStatus(Status.troubleshoot(e));
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
    @Override
    public ParameterVector searchVector() {
        var ids = activeParameters().stream().map(id -> 
                new ParameterIdentifier(id)).collect(Collectors.toList());
        var optimisationVector = new ParameterVector(ids);

        current.getProblem().optimisationVector(optimisationVector);
        curve.getRange().optimisationVector(optimisationVector);

        return optimisationVector;
    }

    /**
     * Assigns the values of the parameters of this {@code SearchTask} to
     * {@code searchParameters}.
     *
     * @param searchParameters an {@code IndexedVector} with relevant search
     * parameters
     * @throws pulse.problem.schemes.solvers.SolverException
     * @see pulse.problem.statements.Problem.assign(IndexedVector)
     */
    @Override
    public void assign(ParameterVector searchParameters) throws SolverException {
        current.getProblem().assign(searchParameters);
        curve.getRange().assign(searchParameters);
    }

    @Override
    public void postProcessing() {

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

                var properties = this.getIterativeState().getParameters();

                if (properties.findMalformedElements().size() > 0) {
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
    
    
    /**
     * Finds what properties are being altered in the search of this SearchTask.
     *
     * @return a {@code List} of property types represented by
     * {@code NumericPropertyKeyword}s
     */
    @Override
    public List<NumericPropertyKeyword> activeParameters() {
        var flags = ActiveFlags.getAllFlags();
        //problem dependent
        var allActiveParams = ActiveFlags.selectActiveAndListed
                (flags, current.getProblem()); 
        //problem independent (lower/upper bound)
        var listed = ActiveFlags.selectActiveAndListed
                (flags, curve.getRange() );
        allActiveParams.addAll(listed);          
        return allActiveParams;
    }
    
        /**
     * Will return {@code true} if status could be updated.
     *
     * @param status the status of the task
     * @return {@code} true if status has been updated. {@code false} if the
     * status was already set to {@code status} previously, or if it could not
     * be updated at this time.
     * @see Calculation.setStatus()
     */
    
    public boolean setStatus(Status status) {
        Objects.requireNonNull(status);

        Status oldStatus = getStatus();
        boolean changed = current.setStatus(status)
                && (oldStatus != getStatus());
        if (changed) {
            notifyStatusListeners(new StateEntry(this, status));
        }

        return changed;
    }
    
    public Status getStatus() {
        return current.getStatus();
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

        return curve.equals(((SearchTask) o).curve);

    }
    
    @Override
    public String describe() {

        var sb = new StringBuilder();
        sb.append(TaskManager.getManagerInstance().getSampleName());
        sb.append("_Task_");
        var extId = curve.getMetadata().getExternalID();
        if (extId < 0) {
            sb.append("IntID_").append(identifier.getValue());
        } else {
            sb.append("ExtID_").append(extId);
        }

        return sb.toString();

    }

    @Override
    public ExperimentalData getInput() {
        return curve;
    }

    @Override
    public Calculation getResponse() {
        return current;
    }

}