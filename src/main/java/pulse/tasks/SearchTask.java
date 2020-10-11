package pulse.tasks;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.TIME_LIMIT;
import static pulse.search.direction.ActiveFlags.activeParameters;
import static pulse.search.direction.ActiveFlags.getAllFlags;
import static pulse.search.direction.PathOptimiser.getErrorTolerance;
import static pulse.search.direction.PathOptimiser.getInstance;
import static pulse.search.direction.PathOptimiser.getLinearSolver;
import static pulse.tasks.logs.Details.ABNORMAL_DISTRIBUTION_OF_RESIDUALS;
import static pulse.tasks.logs.Details.INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT;
import static pulse.tasks.logs.Details.MISSING_BUFFER;
import static pulse.tasks.logs.Details.MISSING_DIFFERENCE_SCHEME;
import static pulse.tasks.logs.Details.MISSING_HEATING_CURVE;
import static pulse.tasks.logs.Details.MISSING_LINEAR_SOLVER;
import static pulse.tasks.logs.Details.MISSING_PATH_SOLVER;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import pulse.input.ExperimentalData;
import pulse.input.InterpolationDataset;
import pulse.math.IndexedVector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.direction.Path;
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

	private Path path;
	private Buffer buffer;
	private Log log;

	private CorrelationBuffer correlationBuffer;
	private CorrelationTest correlationTest;
	private NormalityTest normalityTest;
	
	private Identifier identifier;

	/**
	 * If {@code SearchTask} finishes, and its <i>R<sup>2</sup></i> value is lower
	 * than this constant, the result will be considered {@code AMBIGUOUS}.
	 */

	private List<DataCollectionListener> listeners = new CopyOnWriteArrayList<>();
	private List<StatusChangeListener> statusChangeListeners = new CopyOnWriteArrayList<>();
	
	/**
	 * <p>
	 * Creates a new {@code SearchTask} from {@code curve}. Generates a new
	 * {@code Identifier}, sets the parent of {@code curve} to {@code this}, and
	 * invokes clear(). If any changes to the {@code ExperimentalData} occur, a
	 * listener will ensure the {@code DifferenceScheme} is modified accordingly.
	 * </p>
	 * 
	 * @param curve the {@code ExperimentalData}
	 */

	public SearchTask(ExperimentalData curve) {
		current = new Calculation();
		current.setParent(this);
		this.identifier = new Identifier();
		this.curve = curve;
		curve.setParent(this);
		correlationBuffer = new CorrelationBuffer();
		clear();
		addListeners();
	}
	
	private void addListeners() {
		InterpolationDataset.addListener(e -> {
			var p = current.getProblem().getProperties(); 
			if(p.areThermalPropertiesLoaded())
				p.useTheoreticalEstimates(curve);
		});

		curve.addDataListener(dataEvent -> {
			var scheme = current.getScheme();
			if (scheme != null) {
				var hcurve = current.getProblem().getHeatingCurve();
				var startTime = (double) hcurve.getTimeShift().getValue();
				scheme.setTimeLimit(derive(TIME_LIMIT, Calculation.RELATIVE_TIME_MARGIN * curve.timeLimit() - startTime));
			}
		});
	}

	/**
	 * <p>
	 * Resets everything to default values (for a list of default values please see
	 * the {@code .xml} document. Sets the status of this task to
	 * {@code INCOMPLETE}.		curve.addDataListener(dataEvent -> {
			var scheme = current.getScheme();
			if (scheme != null) {
				var curve = current.getProblem().getHeatingCurve();
				var startTime = (double) curve.getTimeShift().getValue();
				scheme.setTimeLimit(derive(TIME_LIMIT, RELATIVE_TIME_MARGIN * curve.timeLimit() - startTime));
			}
		});
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
		
		this.checkProblems();
		this.notifyStatusListeners(new StateEntry(this, current.getStatus()));
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

	public double solveProblemAndCalculateDeviation() {
		current.process();
		var rs = current.getOptimiserStatistic();
		rs.evaluate(this);
		return (double) rs.getStatistic().getValue();
	}

	public List<NumericProperty> alteredParameters() {
		return activeParameters(this).stream().map(key -> this.numericProperty(key)).collect(Collectors.toList());
	}

	/**
	 * Generates a search vector (= optimisation vector) using the search flags set
	 * by the {@code PathSolver}.
	 * 
	 * @return an {@code IndexedVector} with search parameters of this
	 *         {@code SearchTaks}
	 * @see pulse.search.direction.PathSolver.getSearchFlags()
	 * @see pulse.problem.statements.Problem.optimisationVector(List<Flag>)
	 */

	public IndexedVector[] searchVector() {
		var flags = getAllFlags();
		var keywords = activeParameters(this);
		var optimisationVector = new IndexedVector(keywords);
		var upperBound = new IndexedVector(optimisationVector.getIndices());

		var array = new IndexedVector[] { optimisationVector, upperBound };

		current.getProblem().optimisationVector(array, flags);
		curve.getRange().optimisationVector(array, flags);

		return array;

	}

	/**
	 * Assigns the values of the parameters of this {@code SearchTask} to
	 * {@code searchParameters}.
	 * 
	 * @param searchParameters an {@code IndexedVector} with relevant search
	 *                         parameters
	 * @see pulse.problem.statements.Problem.assign(IndexedVector)
	 */

	public void assign(IndexedVector searchParameters) {
		current.getProblem().assign(searchParameters);
		curve.getRange().assign(searchParameters);
	}

	/**
	 * <p>
	 * Runs this task if is either {@code READY} or {@code QUEUED}. Otherwise, will
	 * do nothing. After making some preparatory steps, will initiate a loop with
	 * successive calls to {@code PathSolver.iteration(this)}, filling the buffer
	 * and notifying any data change listeners in parallel. This loop will go on
	 * until either converging results are obtained, or a timeout is reached, or if
	 * an execution error happens. Whether the run has been successful will be
	 * determined by comparing the associated <i>R</i><sup>2</sup> value with the
	 * {@code SUCCESS_CUTOFF}.
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
		current.process();

		var pathSolver = getInstance();

		path = pathSolver.createPath(this);

		var errorTolerance = (double) getErrorTolerance().getValue();
		int bufferSize = (Integer) getSize().getValue();
		buffer.init();
		correlationBuffer.clear();

		/* search cycle */

		/* sets an independent thread for manipulating the buffer */

		List<CompletableFuture<Void>> bufferFutures = new ArrayList<>(bufferSize);
		var singleThreadExecutor = Executors.newSingleThreadExecutor();

		outer: do {

			bufferFutures.clear();

			for (var i = 0; i < bufferSize; i++) {

				if (current.getStatus() != IN_PROGRESS)
					break outer;

				try {
					pathSolver.iteration(this);
				} catch (SolverException e) {
					setStatus(FAILED);
					System.err.println(this + " failed during execution. Details: ");
					e.printStackTrace();
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

		if (current.getStatus() == IN_PROGRESS)
			runChecks();

	}

	private void runChecks() {

		if (!normalityTest.test(this)) // first, check if the residuals are normally-distributed
			setStatus(FAILED, ABNORMAL_DISTRIBUTION_OF_RESIDUALS);

		else {

			var test = correlationBuffer.test(correlationTest); // second, check there are no unexpected
																// correlations
			notifyDataListeners(new CorrelationLogEntry(this));

			if (test)
				setStatus(AMBIGUOUS, SIGNIFICANT_CORRELATION_BETWEEN_PARAMETERS);
			else {
				// lastly, check if the parameter values estimated in this procedure are
				// reasonable

				var properties = alteredParameters();

				if (properties.stream().anyMatch(np -> !np.validate()))
					setStatus(FAILED, PARAMETER_VALUES_NOT_SENSIBLE);
				else {
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

	public Path getPath() {
		return path;
	}

	/**
	 * Adopts the {@code curve} by this {@code SearchTask}.
	 * 
	 * @param curve the {@code ExperimentalData}.
	 */

	public void setExperimentalCurve(ExperimentalData curve) {
		this.curve = curve;

		if (curve != null)
			curve.setParent(this);

	}
	
	public void setStatus(Status status, Details details) {
		status.setDetails(details);
		if(current.setStatus(status, details)) 
			notifyStatusListeners(new StateEntry(this, status));
	}
	
	/**
	 * Sets a new {@code status} to this {@code SearchTask} and informs the
	 * listeners.
	 * 
	 * @param status the new status
	 */

	public void setStatus(Status status) {
		setStatus(status, Details.NONE);
	}

	public Status checkProblems() {
		return checkProblems(true);
	}

	/**
	 * <p>
	 * Checks if this {@code SearchTask} is ready to be run. Performs basic check to
	 * see whether the user has uploaded all necessary data. If not, will create a
	 * status update with information about the missing data.
	 * </p>
	 * 
	 * @return {@code READY} if the task is ready to be run, {@code DONE} if has
	 *         already been done previously, {@code INCOMPLETE} if some problems
	 *         exist. For the latter, additional details will be available using the
	 *         {@code status.getDetails()} method.
	 *         </p>
	 * @return the current status
	 */

	public Status checkProblems(boolean updateStatus) {
		var status = current.getStatus();
		if (status == DONE)
			return status;

		var pathSolver = getInstance();
		var s = INCOMPLETE;

		if (current.getProblem() == null)
			s.setDetails(MISSING_PROBLEM_STATEMENT);
		else if (!current.getProblem().isReady())
			s.setDetails(INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT);
		else if (current.getScheme() == null)
			s.setDetails(MISSING_DIFFERENCE_SCHEME);
		else if (curve == null)
			s.setDetails(MISSING_HEATING_CURVE);
		else if (pathSolver == null)
			s.setDetails(MISSING_PATH_SOLVER);
		else if (getLinearSolver() == null)
			s.setDetails(MISSING_LINEAR_SOLVER);
		else if (buffer == null)
			s.setDetails(MISSING_BUFFER);
		else
			s = READY;

		if (updateStatus)
			setStatus(s);

		return status;
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
		if (extId < 0)
			sb.append("IntID_" + identifier.getValue());
		else
			sb.append("ExtID_" + extId);

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
		if (o == this)
			return true;

		if (!(o instanceof SearchTask))
			return false;

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
		correlationTest = instantiate(CorrelationTest.class, CorrelationTest.getSelectedTestDescriptor());
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
	
	public void switchTo(Calculation calc) {
		current.setParent(null);
		current = calc;
		current.setParent(this);
		var e = new TaskRepositoryEvent(TaskRepositoryEvent.State.TASK_MODEL_SWITCH, this.getIdentifier());
		fireRepositoryEvent(e);
	}
	
	public void switchToBestModel() {
		var best = stored.stream().reduce((c1, c2) -> c1.compareTo(c2) > 0 ? c2 : c1);
		this.switchTo(best.get());		
		var e = new TaskRepositoryEvent(TaskRepositoryEvent.State.BEST_MODEL_SELECTED, this.getIdentifier());
		fireRepositoryEvent(e);
	}
	
	private void fireRepositoryEvent(TaskRepositoryEvent e) {
		var instance = TaskManager.getManagerInstance();
		for(var l : instance.getTaskRepositoryListeners())
			l.onTaskListChanged(e);
	}

}