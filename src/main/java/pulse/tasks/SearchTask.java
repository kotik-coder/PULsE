package pulse.tasks;

import static pulse.properties.NumericPropertyKeyword.CONDUCTIVITY;
import static pulse.properties.NumericPropertyKeyword.DENSITY;
import static pulse.properties.NumericPropertyKeyword.EMISSIVITY;
import static pulse.properties.NumericPropertyKeyword.RSQUARED;
import static pulse.properties.NumericPropertyKeyword.SPECIFIC_HEAT;
import static pulse.properties.NumericPropertyKeyword.SUM_OF_SQUARES;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;
import static pulse.properties.NumericPropertyKeyword.TIME_LIMIT;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.input.InterpolationDataset;
import pulse.input.Metadata;
import pulse.input.listeners.DataEventType;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.Solver;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.direction.Path;
import pulse.search.direction.PathSolver;
import pulse.search.math.IndexedVector;
import pulse.tasks.Status.Details;
import pulse.tasks.listeners.DataCollectionListener;
import pulse.tasks.listeners.StatusChangeListener;
import pulse.tasks.listeners.TaskStateEvent;
import pulse.ui.components.PropertyHolderTable;
import pulse.util.Accessible;
import pulse.util.PropertyEvent;
import pulse.util.PropertyHolderListener;
import pulse.util.SaveableCategory;

/**
 * A {@code SearchTask} is the most important class in {@code PULsE}. It combines access to all 
 * other bits and can be executed by the {@code TaskManager}. The execution consists in solving the reverse problem
 * of heat conduction, which is done using the {@code PathSolver}. A {@code SearchTask} has an associated
 * {@code ExperimentalData} object linked to it. 
 * @see pulse.tasks.TaskManager
 */

public class SearchTask extends Accessible implements Runnable, SaveableCategory {

	private Problem problem;
	private DifferenceScheme scheme;
	private ExperimentalData curve;

	private Path   path;
	private Buffer buffer;
	private Log	   log;
	
	private Identifier identifier;
	private Status status = Status.INCOMPLETE;
			
	private double testTemperature;
	private double cp, rho, emissivity, lambda;
	private double rSq, ssr;
	
	private final static double RELATIVE_TIME_MARGIN = 1.01;

	/**
	 * If {@code SearchTask} finishes, and its <i>R<sup>2</sup></i> value is lower than this constant,
	 * the result will be considered {@code AMBIGUOUS}. 
	 */
	
	public final static double SUCCESS_CUTOFF = 0.2;
		
	private List<DataCollectionListener> listeners			 = new CopyOnWriteArrayList<DataCollectionListener>();
	private List<StatusChangeListener> statusChangeListeners = new CopyOnWriteArrayList<StatusChangeListener>();

	/**
	 * <p>Creates a new {@code SearchTask} from {@code curve}. Generates a new {@code Identifier}, sets the parent of {@code curve} to {@code this}, 
	 * and invokes clear(). If any changes to the {@code ExperimentalData} occur, a listener will ensure the {@code DifferenceScheme} is modified accordingly.</p>
	 * @param curve the {@code ExperimentalData}
	 */
	
	public SearchTask(ExperimentalData curve) {
		this.identifier = new Identifier();		
		this.curve = curve;
		curve.setParent(this);		
		clear();	
	}
	
	/**
	 * <p>Resets everything to default values (for a list of default values please see the {@code .xml} document.
	 * Sets the status of this task to {@code INCOMPLETE}.</p>
	 */
	
	public void clear() {				
		rSq	= (double) NumericProperty.def
				(NumericPropertyKeyword.RSQUARED).getValue();
		ssr	= (double) NumericProperty.def
				(NumericPropertyKeyword.SUM_OF_SQUARES).getValue();				
		
		buffer 			= new Buffer();
		buffer.setParent(this);		
		log 			= new Log(this);
					
		testTemperature = (double)curve.getMetadata().getTestTemperature().getValue();
		
		calculateThermalProperties();
		
		this.path		= null;
		this.problem	= null;
		this.scheme		= null;				
				
		setStatus(Status.INCOMPLETE);		
		TaskStateEvent e = new TaskStateEvent(this, status);
		notifyStatusListeners(e);
		notifyDataListeners(e);
		
		curve.addDataListener( dataEvent -> {
			if(scheme != null) {
				double startTime = (double)problem.getHeatingCurve().getStartTime().getValue();
				scheme.setTimeLimit
				(NumericProperty.derive(TIME_LIMIT, RELATIVE_TIME_MARGIN*curve.timeLimit() - startTime));
			}
		}
		);

	}
	
