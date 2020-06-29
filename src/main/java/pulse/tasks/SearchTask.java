package pulse.tasks;

import static pulse.input.InterpolationDataset.getDataset;
import static pulse.input.listeners.DataEventType.CHANGE_OF_ORIGIN;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;
import static pulse.properties.NumericPropertyKeyword.TIME_LIMIT;
import static pulse.search.direction.PathOptimiser.activeParameters;
import static pulse.search.direction.PathOptimiser.getAllFlags;
import static pulse.search.direction.PathOptimiser.getErrorTolerance;
import static pulse.search.direction.PathOptimiser.getLinearSolver;
import static pulse.search.direction.PathOptimiser.getSelectedPathOptimiser;
import static pulse.search.statistics.ResidualStatistic.getSelectedOptimiserDescriptor;
import static pulse.tasks.Buffer.getSize;
import static pulse.tasks.Status.AMBIGUOUS;
import static pulse.tasks.Status.DONE;
import static pulse.tasks.Status.FAILED;
import static pulse.tasks.Status.INCOMPLETE;
import static pulse.tasks.Status.IN_PROGRESS;
import static pulse.tasks.Status.READY;
import static pulse.tasks.Status.TERMINATED;
import static pulse.tasks.Status.Details.ABNORMAL_DISTRIBUTION_OF_RESIDUALS;
import static pulse.tasks.Status.Details.INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT;
import static pulse.tasks.Status.Details.MISSING_BUFFER;
import static pulse.tasks.Status.Details.MISSING_DIFFERENCE_SCHEME;
import static pulse.tasks.Status.Details.MISSING_HEATING_CURVE;
import static pulse.tasks.Status.Details.MISSING_LINEAR_SOLVER;
import static pulse.tasks.Status.Details.MISSING_PATH_SOLVER;
import static pulse.tasks.Status.Details.MISSING_PROBLEM_STATEMENT;
import static pulse.tasks.Status.Details.NONE;
import static pulse.tasks.Status.Details.PARAMETER_VALUES_NOT_SENSIBLE;
import static pulse.tasks.Status.Details.SIGNIFICANT_CORRELATION_BETWEEN_PARAMETERS;
import static pulse.tasks.TaskManager.getSampleName;
import static pulse.util.Reflexive.instantiate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import pulse.input.ExperimentalData;
import pulse.input.InterpolationDataset;
import pulse.input.InterpolationDataset.StandartType;
import pulse.input.Metadata;
import pulse.math.IndexedVector;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.Solver;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.direction.Path;
import pulse.search.statistics.CorrelationTest;
import pulse.search.statistics.NormalityTest;
import pulse.search.statistics.RSquaredTest;
import pulse.search.statistics.ResidualStatistic;
import pulse.search.statistics.SumOfSquares;
import pulse.tasks.listeners.DataCollectionListener;
import pulse.tasks.listeners.StatusChangeListener;
import pulse.ui.components.PropertyHolderTable;
import pulse.util.Accessible;
import pulse.util.PropertyEvent;

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

	private Problem problem;
	private DifferenceScheme scheme;
	private ExperimentalData curve;
	private ResidualStatistic rs;

	private Path path;
	private Buffer buffer;
	private CorrelationBuffer correlationBuffer;
	private Log log;
	private CorrelationTest correlationTest;

	private Identifier identifier;
	private Status status = INCOMPLETE;

	private double testTemperature;
	private double cp, rho;

	private NormalityTest normalityTest;

	private final static double RELATIVE_TIME_MARGIN = 1.01;

	/**
	 * If {@code SearchTask} finishes, and its <i>R<sup>2</sup></i> value is lower
	 * than this constant, the result will be considered {@code AMBIGUOUS}.
	 */

	private List<DataCollectionListener> listeners = new CopyOnWriteArrayList<DataCollectionListener>();
	private List<StatusChangeListener> statusChangeListeners = new CopyOnWriteArrayList<StatusChangeListener>();

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
		this.identifier = new Identifier();
		this.curve = curve;
		curve.setParent(this);
		correlationBuffer = new CorrelationBuffer();
		clear();
	}

	/**
	 * <p>
	 * Resets everything to default values (for a list of default values please see
	 * the {@code .xml} document. Sets the status of this task to
	 * {@code INCOMPLETE}.
	 * </p>
	 */

	public void clear() {
		curve.resetRanges();
		buffer = new Buffer();
		correlationBuffer.clear();
		buffer.setParent(this);
		log = new Log(this);

		initOptimiser();
		initCorrelationTest();
		initNormalityTest();

		testTemperature = (double) curve.getMetadata().getTestTemperature().getValue();

		calculateThermalProperties();

		this.path = null;
		this.problem = null;
		this.scheme = null;

		setStatus(INCOMPLETE);

		curve.addDataListener(dataEvent -> {
			if (scheme != null) {
				var startTime = (double) problem.getHeatingCurve().getTimeShift().getValue();
				scheme.setTimeLimit(derive(TIME_LIMIT, RELATIVE_TIME_MARGIN * curve.timeLimit() - startTime));
			}
		});

	}

	public List<NumericProperty> alteredParameters() {
		return activeParameters(this).stream().map(key -> this.numericProperty(key))
				.collect(Collectors.toList());
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

		problem.optimisationVector(array, flags);
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
		problem.assign(searchParameters);
		curve.getRange().assign(searchParameters);
	}

	/**
	 * Calculates some or all of the following properties:
	 * <math><i>C</i><sub>p</sub>, <i>&rho;</i>, <i>&labmda;</i>,
	 * <i>&epsilon;</i></math>.
	 * <p>
	 * These properties will be calculated only if the necessary
	 * {@code InterpolationDataset}s were previously loaded by the
	 * {@code TaskManager}.
	 * </p>
	 */

	public void calculateThermalProperties() {

		if (problem == null)
			return;

		var cpCurve = getDataset(StandartType.SPECIFIC_HEAT);

		if (cpCurve != null) {
			cp = cpCurve.interpolateAt(testTemperature);
			problem.set(NumericPropertyKeyword.SPECIFIC_HEAT, 
                                derive(NumericPropertyKeyword.SPECIFIC_HEAT, cp));
		}

		var rhoCurve = getDataset(StandartType.DENSITY);

		if (rhoCurve != null) {
			rho = rhoCurve.interpolateAt(testTemperature);
			problem.set(NumericPropertyKeyword.DENSITY, 
                                derive(NumericPropertyKeyword.DENSITY, rho));
		}

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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public double solveProblemAndCalculateDeviation() {
		try {
			((Solver) scheme).solve(problem);
		} catch (SolverException e) {
			status = FAILED;
			System.err.println("Solver of " + this + " has encountered an error. Details: ");
			e.printStackTrace();
		}
		rs.evaluate(this);
		return (double) rs.getStatistic().getValue();
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

		/* check of status */

		switch (status) {
		case READY:
		case QUEUED:
			setStatus(IN_PROGRESS);
			break;
		default:
			return;
		}

		/* preparatory steps */

		solveProblemAndCalculateDeviation();

		var pathSolver = getSelectedPathOptimiser();

		path = pathSolver.createPath(this);

		var errorTolerance = (double) getErrorTolerance().getValue();
		int bufferSize = (Integer) getSize().getValue();
		buffer.clear();
		correlationBuffer.clear();

		/* search cycle */

		/* sets an independent thread for manipulating the buffer */

		List<CompletableFuture<Void>> bufferFutures = new ArrayList<>(bufferSize);
		var singleThreadExecutor = Executors.newSingleThreadExecutor();

		outer: do {

			bufferFutures.clear();

			for (var i = 0; i < bufferSize; i++) {

				if (status != IN_PROGRESS)
					break outer;

				try {
					pathSolver.iteration(this);
				} catch (SolverException e) {
					status = FAILED;
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

		if (status == IN_PROGRESS)
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
				else
					setStatus(DONE);

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

	public Problem getProblem() {
		return problem;
	}

	public DifferenceScheme getScheme() {
		return scheme;
	}

	public ExperimentalData getExperimentalCurve() {
		return curve;
	}

	public NumericProperty getTestTemperature() {
		return derive(TEST_TEMPERATURE, testTemperature);
	}

	public Path getPath() {
		return path;
	}

	/**
	 * <p>
	 * After setting and adopting the {@code problem} by this {@code SearchTask},
	 * this will attempt to change the parameters of that {@code problem} in
	 * accordance with the loaded {@code ExperimentalData} for this
	 * {@code SearchTask} (if not null). Later, if any changes to the properties of
	 * that {@code Problem} occur and if the source of that event is either the
	 * {@code Metadata} or the {@code PropertyHolderTable}, they will be accounted
	 * for by altering the parameters of the {@code problem} accordingly --
	 * immediately after the former take place.
	 * </p>
	 * 
	 * @param problem a {@code Problem}
	 */

	public void setProblem(Problem problem) {
		if (curve == null || problem == null)
			return;

		this.problem = problem;
		problem.setParent(this);
		problem.removeListeners();
		problem.retrieveData(curve);

		problem.addListener((PropertyEvent event) -> {
                    if (event.getSource() instanceof Metadata) {
                        problem.estimateSignalRange(curve);
                        problem.useTheoreticalEstimates(curve);
                    } else if (event.getSource() instanceof InterpolationDataset) {
                        problem.useTheoreticalEstimates(curve);
                    } else if (event.getSource() instanceof PropertyHolderTable) {
                        problem.estimateSignalRange(curve);
                    }
        });

		problem.getHeatingCurve().addDataListener(dataEvent -> {

			var event = dataEvent.getType();

			if (event == CHANGE_OF_ORIGIN) {
				var upperLimitUpdated = RELATIVE_TIME_MARGIN * curve.timeLimit()
						- (double) problem.getHeatingCurve().getTimeShift().getValue();
				scheme.setTimeLimit(derive(TIME_LIMIT, upperLimitUpdated));
			}

		});

	}

	/**
	 * Adopts the {@code scheme} by this {@code SearchTask} and updates the time
	 * limit of {@scheme} to match {@code ExperimentalData}.
	 * 
	 * @param scheme the {@code DiffenceScheme}.
	 */

	public void setScheme(DifferenceScheme scheme) {
		this.scheme = scheme;
		if (scheme != null && problem != null) {
			scheme.setParent(this);

			var upperLimit = RELATIVE_TIME_MARGIN * curve.timeLimit()
					- (double) problem.getHeatingCurve().getTimeShift().getValue();

			scheme.setTimeLimit(derive(TIME_LIMIT, upperLimit));

		}
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

	/**
	 * Sets the test temperature and modifies, if needed, any of the thermal
	 * properties that depend on this parameter.
	 * 
	 * @param testTemperature the test temperature
	 */

	public void setTestTemperature(NumericProperty testTemperature) {
		this.testTemperature = (double) testTemperature.getValue();
		calculateThermalProperties();
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status, Status.Details details) {
		if (this.status == status)
			return;

		this.status = status;
		status.setDetails(details);
		notifyStatusListeners(new StateEntry(this, status));
	}

	/**
	 * Sets a new {@code status} to this {@code SearchTask} and informs the
	 * listeners.
	 * 
	 * @param status the new status
	 */

	public void setStatus(Status status) {
		if (this.status == status)
			return;

		this.status = status;
		status.setDetails(NONE);
		notifyStatusListeners(new StateEntry(this, status));
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
		if (status == DONE)
			return status;

		var pathSolver = getSelectedPathOptimiser();
		var s = INCOMPLETE;

		if (problem == null)
			s.setDetails(MISSING_PROBLEM_STATEMENT);
		else if (!problem.allDetailsPresent())
			s.setDetails(INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT);
		else if (scheme == null)
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
		sb.append(getSampleName());
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
		switch (status) {
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

	public ResidualStatistic getResidualStatistic() {
		return rs;
	}

	public void setResidualStatistic(ResidualStatistic rs) {
		this.rs = rs;
		rs.setParent(this);
	}

	public void initNormalityTest() {
		normalityTest = instantiate(NormalityTest.class, 
                        NormalityTest.getSelectedTestDescriptor());

		if (normalityTest instanceof RSquaredTest && rs instanceof SumOfSquares)
			((RSquaredTest) normalityTest).setSumOfSquares((SumOfSquares) rs);

		normalityTest.setParent(this);
	}

	public void initOptimiser() {
		rs = instantiate(ResidualStatistic.class, getSelectedOptimiserDescriptor());
		rs.setParent(this);
	}

	public void initCorrelationTest() {
		correlationTest = instantiate(CorrelationTest.class, 
                        CorrelationTest.getSelectedTestDescriptor());
		correlationTest.setParent(this);
	}

	public CorrelationBuffer getCorrelationBuffer() {
		return correlationBuffer;
	}

	public CorrelationTest getCorrelationTest() {
		return correlationTest;
	}

}