	/**
	 * Generates a search vector (= optimisation vector) using the search flags 
	 * set by the {@code PathSolver}.
	 * @return an {@code IndexedVector} with search parameters of this {@code SearchTaks}
	 * @see pulse.search.direction.PathSolver.getSearchFlags()
	 * @see pulse.problem.statements.Problem.optimisationVector(List<Flag>)
	 */
	
	public IndexedVector searchVector() {
		return problem.optimisationVector(PathSolver.getSearchFlags())[0];
	}
	
	/**
	 * Assigns the values of the parameters of this {@code SearchTask} to {@code searchParameters}.
	 * @param searchParameters an {@code IndexedVector} with relevant search parameters
	 * @see pulse.problem.statements.Problem.assign(IndexedVector)
	 */
	
	public void assign(IndexedVector searchParameters) {
		problem.assign(searchParameters);
	}
	
	/**
	 * Calculates some or all of the following properties: <math><i>C</i><sub>p</sub>, <i>&rho;</i>, <i>&labmda;</i>, <i>&epsilon;</i></math>.
	 * <p>These properties will be calculated only if the necessary {@code InterpolationDataset}s were previously loaded by the {@code TaskManager}.</p>   
	 */
	
	public void calculateThermalProperties() {
		if(problem == null)
			return;
		
		InterpolationDataset cpCurve = TaskManager.getSpecificHeatCurve();				
		
		if(cpCurve != null) {
			cp = cpCurve.interpolateAt(testTemperature); 
			problem.set(SPECIFIC_HEAT, NumericProperty.derive(SPECIFIC_HEAT, cp));
		}
		
		InterpolationDataset rhoCurve = TaskManager.getDensityCurve();		
		if(rhoCurve != null) { 
			rho = rhoCurve.interpolateAt(testTemperature);
			problem.set(DENSITY, NumericProperty.derive(DENSITY, rho));
		}
		
		if(rhoCurve != null && cpCurve != null) {
			evalThermalConductivity();
			evalEmissivity();
		}
		
	}
	
	private void evalThermalConductivity() {
		double a   = (double)problem.getDiffusivity().getValue();			
		lambda = a * cp * rho;
	}

	private void evalEmissivity() {				
		double Bi     = (double)problem.getHeatLoss().getValue();
		double l      = (double)problem.getSampleThickness().getValue();		
			
		final double sigma = 5.6703E-08; //Stephan-Boltzmann constant
				
		emissivity =  Bi*lambda/(4.*Math.pow(testTemperature, 3)*l*sigma);
	}

	/**
	 * This will use the current {@code DifferenceScheme} to solve the {@code Problem} for this
	 * {@code SearchTask} and calculate the SSR value showing how well (or bad) the calculated solution
	 * describes the {@code ExperimentalData}.
	 * @return the value of SSR (sum of squared residuals).
	 */
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public double solveProblemAndCalculateDeviation() {
		((Solver)scheme).solve(problem);
		return problem.getHeatingCurve().deviationSquares(getExperimentalCurve()); 
	}
	
	/**
	 * <p>Runs this task if is either {@code READY} or {@code QUEUED}. Otherwise, will do nothing.
	 * After making some preparatory steps, will initiate a loop with successive calls to 
	 * {@code PathSolver.iteration(this)}, filling the buffer and notifying any data change listeners
	 * in parallel. This loop will go on until either converging results are obtained, or a timeout is
	 * reached, or if an execution error happens. Whether the run has been successful will be determined 
	 * by comparing the associated <i>R</i><sup>2</sup> value with the {@code SUCCESS_CUTOFF}.</p>
	 */
	
	@Override
	public void run() {
	
	  /* check of status */
		
	  switch(status) {
	  	case READY :
	  	case QUEUED : 
	  		setStatus(Status.IN_PROGRESS);
	  		break;
	  	default :
	  		return;
	  }		
	  
	  /* preparatory steps */
	    
	  HeatingCurve solutionCurve = problem.getHeatingCurve();
	  ssr	   			 		 = solveProblemAndCalculateDeviation();
	  
	  PathSolver pathSolver 	= TaskManager.getPathSolver();
	  
	  path 						= pathSolver.createPath(this);
	   
	  double errorTolerance		= (double)PathSolver.getErrorTolerance().getValue();
	  int bufferSize			= (Integer)buffer.getSize().getValue();	  
	  
	  rSq 						= solutionCurve.rSquared(this.getExperimentalCurve());	  
	  
	  /* search cycle */
	  
	  TaskStateEvent dataCollected = new TaskStateEvent(this, null);
	  
	  /* sets an independent thread for manipulating the buffer */
	  
	  List<CompletableFuture<Void>> bufferFutures = new ArrayList<CompletableFuture<Void>>(bufferSize);
	  ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
  
	  outer: do {				 
		  
		bufferFutures.clear();
		  
		for (int i = 0; i < bufferSize; i++) {	
			
				if(status != Status.IN_PROGRESS)
					break outer;
					
				ssr = pathSolver.iteration(this);
		  		rSq = solutionCurve.rSquared(getExperimentalCurve());		  				  	
		  		
		  		final int j = i;		  		
		  		
		  		bufferFutures.add(CompletableFuture.runAsync( () -> {
		  			buffer.fill(this, j);		  		
		  			notifyDataListeners(dataCollected); }, singleThreadExecutor ));		  		
		  		
		}
		
		bufferFutures.forEach( future -> future.join());
	  
	  }  while( buffer.isErrorTooHigh(errorTolerance) );
	  
	  calculateThermalProperties();
	  
	  singleThreadExecutor.shutdown();
	 	  	  
	  if( status == Status.IN_PROGRESS )
		 if(rSq > SUCCESS_CUTOFF)
			 setStatus(Status.DONE);
		 else
			 setStatus(Status.AMBIGUOUS);
	  		
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
		return NumericProperty.derive(TEST_TEMPERATURE, testTemperature);
	}
	
	public Path getPath() {
		return path;
	}
	
	public NumericProperty getSumOfSquares() {
		return NumericProperty.derive(SUM_OF_SQUARES, ssr); 
	}
	
	public NumericProperty getRSquared() {
		return NumericProperty.derive(RSQUARED, rSq);
	}
	
	public void setRSquared(double rSquared) {
		this.rSq = rSquared;
	}

	public void setSumOfSquares(double sumOfSquares) {
		this.ssr = sumOfSquares;
	}
	
	/**
	 * <p>After setting and adopting the {@code problem} by this {@code SearchTask},  
	 * this will attempt to change the parameters of that {@code problem} in accordance with the loaded
	 * {@code ExperimentalData} for this {@code SearchTask} (if not null). Later, if any changes to the 
	 * properties of that {@code Problem} occur and if the source of that event is either the {@code Metadata}
	 *  or the {@code PropertyHolderTable}, they will be accounted for by altering the parameters of the {@code problem}
	 * accordingly -- immediately after the former take place.</p>
	 * @param problem a {@code Problem}
	 */
	
	public void setProblem(Problem problem) {
		this.problem = problem;
				
		if(curve == null)
			return;
		
		if(problem == null)
			return;
			
		problem.setParent(this);
		problem.removeListeners(); 
		problem.retrieveData(curve);
		
		problem.addListener(new PropertyHolderListener() {

			@Override
			public void onPropertyChanged(PropertyEvent event) {
				if(!(event.getSource() instanceof Metadata))
					if(!(event.getSource() instanceof PropertyHolderTable))
						return;
				
				problem.estimateSignalRange(curve);
				problem.useParkersSolution(curve);
			
			}

		});
		
		problem.getHeatingCurve().addDataListener( dataEvent -> {
			
			DataEventType event = dataEvent.getType();								
			
			if(event == DataEventType.CHANGE_OF_ORIGIN) {
				double upperLimitUpdated = RELATIVE_TIME_MARGIN*curve.timeLimit() - 
						(double)problem.getHeatingCurve().getStartTime().getValue();
				scheme.setTimeLimit
				(NumericProperty.derive(TIME_LIMIT, upperLimitUpdated));
			}
				
			
		});
		
	}
	
	/**
	 * Adopts the {@code scheme} by this {@code SearchTask} and updates the time limit of {@scheme}
	 * to match {@code ExperimentalData}.
	 * @param scheme the {@code DiffenceScheme}.
	 */
	
	public void setScheme(DifferenceScheme scheme) {
		this.scheme = scheme;
		if(scheme != null) {
			scheme.setParent(this);
			
			double upperLimit = RELATIVE_TIME_MARGIN*curve.timeLimit() - 
					(double)problem.getHeatingCurve().getStartTime().getValue();
			
			scheme.setTimeLimit
			(NumericProperty.derive(TIME_LIMIT, upperLimit ));
			
		}
	}
	
	/**
	 * Adopts the {@code curve} by this {@code SearchTask}.
	 * @param curve the {@code ExperimentalData}.
	 */
	
	public void setExperimentalCurve(ExperimentalData curve) {
		this.curve = curve;
		
		if(curve != null)		
			curve.setParent(this);
	
	}
	
	/**
	 * Sets the test temperature and modifies, if needed, any of the thermal properties
	 * that depend on this parameter.
	 * @param testTemperature the test temperature
	 */
	
	public void setTestTemperature(NumericProperty testTemperature) {
		this.testTemperature = (double)testTemperature.getValue();
		calculateThermalProperties();
	}
	
	public Status getStatus() {
		return status;
	}
	
	/**
	 * Sets a new {@code status} to this {@code SearchTask} and informs the listeners.
	 * @param status the new status
	 */
	
	public void setStatus(Status status) {
		if(this.status == status)
			return;
		
		this.status = status;		
		notifyStatusListeners(new TaskStateEvent(this, status));
	}
		
	/**
	 * <p>Checks if this {@code SearchTask} is ready to be run. Performs basic check to see
	 * whether the user has uploaded all necessary data. If not, will create a status update
	 * with information about the missing data.</p>
	 * @return {@code READY} if the task is ready to be run, {@code DONE} if has already been 
	 * done previously, {@code INCOMPLETE} if some problems exist. For the latter, additional
	 * details will be available using the {@code status.getDetails()} method.</p>
	 * @return the current status
	 */
	
	public Status checkProblems() {
		if(status == Status.DONE)
			return status;		
		
		PathSolver pathSolver = TaskManager.getPathSolver();				
		Status s = Status.INCOMPLETE;
		
		if(problem == null) 
			s.setDetails(Details.MISSING_PROBLEM_STATEMENT);
		else if(! problem.allDetailsPresent())
			s.setDetails(Details.INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT);
		else if(scheme == null) 
			s.setDetails(Details.MISSING_DIFFERENCE_SCHEME);
		else if(curve == null)
			s.setDetails(Details.MISSING_HEATING_CURVE);
		else if(pathSolver == null)
			s.setDetails(Details.MISSING_PATH_SOLVER);
		else if(PathSolver.getLinearSolver() == null)
			s.setDetails(Details.MISSING_LINEAR_SOLVER);
		else if(buffer == null)
			s.setDetails(Details.MISSING_BUFFER);
		else 
			s = Status.READY;		
			
		setStatus(s);
			
		return status;
	}

	public Identifier getIdentifier() {
		return identifier;
	}
	
	public Log getLog() {
		return log;
	}

	public NumericProperty getThermalConductivity() {
		return NumericProperty.derive(CONDUCTIVITY, lambda);
	}

	public void setThermalConductivity(NumericProperty lambda) {
		this.lambda = (double)lambda.getValue();
	}	
	
	public NumericProperty getEmissivity() {
		return NumericProperty.derive(EMISSIVITY, emissivity);
	}

	public void setEmissivity(NumericProperty emissivity) {
		this.emissivity = (double)emissivity.getValue();
	}
	
	private void notifyDataListeners(TaskStateEvent e) {
		for(DataCollectionListener l : listeners)
  			l.onDataCollected(e);
	}
	
	private void notifyStatusListeners(TaskStateEvent e) {
		for(StatusChangeListener l : statusChangeListeners)
  			l.onStatusChange(e);
	}
	
	@Override
	public String describe() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(TaskManager.getSampleName());
		sb.append("_Task_");
		int extId = curve.getMetadata().getExternalID();
		if(extId < 0)
			sb.append("IntID_" + identifier.getValue());
		else
			sb.append("ExtID_" + extId);
		
		return sb.toString();
		
	}
	
	/**
	 * If the current task is either {@code IN_PROGRESS}, {@code QUEUED}, or {@code READY}, terminates 
	 * it by setting its status to {@code TERMINATED}. This change of status will then force the {@code run()}
	 * loop to stop (if running).
	 */
	
	public void terminate() {
		switch(status) {
			case IN_PROGRESS :
			case QUEUED :
			case READY :
				setStatus(Status.TERMINATED);
				break;
			default :
				return;
			}
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {		
		return;
	}
	
	/**
	 * A {@code SearchTask} is deemed equal to another one if it has the 
	 * same {@code ExperimentalData}. 
	 */
	
	@Override
	public boolean equals(Object o) {		
		if(o == this)
			return true;
		
		if(! (o instanceof SearchTask))
			return false;
		
		SearchTask other = (SearchTask)o;
		
		if( ! curve.equals(other.getExperimentalCurve()))
			return false;
		
		return true;
	
	}
	
	/*
	public void storeCurrentSolution() {
		storedSolution = HeatingCurve.copy(problem.getHeatingCurve());
	}
	
	public HeatingCurve getStoredSolution() {
		return storedSolution;
	}
	*/
}